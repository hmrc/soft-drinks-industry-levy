/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.softdrinksindustrylevy.connectors

import com.google.inject.{Inject, Singleton}
import play.api.libs.json.{Json, OWrites}
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import play.api.{Logger, Mode}
import sdil.models.*
import sdil.models.des.FinancialTransactionResponse
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.softdrinksindustrylevy.models.*
import uk.gov.hmrc.softdrinksindustrylevy.models.json.des.returns.*
import uk.gov.hmrc.softdrinksindustrylevy.services.{JsonSchemaChecker, Memoized, SdilMongoPersistence}
import uk.gov.hmrc.softdrinksindustrylevy.utils
import uk.gov.hmrc.softdrinksindustrylevy.utils.*

import java.net.URLEncoder.encode
import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.stm.TMap
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DesConnector @Inject() (
  val http: HttpClientV2,
  val mode: Mode,
  servicesConfig: ServicesConfig,
  persistence: SdilMongoPersistence,
  auditing: AuditConnector
)(implicit executionContext: ExecutionContext)
    extends DesHelpers(servicesConfig) with SubscriptionConnector {

  implicit private val logger: Logger = Logger(this.getClass)
  val desURL: String = servicesConfig.baseUrl("des")
  val desDirectDebitUrl: String = servicesConfig.baseUrl("des-direct-debit")
  val serviceURL: String = "soft-drinks"
  val cache: TMap[String, (Option[Subscription], LocalDateTime)] = TMap[String, (Option[Subscription], LocalDateTime)]()

  private val rawHttpReads = new RawHttpReads

  def createSubscription(request: Subscription, idType: String, idNumber: String)(implicit
    hc: HeaderCarrier
  ): Future[CreateSubscriptionResponse] = {
    import json.des.create.*
    val formattedWarehouseSites = request.warehouseSites.map(site => site.copy(address = formatAddress(site.address)))
    val formattedProductionSites = request.productionSites.map(site => site.copy(address = formatAddress(site.address)))
    val formattedAddress = formatAddress(request.address)
    val submission = request.copy(
      address = formattedAddress,
      warehouseSites = formattedWarehouseSites,
      productionSites = formattedProductionSites
    )

    JsonSchemaChecker[Subscription](request, "create-subscription")
    val path = s"/$serviceURL/subscription/$idType/$idNumber"
    val operation = "createSubscription"
    val subscriptionUrl = s"$desURL$path"
    val startTime = System.currentTimeMillis()
    val desHc = outboundHeaderCarrier(hc)
    logger.info(s"DES request ${loggingContext(operation)}")
    http
      .post(url"$subscriptionUrl")(using desHc)
      .transform(_.addHttpHeaders(desHeaders*))
      .withBody(Json.toJson(submission))
      .execute[HttpResponse](using rawHttpReads, executionContext)
      .map { response =>
        logger.info(
          s"DES response ${loggingContext(operation, status = Some(response.status), startTime = Some(startTime))}"
        )
        response.status match {
          case 429 =>
            throw UpstreamErrorResponse("429 received from DES - converted to 503", 503, 503)
          case status if status >= 200 && status < 300 =>
            parseResponse[CreateSubscriptionResponse](response)
          case status =>
            throw upstreamError("DES", operation, status)
        }
      }
      .recoverWith(recover("DES", operation, startTime))
  }

  def retrieveSubscriptionDetails(idType: String, idNumber: String)(implicit
    hc: HeaderCarrier
  ): Future[Option[Subscription]] = {

    lazy val memoized: String => Future[Option[Subscription]] =
      Memoized.memoizedCache[Future, String, Option[Subscription]](cache, 60 * 60)(getSubscriptionFromDES)

    def getSubscriptionFromDES(subscriptionUrl: String)(implicit hc: HeaderCarrier): Future[Option[Subscription]] = {
      import json.des.get._
      val operation = "retrieveSubscriptionDetails"
      val startTime = System.currentTimeMillis()
      val desHc = outboundHeaderCarrier(hc)
      logger.info(s"DES request ${loggingContext(operation)}")
      http
        .get(url"$subscriptionUrl")(using desHc)
        .transform(_.addHttpHeaders(desHeaders*))
        .execute[HttpResponse](using rawHttpReads, executionContext)
        .map { response =>
          logger.info(
            s"DES response ${loggingContext(operation, status = Some(response.status), startTime = Some(startTime))}"
          )
          response.status match {
            case 204 | 404 | 503 | 403 => None
            case 429 => throw UpstreamErrorResponse("429 received from DES - converted to 503", 503, 503)
            case status if status >= 200 && status < 300 =>
              Some(parseResponse[Subscription](response))
            case status =>
              throw upstreamError("DES", operation, status)
          }
        }
        .recoverWith(recover("DES", operation, startTime))
    }

    for {
      sub  <- memoized(s"$desURL/$serviceURL/subscription/details/$idType/$idNumber")
      subs <- sub.fold(Future(List.empty[Subscription]))(s => persistence.list(s.utr))
      _ <- sub.fold(Future(())) { x =>
             if (!subs.contains(x)) {
               persistence.insert(x.utr, x)
             } else Future(())
           }
    } yield sub
  }

  def submitReturn(sdilRef: String, returnsRequest: ReturnsRequest)(implicit
    hc: HeaderCarrier,
    period: ReturnPeriod
  ): Future[HttpResponse] = {
    val path = s"/$serviceURL/$sdilRef/return"
    val operation = "submitReturn"
    val returnUrl = s"$desURL$path"
    val startTime = System.currentTimeMillis()
    val desHc = outboundHeaderCarrier(hc)
    logger.info(s"DES request ${loggingContext(operation)}")
    http
      .post(url"$returnUrl")(using desHc)
      .transform(_.addHttpHeaders(desHeaders*))
      .withBody(Json.toJson(returnsRequest))
      .execute[HttpResponse](using readRaw, executionContext)
      .map { response =>
        logger.info(
          s"DES response ${loggingContext(operation, status = Some(response.status), startTime = Some(startTime))}"
        )
        response
      }
      .recoverWith(recover("DES", operation, startTime))
  }

  /** Calls API#1166: Get Financial Data.
    *
    * Attempts to retrieve a list of financial line items.
    *
    * @param year
    *   If provided will show all items for that year, if omitted will only show 'open' items
    */
  def retrieveFinancialData(
    sdilRef: String,
    year: Option[Int] = Some(LocalDate.now.getYear)
  )(implicit
    hc: HeaderCarrier
  ): Future[Option[des.FinancialTransactionResponse]] = {
    import des.FinancialTransaction.*

    val args: Map[String, Any] = Map(
      "onlyOpenItems"              -> year.isEmpty,
      "includeLocks"               -> false,
      "calculateAccruedInterest"   -> true,
      "customerPaymentInformation" -> true
    ) ++ (
      year match {
        case Some(y) =>
          Map(
            "dateFrom" -> s"$y-01-01",
            "dateTo"   -> s"$y-12-31"
          )
        case None => Map.empty[String, Any]
      }
    )

    def encodePair(in: (String, Any)): String =
      s"${encode(in._1, "UTF-8")}=${encode(in._2.toString, "UTF-8")}"

    val uri = s"$desURL/enterprise/financial-data/ZSDL/$sdilRef/ZSDL?" ++
      args
        .map {
          encodePair
        }
        .mkString("&")
    val operation = "retrieveFinancialData"
    val startTime = System.currentTimeMillis()
    val desHc = outboundHeaderCarrier(hc)
    logger.info(s"DES request ${loggingContext(operation)}")
    http
      .get(url"$uri")(using desHc)
      .transform(_.addHttpHeaders(desHeaders*))
      .execute[HttpResponse](using rawHttpReads, executionContext)
      .flatMap { response =>
        logger.info(
          s"DES response ${loggingContext(operation, status = Some(response.status), startTime = Some(startTime))}"
        )
        response.status match {
          case 204 | 404 | 503 | 403 =>
            Future.successful(None)
          case 429 =>
            Future.failed(UpstreamErrorResponse("429 received from DES - converted to 503", 503, 503))
          case status if status >= 200 && status < 300 =>
            val financialTransactionResponse = parseResponse[des.FinancialTransactionResponse](response)
            auditing.sendExtendedEvent(buildAuditEvent(financialTransactionResponse, uri, sdilRef)).map { _ =>
              Some(financialTransactionResponse)
            }
          case status =>
            Future.failed(upstreamError("DES", operation, status))
        }
      }
      .recoverWith(recover("DES", operation, startTime))
  }

  def displayDirectDebit(sdilRef: String)(implicit hc: HeaderCarrier): Future[DisplayDirectDebitResponse] = {
    val path = s"/cross-regime/direct-debits/zsdl/zsdl/$sdilRef"
    val operation = "displayDirectDebit"
    val uri = s"$desDirectDebitUrl$path"
    val startTime = System.currentTimeMillis()
    val desHc = outboundHeaderCarrier(hc)
    logger.info(s"DES request ${loggingContext(operation)}")
    http
      .get(url"$uri")(using desHc)
      .transform(_.addHttpHeaders(desHeaders*))
      .execute[HttpResponse](using rawHttpReads, executionContext)
      .map { response =>
        logger.info(
          s"DES response ${loggingContext(operation, status = Some(response.status), startTime = Some(startTime))}"
        )
        response.status match {
          case status if status >= 200 && status < 300 =>
            parseResponse[DisplayDirectDebitResponse](response)
          case status =>
            throw upstreamError("DES", operation, status)
        }
      }
      .recoverWith(recover("DES", operation, startTime))
  }

  private def buildAuditEvent(body: FinancialTransactionResponse, path: String, subscriptionId: String)(implicit
    hc: HeaderCarrier
  ) = {
    implicit val callbackFormat: OWrites[FinancialTransactionResponse] = Json.writes[FinancialTransactionResponse]
    val detailJson = Json.obj(
      "subscriptionId" -> subscriptionId,
      "url"            -> path,
      "response"       -> body
    )
    new BalanceQueryEvent(path, detailJson)
  }
}
