/*
 * Copyright 2021 HM Revenue & Customs
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

import java.net.URLEncoder.encode
import java.time.{LocalDate, LocalDateTime}
import cats.implicits._
import com.google.inject.{Inject, Singleton}
import play.api.{Logger, Mode}
import play.api.libs.json.{Json, OWrites}
import sdil.models._
import sdil.models.des.FinancialTransactionResponse
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.softdrinksindustrylevy.models._
import uk.gov.hmrc.softdrinksindustrylevy.models.json.des.returns._
import uk.gov.hmrc.softdrinksindustrylevy.services.{JsonSchemaChecker, Memoized, SdilPersistence}

import scala.concurrent.stm.TMap
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw

@Singleton
class DesConnector @Inject()(
  val http: HttpClient,
  val mode: Mode,
  servicesConfig: ServicesConfig,
  persistence: SdilPersistence,
  auditing: AuditConnector)(implicit executionContext: ExecutionContext)
    extends DesHelpers(servicesConfig) {

  val logger: Logger = Logger(this.getClass)
  val desURL: String = servicesConfig.baseUrl("des")
  val desDirectDebitUrl: String = servicesConfig.baseUrl("des-direct-debit")
  val serviceURL: String = "soft-drinks"
  val cache: TMap[String, (Option[Subscription], LocalDateTime)] = TMap[String, (Option[Subscription], LocalDateTime)]()

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

  def createSubscription(request: Subscription, idType: String, idNumber: String)(
    implicit hc: HeaderCarrier): Future[CreateSubscriptionResponse] = {
    import json.des.create._
    import uk.gov.hmrc.softdrinksindustrylevy.models.RosmResponseAddress._
    val formattedLines = request.address.lines.map { line =>
      line.clean
    }
    val formattedAddress = request.address match {
      case a: UkAddress      => a.copy(lines = formattedLines)
      case b: ForeignAddress => b.copy(lines = formattedLines)
    }
    val submission = request.copy(address = formattedAddress)

    JsonSchemaChecker[Subscription](request, "des-create-subscription")
    http
      .POST[Subscription, CreateSubscriptionResponse](
        s"$desURL/$serviceURL/subscription/$idType/$idNumber",
        submission,
        headers = desHeaders)
      .recover {
        case UpstreamErrorResponse(_, 429, _, _) =>
          logger.error("[RATE LIMITED] Received 429 from DES - converting to 503")
          throw UpstreamErrorResponse("429 received from DES - converted to 503", 503, 503)
      }
  }

  def retrieveSubscriptionDetails(idType: String, idNumber: String)(
    implicit hc: HeaderCarrier): Future[Option[Subscription]] = {

    lazy val memoized: String => Future[Option[Subscription]] =
      Memoized.memoizedCache[Future, String, Option[Subscription]](cache, 60 * 60)(getSubscriptionFromDES)

    def getSubscriptionFromDES(url: String)(implicit hc: HeaderCarrier): Future[Option[Subscription]] = {
      import json.des.get._
      http.GET[Option[Subscription]](url, headers = desHeaders)(implicitly, hc, implicitly)
    }

    for {
      sub  <- memoized(s"$desURL/$serviceURL/subscription/details/$idType/$idNumber")
      subs <- sub.fold(Future(List.empty[Subscription]))(s => persistence.subscriptions.list(s.utr))
      _ <- sub.fold(Future(())) { x =>
            if (!subs.contains(x)) {
              persistence.subscriptions.insert(x.utr, x)
            } else Future(())
          }
    } yield sub
  }

  def submitReturn(sdilRef: String, returnsRequest: ReturnsRequest)(
    implicit hc: HeaderCarrier,
    period: ReturnPeriod): Future[HttpResponse] =
    http
      .POST[ReturnsRequest, HttpResponse](s"$desURL/$serviceURL/$sdilRef/return", returnsRequest, headers = desHeaders)
      .recover {
        case UpstreamErrorResponse(_, 429, _, _) =>
          logger.error("[RATE LIMITED] Received 429 from DES - converting to 503")
          throw UpstreamErrorResponse("429 received from DES - converted to 503", 503, 503)
      }

  /** Calls API#1166: Get Financial Data.
    *
    * Attempts to retrieve a list of financial line items.
    *
    * @param year If provided will show all items for that year, if omitted will only show 'open' items
    */
  def retrieveFinancialData(
    sdilRef: String,
    year: Option[Int] = Some(LocalDate.now.getYear)
  )(
    implicit hc: HeaderCarrier
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

    http
      .GET[Option[des.FinancialTransactionResponse]](uri, headers = desHeaders)(implicitly, hc, implicitly)
      .flatMap { x =>
        x.map { y =>
          auditing.sendExtendedEvent(buildAuditEvent(y, uri, sdilRef))
        }
        Future(x)
      }
  }

  def displayDirectDebit(sdilRef: String)(implicit hc: HeaderCarrier): Future[DisplayDirectDebitResponse] = {
    val uri = s"$desDirectDebitUrl/cross-regime/direct-debits/zsdl/zsdl/$sdilRef"
    http.GET[DisplayDirectDebitResponse](uri, headers = desHeaders)(implicitly, hc, implicitly)
  }

  private def buildAuditEvent(body: FinancialTransactionResponse, path: String, subscriptionId: String)(
    implicit hc: HeaderCarrier) = {
    implicit val callbackFormat: OWrites[FinancialTransactionResponse] = Json.writes[FinancialTransactionResponse]
    val detailJson = Json.obj(
      "subscriptionId" -> subscriptionId,
      "url"            -> path,
      "response"       -> body
    )
    new BalanceQueryEvent(path, detailJson)
  }
}
