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

import cats.implicits.catsSyntaxOptionId
import play.api.http.Status.*
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import play.api.{Logger, Mode}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReadsInstances, HttpResponse, StringContextOps}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.softdrinksindustrylevy.models.*
import uk.gov.hmrc.softdrinksindustrylevy.models.HipRetrieveSubscriptionDetailsResponse.toSubscription
import uk.gov.hmrc.softdrinksindustrylevy.services.{JsonSchemaChecker, Memoized, SdilMongoPersistence}

import java.time.{Clock, LocalDateTime}
import javax.inject.Inject
import scala.concurrent.stm.TMap
import scala.concurrent.{ExecutionContext, Future}

class HipSubscriptionConnector @Inject(
  http: HttpClientV2,
  mode: Mode,
  servicesConfig: ServicesConfig,
  persistence: SdilMongoPersistence,
  auditing: AuditConnector,
  clock: Clock
) (implicit executionContext: ExecutionContext)
    extends ConnectorHelpers(servicesConfig, clock) {

  implicit private val logger: Logger = Logger(this.getClass)

  private val cache: TMap[String, (Option[Subscription], LocalDateTime)] =
    TMap[String, (Option[Subscription], LocalDateTime)]()

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

    logger.info(s"HIP request ${loggingContext(operation)}")

    http
      .post(url"${endpointUrl(hipBaseURL, path)}")(using outboundHeaderCarrier(hc))
      .transform(_.addHttpHeaders(hipHeaders*))
      .withBody(Json.toJson(submission)(using subscriptionFormat))
      .execute[HttpResponse](using HttpReadsInstances.readRaw, executionContext)
      .map { response =>
        logger.info(s"HIP response ${loggingContext(operation, response.status.some, startTime.some)}")

        response.status match {
          case CREATED =>
            val hipResponse = response.json.as[HipCreateSubscriptionResponse]
            CreateSubscriptionResponse(
              processingDate = hipResponse.success.processingDate,
              formBundleNumber = hipResponse.success.formBundleNumber
            )
          case status @ (BAD_REQUEST | UNAUTHORIZED | FORBIDDEN | NOT_FOUND | UNPROCESSABLE_ENTITY |
              INTERNAL_SERVER_ERROR) =>
            throw upstreamError(
              "HIP",
              operation,
              status,
              response.body.some
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

      logger.info(s"HIP request ${loggingContext(operation)}")

      http
        .get(url"$subscriptionUrl")(using outboundHeaderCarrier(hc))
        .transform(_.addHttpHeaders(hipHeaders*))
        .execute[HttpResponse](using HttpReadsInstances.readRaw, executionContext)
        .map { response =>
          logger.info(s"HIP response ${loggingContext(operation, response.status.some, startTime.some)}")
          response.status match {
            case OK =>
              toSubscription(response.json.as[HipRetrieveSubscriptionDetailsResponse]).some
            case status @ (BAD_REQUEST | UNAUTHORIZED | FORBIDDEN | NOT_FOUND | UNPROCESSABLE_ENTITY |
                INTERNAL_SERVER_ERROR) =>
              throw upstreamError(
                "HIP",
                operation,
                status,
                response.body.some
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

}
