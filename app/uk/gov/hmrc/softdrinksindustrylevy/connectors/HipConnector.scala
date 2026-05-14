/*
 * Copyright 2026 HM Revenue & Customs
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
import play.api.Logger
import play.api.http.Status.*
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import sdil.models.ReturnPeriod
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReadsInstances, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.softdrinksindustrylevy.models.HipRetrieveSubscriptionDetailsResponse.toSubscription
import uk.gov.hmrc.softdrinksindustrylevy.models.*
import uk.gov.hmrc.softdrinksindustrylevy.models.json.des.returns.*
import uk.gov.hmrc.softdrinksindustrylevy.services.{JsonSchemaChecker, Memoized, SdilMongoPersistence}

import java.time.format.DateTimeFormatter
import java.time.{Clock, Instant, LocalDateTime}
import java.util.UUID
import scala.concurrent.stm.TMap
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class HipConnector @Inject() (
  http: HttpClientV2,
  servicesConfig: ServicesConfig,
  persistence: SdilMongoPersistence,
  clock: Clock
)(implicit executionContext: ExecutionContext) {

  implicit private val logger: Logger = Logger(this.getClass)

  private val hipBaseURL: String = servicesConfig.baseUrl("hip")
  private val softDrinksApiRoot: String = "soft-drinks"

  private val cache: TMap[String, (Option[Subscription], LocalDateTime)] =
    TMap[String, (Option[Subscription], LocalDateTime)]()

  private def hipHeaders: Seq[(String, String)] =
    Seq(
      "correlationid"         -> UUID.randomUUID().toString,
      "X-Originating-System"  -> "SDIL",
      "X-Receipt-Date"        -> DateTimeFormatter.ISO_INSTANT.format(Instant.now(clock)),
      "X-Transmitting-System" -> "HIP"
    )

  private def outboundHeaderCarrier(hc: HeaderCarrier): HeaderCarrier =
    HeaderCarrier(
      requestId = hc.requestId,
      sessionId = hc.sessionId
    )

  private def upstreamError(
    system: String,
    operation: String,
    status: Int,
    responseBody: Option[String]
  ): UpstreamErrorResponse =
    UpstreamErrorResponse(
      s"Received $status from $system during $operation" +
        responseBody.fold("")(body => s" with response body: $body"),
      status
    )

  private def recover[A](operation: String, startTime: Long): PartialFunction[Throwable, Future[A]] = {
    case e @ UpstreamErrorResponse(message, status, _, _) =>
      logger.error(
        s"$message ${hipContext(
            operation = operation,
            status = Some(status),
            startTime = Some(startTime),
            errorClass = Some(e.getClass.getSimpleName)
          )}"
      )
      Future.failed(e)

    case NonFatal(e) =>
      logger.error(
        s"${e.getMessage} ${hipContext(
            operation = operation,
            startTime = Some(startTime),
            errorClass = Some(e.getClass.getSimpleName)
          )}",
        e
      )
      Future.failed(e)
  }

  private def endpointUrl(apiBaseUrl: String, path: String): String = s"$apiBaseUrl$path"

  private def hipContext(
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

  private def formatAddress(address: Address): Address = {
    import uk.gov.hmrc.softdrinksindustrylevy.models.RosmResponseAddress.*
    address match {
      case a: UkAddress      => a.copy(lines = address.lines.map(_.clean))
      case b: ForeignAddress => b.copy(lines = address.lines.map(_.clean))
      case _                 => throw new Exception("Cannot format address with params supplied")
    }
  }

  def createSubscription(
    request: Subscription,
    idType: String,
    idNumber: String
  )(implicit hc: HeaderCarrier): Future[CreateSubscriptionResponse] = {
    import uk.gov.hmrc.softdrinksindustrylevy.models.json.des.create.*

    val submission =
      request.copy(
        address = formatAddress(request.address),
        warehouseSites = request.warehouseSites.map(site => site.copy(address = formatAddress(site.address))),
        productionSites = request.productionSites.map(site => site.copy(address = formatAddress(site.address)))
      )

    JsonSchemaChecker[Subscription](request, "create-subscription")

    val operation = "createSubscription"
    val path = s"/$softDrinksApiRoot/subscription/$idType/$idNumber"
    val startTime = System.currentTimeMillis()

    logger.info(s"HIP request ${hipContext(operation)}")

    http
      .post(url"${endpointUrl(hipBaseURL, path)}")(using outboundHeaderCarrier(hc))
      .transform(_.addHttpHeaders(hipHeaders*))
      .withBody(Json.toJson(submission)(using subscriptionFormat))
      .execute[HttpResponse](using HttpReadsInstances.readRaw, executionContext)
      .map { response =>
        logger.info(s"HIP response ${hipContext(operation, Some(response.status), Some(startTime))}")

        response.status match {
          case CREATED =>
            val hipResponse = response.json.as[HipCreateSubscriptionResponse]
            CreateSubscriptionResponse(
              processingDate = hipResponse.success.processingDate,
              formBundleNumber = hipResponse.success.formBundleNumber
            )
          case status =>
            throw upstreamError(
              "HIP",
              operation,
              status,
              Some(response.body)
            )
        }
      }
      .recoverWith(recover(operation, startTime))
  }

  def retrieveSubscriptionDetails(idType: String, idNumber: String)(implicit
    hc: HeaderCarrier
  ): Future[Option[Subscription]] = {

    def getSubscriptionFromHip(subscriptionUrl: String)(implicit hc: HeaderCarrier): Future[Option[Subscription]] = {
      val operation = "retrieveSubscriptionDetails"
      val startTime = System.currentTimeMillis()

      logger.info(s"HIP request ${hipContext(operation)}")

      http
        .get(url"$subscriptionUrl")(using outboundHeaderCarrier(hc))
        .transform(_.addHttpHeaders(hipHeaders*))
        .execute[HttpResponse](using HttpReadsInstances.readRaw, executionContext)
        .map { response =>
          logger.info(s"HIP response ${hipContext(operation, Some(response.status), Some(startTime))}")
          response.status match {
            case OK =>
              Some(toSubscription(response.json.as[HipRetrieveSubscriptionDetailsResponse]))
            case NO_CONTENT | NOT_FOUND | SERVICE_UNAVAILABLE | FORBIDDEN =>
              None
            case status =>
              throw upstreamError(
                "HIP",
                operation,
                status,
                Some(response.body)
              )
          }
        }
        .recoverWith(recover(operation, startTime))
    }

    lazy val memoized: String => Future[Option[Subscription]] =
      Memoized.memoizedCache[Future, String, Option[Subscription]](cache, 60 * 60)(getSubscriptionFromHip)

    for {
      sub  <- memoized(s"$hipBaseURL/$softDrinksApiRoot/subscription/details/$idType/$idNumber")
      subs <- sub.fold(Future.successful(List.empty[Subscription]))(s => persistence.list(s.utr))
      _ <- sub.fold(Future.successful(())) { x =>
             if (!subs.contains(x)) persistence.insert(x.utr, x)
             else Future.successful(())
           }
    } yield sub
  }

  def submitReturn(sdilRef: String, returnsRequest: ReturnsRequest)(implicit
    hc: HeaderCarrier,
    period: ReturnPeriod
  ): Future[HttpResponse] = {

    val operation = "submitReturn"
    val path = s"/$softDrinksApiRoot/$sdilRef/return"
    val startTime = System.currentTimeMillis()

    logger.info(s"HIP request ${hipContext(operation)}")

    http
      .post(url"${endpointUrl(hipBaseURL, path)}")(using outboundHeaderCarrier(hc))
      .transform(_.addHttpHeaders(hipHeaders*))
      .withBody(Json.toJson(returnsRequest))
      .execute[HttpResponse](using HttpReadsInstances.readRaw, executionContext)
      .map { response =>
        logger.info(s"HIP response ${hipContext(operation, Some(response.status), Some(startTime))}")
        response
      }
      .recoverWith(recover(operation, startTime))
  }
}
