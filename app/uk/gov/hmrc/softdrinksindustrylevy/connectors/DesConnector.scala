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
import play.api.libs.json.{Json, OWrites, Reads}
import play.api.{Logger, Mode}
import sdil.models._
import sdil.models.des.FinancialTransactionResponse
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.softdrinksindustrylevy.models._
import uk.gov.hmrc.softdrinksindustrylevy.models.json.des.returns._
import uk.gov.hmrc.softdrinksindustrylevy.services.{JsonSchemaChecker, Memoized, SdilMongoPersistence}

import java.net.URLEncoder.encode
import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.stm.TMap
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue

@Singleton
class DesConnector @Inject() (
  val http: HttpClientV2,
  val mode: Mode,
  servicesConfig: ServicesConfig,
  persistence: SdilMongoPersistence,
  auditing: AuditConnector
)(implicit executionContext: ExecutionContext)
    extends DesHelpers(servicesConfig) {

  val logger: Logger = Logger(this.getClass)
  val desURL: String = servicesConfig.baseUrl("des")
  val desDirectDebitUrl: String = servicesConfig.baseUrl("des-direct-debit")
  val serviceURL: String = "soft-drinks"
  val cache: TMap[String, (Option[Subscription], LocalDateTime)] = TMap[String, (Option[Subscription], LocalDateTime)]()

  private class RawHttpReads extends HttpReads[HttpResponse] {
    override def read(method: String, url: String, response: HttpResponse): HttpResponse = response
  }

  private val rawHttpReads = new RawHttpReads

  private def outboundHeaderCarrier(hc: HeaderCarrier): HeaderCarrier =
    HeaderCarrier(
      requestId = hc.requestId,
      sessionId = hc.sessionId
    )

  private def desContext(
    operation: String,
    status: Option[Int] = None,
    startTime: Option[Long] = None,
    errorClass: Option[String] = None
  ): String =
    Seq(
      Some(s"operation=$operation"),
      status.map(st => s"status=$st"),
      startTime.map(st => s"durationMs=${System.currentTimeMillis() - st}"),
      errorClass.map(name => s"errorClass=$name")
    ).flatten.mkString(" ")

  private def parseResponse[A: Reads](response: HttpResponse): A =
    response.json.as[A]

  private def upstreamError(operation: String, status: Int): UpstreamErrorResponse =
    UpstreamErrorResponse(s"Received $status from DES during $operation", status, status)

  // DES return 503 in the event of no subscription for the UTR, we are expected to treat as 404, hence this override
  implicit def HttpReads[A](implicit rds: HttpReads[A]): HttpReads[Option[A]] = new HttpReads[Option[A]] {
    def read(method: String, url: String, response: HttpResponse): Option[A] = response.status match {
      case 204 | 404 | 503 | 403 => None
      case 429 =>
        logger.error("[RATE LIMITED] Received 429 from DES - converting to 503")
        throw UpstreamErrorResponse("429 received from DES - converted to 503", 503, 503)
      case _ => Some(rds.read(method, url, response))
    }
  }

  private def formatAddress(address: Address): Address = {
    import uk.gov.hmrc.softdrinksindustrylevy.models.RosmResponseAddress._
    address match {
      case a: UkAddress      => a.copy(lines = address.lines.map(_.clean))
      case b: ForeignAddress => b.copy(lines = address.lines.map(_.clean))
      case _                 => throw new Exception("Cannot format address with params supplied")
    }
  }

  def createSubscription(request: Subscription, idType: String, idNumber: String)(implicit
    hc: HeaderCarrier
  ): Future[CreateSubscriptionResponse] = {
    import json.des.create._
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
    logger.info(s"DES request ${desContext(operation)}")
    http
      .post(url"$subscriptionUrl")(using desHc)
      .transform(_.addHttpHeaders(desHeaders*))
      .withBody(Json.toJson(submission))
      .execute[HttpResponse](using rawHttpReads, executionContext)
      .map { response =>
        logger.info(
          s"DES response ${desContext(operation, status = Some(response.status), startTime = Some(startTime))}"
        )
        response.status match {
          case 429 =>
            throw UpstreamErrorResponse("429 received from DES - converted to 503", 503, 503)
          case status if status >= 200 && status < 300 =>
            parseResponse[CreateSubscriptionResponse](response)
          case status =>
            throw upstreamError(operation, status)
        }
      }
      .recoverWith {
        case e @ UpstreamErrorResponse(_, status, _, _) =>
          logger.error(
            s"DES failure ${desContext(operation, status = Some(status), startTime = Some(startTime), errorClass = Some(e.getClass.getSimpleName))}"
          )
          Future.failed(e)
        case NonFatal(e) =>
          logger.error(
            s"DES failure ${desContext(operation, startTime = Some(startTime), errorClass = Some(e.getClass.getSimpleName))}",
            e
          )
          Future.failed(e)
      }
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
      logger.info(s"DES request ${desContext(operation)}")
      http
        .get(url"$subscriptionUrl")(using desHc)
        .transform(_.addHttpHeaders(desHeaders*))
        .execute[HttpResponse](using rawHttpReads, executionContext)
        .map { response =>
          logger.info(
            s"DES response ${desContext(operation, status = Some(response.status), startTime = Some(startTime))}"
          )
          response.status match {
            case 204 | 404 | 503 | 403 => None
            case 429 => throw UpstreamErrorResponse("429 received from DES - converted to 503", 503, 503)
            case status if status >= 200 && status < 300 =>
              Some(parseResponse[Subscription](response))
            case status =>
              throw upstreamError(operation, status)
          }
        }
        .recoverWith {
          case e @ UpstreamErrorResponse(_, status, _, _) =>
            logger.error(
              s"DES failure ${desContext(operation, status = Some(status), startTime = Some(startTime), errorClass = Some(e.getClass.getSimpleName))}"
            )
            Future.failed(e)
          case NonFatal(e) =>
            logger.error(
              s"DES failure ${desContext(operation, startTime = Some(startTime), errorClass = Some(e.getClass.getSimpleName))}",
              e
            )
            Future.failed(e)
        }
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
    logger.info(s"DES request ${desContext(operation)}")
    http
      .post(url"$returnUrl")(using desHc)
      .transform(_.addHttpHeaders(desHeaders*))
      .withBody(Json.toJson(returnsRequest))
      .execute[HttpResponse](using readRaw, executionContext)
      .map { response =>
        logger.info(
          s"DES response ${desContext(operation, status = Some(response.status), startTime = Some(startTime))}"
        )
        response
      }
      .recoverWith {
        case e @ UpstreamErrorResponse(_, status, _, _) =>
          logger.error(
            s"DES failure ${desContext(operation, status = Some(status), startTime = Some(startTime), errorClass = Some(e.getClass.getSimpleName))}"
          )
          Future.failed(e)
        case NonFatal(e) =>
          logger.error(
            s"DES failure ${desContext(operation, startTime = Some(startTime), errorClass = Some(e.getClass.getSimpleName))}",
            e
          )
          Future.failed(e)
      }
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
    import des.FinancialTransaction._

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
    logger.info(s"DES request ${desContext(operation)}")
    http
      .get(url"$uri")(using desHc)
      .transform(_.addHttpHeaders(desHeaders*))
      .execute[HttpResponse](using rawHttpReads, executionContext)
      .flatMap { response =>
        logger.info(
          s"DES response ${desContext(operation, status = Some(response.status), startTime = Some(startTime))}"
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
            Future.failed(upstreamError(operation, status))
        }
      }
      .recoverWith {
        case e @ UpstreamErrorResponse(_, status, _, _) =>
          logger.error(
            s"DES failure ${desContext(operation, status = Some(status), startTime = Some(startTime), errorClass = Some(e.getClass.getSimpleName))}"
          )
          Future.failed(e)
        case NonFatal(e) =>
          logger.error(
            s"DES failure ${desContext(operation, startTime = Some(startTime), errorClass = Some(e.getClass.getSimpleName))}",
            e
          )
          Future.failed(e)
      }
  }

  def displayDirectDebit(sdilRef: String)(implicit hc: HeaderCarrier): Future[DisplayDirectDebitResponse] = {
    val path = s"/cross-regime/direct-debits/zsdl/zsdl/$sdilRef"
    val operation = "displayDirectDebit"
    val uri = s"$desDirectDebitUrl$path"
    val startTime = System.currentTimeMillis()
    val desHc = outboundHeaderCarrier(hc)
    logger.info(s"DES request ${desContext(operation)}")
    http
      .get(url"$uri")(using desHc)
      .transform(_.addHttpHeaders(desHeaders*))
      .execute[HttpResponse](using rawHttpReads, executionContext)
      .map { response =>
        logger.info(
          s"DES response ${desContext(operation, status = Some(response.status), startTime = Some(startTime))}"
        )
        response.status match {
          case status if status >= 200 && status < 300 =>
            parseResponse[DisplayDirectDebitResponse](response)
          case status =>
            throw upstreamError(operation, status)
        }
      }
      .recoverWith {
        case e @ UpstreamErrorResponse(_, status, _, _) =>
          logger.error(
            s"DES failure ${desContext(operation, status = Some(status), startTime = Some(startTime), errorClass = Some(e.getClass.getSimpleName))}"
          )
          Future.failed(e)
        case NonFatal(e) =>
          logger.error(
            s"DES failure ${desContext(operation, startTime = Some(startTime), errorClass = Some(e.getClass.getSimpleName))}",
            e
          )
          Future.failed(e)
      }
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
