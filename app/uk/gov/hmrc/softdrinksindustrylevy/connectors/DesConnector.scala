/*
 * Copyright 2018 HM Revenue & Customs
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
import java.time.{Clock, LocalDate, LocalDateTime}

import cats.Monad
import cats.implicits._
import play.api.Configuration
import play.api.Mode.Mode
import play.api.libs.json.{Json, OWrites}
import sdil.models._
import sdil.models.des.FinancialTransactionResponse
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.softdrinksindustrylevy.models._
import uk.gov.hmrc.softdrinksindustrylevy.models.json.des.returns._
import uk.gov.hmrc.softdrinksindustrylevy.services.{JsonSchemaChecker, MemoizedSubscriptions, SdilPersistence}

import scala.concurrent.stm.TMap
import scala.concurrent.stm._
import scala.language.higherKinds
import scala.concurrent.{ExecutionContext, Future}

class DesConnector(val http: HttpClient,
                   val mode: Mode,
                   val runModeConfiguration: Configuration,
                   persistence: SdilPersistence,
                   auditing: AuditConnector)
                  (implicit clock: Clock)
  extends ServicesConfig with OptionHttpReads with DesHelpers {

  val desURL: String = baseUrl("des")
  val serviceURL: String = "soft-drinks"
  val memoizedSubscriptions = new MemoizedSubscriptions(http, mode, runModeConfiguration)

//  val cache = TMap[String, (Option[Subscription], LocalDateTime)]()

  // DES return 503 in the event of no subscription for the UTR, we are expected to treat as 404, hence this override
  implicit override def readOptionOf[P](implicit rds: HttpReads[P]): HttpReads[Option[P]] = new HttpReads[Option[P]] {
    def read(method: String, url: String, response: HttpResponse): Option[P] = response.status match {
      case 204 | 404 | 503 => None
      case _ => Some(rds.read(method, url, response))
    }
  }

  def createSubscription(request: Subscription, idType: String, idNumber: String)
                        (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[CreateSubscriptionResponse] = {
    import json.des.create._
    import uk.gov.hmrc.softdrinksindustrylevy.models.RosmResponseAddress._
    val formattedLines = request.address.lines.map { line => line.clean }
    val formattedAddress = request.address match {
      case a: UkAddress => a.copy(lines = formattedLines)
      case b: ForeignAddress => b.copy(lines = formattedLines)
    }
    val submission = request.copy(address = formattedAddress)

    JsonSchemaChecker[Subscription](request, "des-create-subscription")
    desPost[Subscription, CreateSubscriptionResponse](s"$desURL/$serviceURL/subscription/$idType/$idNumber", submission)
  }

  def retrieveSubscriptionDetails(idType: String, idNumber: String)
                                 (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Subscription]] = {
//    val url = s"$desURL/$serviceURL/subscription/details/$idType/$idNumber"
    for {
//      sub  <- getSub(idType, idNumber)
//      sub <- http.GET[Option[Subscription]](url)(implicitly, addHeaders, ec)
      sub <- memoizedSubscriptions.getSubscription(s"$desURL/$serviceURL/subscription/details/$idType/$idNumber")
      subs <- sub.fold(Future(List.empty[Subscription]))(s => persistence.subscriptions.list(s.utr))
      _ <- sub.fold(Future(())) { x =>
        if (!subs.contains(x)) {
          persistence.subscriptions.insert(x.utr, x)
        } else Future(())
      }
    } yield sub
  }

//  private def getSub(idType: String, idNumber: String)
//    (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Subscription]] = {
//
//    import json.des.get._
//
//    val url = s"$desURL/$serviceURL/subscription/details/$idType/$idNumber"
//
//    def fetch(key: String): Future[Option[Subscription]] =
//      http.GET[Option[Subscription]](url)(implicitly, addHeaders, ec)
//
//    def read(key: String): Future[Option[(Option[Subscription], LocalDateTime)]] = {
//      Future(atomic {implicit t => cache.get(key)})
//    }
//
//    def write(key: String, value: (Option[Subscription], LocalDateTime)):Future[Unit] = {
//      Future(atomic(implicit t => cache.put(key, value)).map(_ => ()))
//    }
//
//    memoized[Future, String, Option[Subscription]](
//      fetch,
//      read,
//      write
//    ).apply(url)
//  }
//
//
//  def memoized[F[_] : Monad,A,B](
//    f: A => F[B],
//    cacheRead: A => F[Option[(B,LocalDateTime)]],
//    cacheWrite: (A, (B,LocalDateTime)) => F[Unit],
//    ttl: LocalDateTime = LocalDateTime.now().plusHours(1)
//  ): A => F[B] = { args =>
//    cacheRead(args).flatMap {
//      case Some((v,d)) if d.isBefore(ttl) => {
//        v.pure[F]
//      }
//      case None => {
//        f(args).flatMap { z =>
//          cacheWrite(args, (z, LocalDateTime.now())).map(_ => z)
//        }
//      }
//    }
//  }


  def submitReturn(sdilRef: String, returnsRequest: ReturnsRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext, period: ReturnPeriod): Future[HttpResponse] = {
    desPost[ReturnsRequest, HttpResponse](s"$desURL/$serviceURL/$sdilRef/return", returnsRequest)
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
    implicit hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Option[des.FinancialTransactionResponse]] = {
    import des.FinancialTransaction._

    val args: Map[String,Any] = Map(
      "onlyOpenItems" -> year.isEmpty,
      "includeLocks" -> false,
      "calculateAccruedInterest" -> true,
      "customerPaymentInformation" -> true
    ) ++ (
      year match {
        case Some(y) => Map(
          "dateFrom" -> s"$y-01-01",
          "dateTo" -> s"$y-12-31"
        )
        case None => Map.empty[String,Any]
      }
    )

    def encodePair(in: (String,Any)): String = 
      s"${encode(in._1, "UTF-8")}=${encode(in._2.toString, "UTF-8")}"

    val uri = s"$desURL/enterprise/financial-data/ZSDL/${sdilRef}/ZSDL?" ++
      args.map{encodePair}.mkString("&")


    http.GET[Option[des.FinancialTransactionResponse]](uri)(implicitly, addHeaders, ec).flatMap{x =>
      x.map { y =>
        auditing.sendExtendedEvent(buildAuditEvent(y, uri, sdilRef))
      }
      Future(x)
    }
  }

  private def buildAuditEvent(body: FinancialTransactionResponse, path: String, subscriptionId: String)(implicit hc: HeaderCarrier) = {
    implicit val callbackFormat: OWrites[FinancialTransactionResponse] = Json.writes[FinancialTransactionResponse]
    val detailJson = Json.obj(
      "subscriptionId" -> subscriptionId,
      "url" -> path,
      "response" -> body
    )
    new BalanceQueryEvent(path, detailJson)
  }

}
