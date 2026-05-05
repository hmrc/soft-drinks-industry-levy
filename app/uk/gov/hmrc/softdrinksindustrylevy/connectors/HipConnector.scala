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
import play.api.http.Status.*
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import play.api.{Logger, Mode}
import sdil.models.ReturnPeriod
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.softdrinksindustrylevy.models.*
import uk.gov.hmrc.softdrinksindustrylevy.models.HipCreateSubscriptionResponse.*
import uk.gov.hmrc.softdrinksindustrylevy.models.json.des.returns.*
import uk.gov.hmrc.softdrinksindustrylevy.services.{JsonSchemaChecker, Memoized, SdilMongoPersistence}
import uk.gov.hmrc.softdrinksindustrylevy.utils.*

import java.time.format.DateTimeFormatter
import java.time.{Clock, Instant, LocalDateTime}
import java.util.UUID
import scala.concurrent.stm.TMap
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HipConnector @Inject() (
  val http: HttpClientV2,
  val mode: Mode,
  servicesConfig: ServicesConfig,
  persistence: SdilMongoPersistence,
  clock: Clock
)(implicit executionContext: ExecutionContext)
    extends SubscriptionConnector {

  implicit private val logger: Logger = Logger(this.getClass)

  private val sdilProtocolAndHostAndPort: String = servicesConfig.baseUrl("hip")
  private val sdilRoot: String = "soft-drinks"

  private val cache: TMap[String, (Option[Subscription], LocalDateTime)] =
    TMap[String, (Option[Subscription], LocalDateTime)]()

  private val rawHttpReads = new RawHttpReads

  private def hipHeaders: Seq[(String, String)] =
    Seq(
      "correlationid"         -> UUID.randomUUID().toString,
      "X-Originating-System"  -> "SDIL",
      "X-Receipt-Date"        -> DateTimeFormatter.ISO_INSTANT.format(Instant.now(clock)),
      "X-Transmitting-System" -> "HIP"
    )

  override def createSubscription(
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
    val path = s"/$sdilRoot/subscription/$idType/$idNumber"
    val startTime = System.currentTimeMillis()

    logger.info(s"HIP request ${loggingContext(operation)}")

    http
      .post(url"${endpointUrl(sdilProtocolAndHostAndPort, path)}")(using outboundHeaderCarrier(hc))
      .transform(_.addHttpHeaders(hipHeaders*))
      .withBody(Json.toJson(submission))
      .execute[HttpResponse](using rawHttpReads, executionContext)
      .map { response =>
        logger.info(s"HIP response ${loggingContext(operation, Some(response.status), Some(startTime))}")

        response.status match {
          case TOO_MANY_REQUESTS =>
            throw UpstreamErrorResponse(
              s"$TOO_MANY_REQUESTS received from HIP - converted to $SERVICE_UNAVAILABLE",
              SERVICE_UNAVAILABLE,
              SERVICE_UNAVAILABLE
            )
          case status if status >= 200 && status < 300 =>
            val hipResponse = parseResponse[HipCreateSubscriptionResponse](response)

            CreateSubscriptionResponse(
              processingDate = hipResponse.success.processingDate,
              formBundleNumber = hipResponse.success.formBundleNumber
            )
          case status =>
            throw upstreamError("HIP", operation, status)
        }
      }
      .recoverWith(recover("HIP", operation, startTime))
  }

  override def retrieveSubscriptionDetails(
    idType: String,
    idNumber: String
  )(implicit hc: HeaderCarrier): Future[Option[Subscription]] = {

    def getSubscriptionFromHip(subscriptionUrl: String)(implicit hc: HeaderCarrier): Future[Option[Subscription]] = {
      val operation = "retrieveSubscriptionDetails"
      val startTime = System.currentTimeMillis()

      logger.info(s"HIP request ${loggingContext(operation)}")

      http
        .get(url"$subscriptionUrl")(using outboundHeaderCarrier(hc))
        .transform(_.addHttpHeaders(hipHeaders*))
        .execute[HttpResponse](using rawHttpReads, executionContext)
        .map { response =>
          logger.info(s"HIP response ${loggingContext(operation, Some(response.status), Some(startTime))}")

          response.status match {
            case NO_CONTENT | NOT_FOUND | SERVICE_UNAVAILABLE | FORBIDDEN => None
            case TOO_MANY_REQUESTS =>
              throw UpstreamErrorResponse(
                "429 received from HIP - converted to 503",
                SERVICE_UNAVAILABLE,
                SERVICE_UNAVAILABLE
              )
            case status if status >= 200 && status < 300 =>
              Some(toSubscription(parseResponse[HipRetrieveSubscriptionDetailsResponse](response)))
            case status =>
              throw upstreamError("HIP", operation, status)
          }
        }
        .recoverWith(recover("HIP", operation, startTime))
    }

    lazy val memoized: String => Future[Option[Subscription]] =
      Memoized.memoizedCache[Future, String, Option[Subscription]](cache, 60 * 60)(getSubscriptionFromHip)

    for {
      sub  <- memoized(s"$sdilProtocolAndHostAndPort/$sdilRoot/subscription/details/$idType/$idNumber")
      subs <- sub.fold(Future.successful(List.empty[Subscription]))(s => persistence.list(s.utr))
      _ <- sub.fold(Future.successful(())) { x =>
             if (!subs.contains(x)) persistence.insert(x.utr, x)
             else Future.successful(())
           }
    } yield sub
  }

  override def submitReturn(
    sdilRef: String,
    returnsRequest: ReturnsRequest
  )(implicit hc: HeaderCarrier, period: ReturnPeriod): Future[HttpResponse] = {
    val operation = "submitReturn"
    val path = s"/$sdilRoot/$sdilRef/return"
    val startTime = System.currentTimeMillis()

    logger.info(s"HIP request ${loggingContext(operation)}")

    http
      .post(url"${endpointUrl(sdilProtocolAndHostAndPort, path)}")(using outboundHeaderCarrier(hc))
      .transform(_.addHttpHeaders(hipHeaders*))
      .withBody(Json.toJson(returnsRequest))
      .execute[HttpResponse](using readRaw, executionContext)
      .map { response =>
        logger.info(s"HIP response ${loggingContext(operation, Some(response.status), Some(startTime))}")
        response
      }
      .recoverWith(recover("HIP", operation, startTime))
  }

  private def toAddress(address: HipAddress): Address =
    address.country.map(_.toUpperCase) match {
      case Some("GB") | None =>
        UkAddress(
          lines = List(
            Some(address.line1),
            address.line2,
            address.line3,
            address.line4
          ).flatten.filter(_.trim.nonEmpty),
          postCode = address.postCode.getOrElse("")
        )

      case Some(country) =>
        ForeignAddress(
          lines = List(
            Some(address.line1),
            address.line2,
            address.line3,
            address.line4
          ).flatten.filter(_.trim.nonEmpty),
          country = country
        )
    }

  private def toSite(site: HipSite): Site =
    Site(
      address = toAddress(site.siteAddress),
      ref = site.siteReference,
      tradingName = site.tradingName,
      closureDate = site.closureDate
    )

  private def toSubscription(response: HipRetrieveSubscriptionDetailsResponse): Subscription = {
    val success = response.success
    val details = success.subscriptionDetails

    Subscription(
      utr = success.utr,
      sdilRef = Some(details.sdilRegistrationNumber),
      orgName = details.tradingName,
      orgType = None,
      address = toAddress(success.businessAddress),
      activity = RetrievedActivity(
        isProducer = details.smallProducer || details.largeProducer,
        isLarge = details.largeProducer,
        isContractPacker = details.contractPacker,
        isImporter = details.importer
      ),
      liabilityDate = details.taxObligationStartDate,
      productionSites = success.sites.filter(_.siteType == "2").map(toSite),
      warehouseSites = success.sites.filter(_.siteType == "1").map(toSite),
      contact = Contact(
        name = details.primaryContactName,
        positionInCompany = details.primaryPositionInCompany,
        phoneNumber = details.primaryTelephone,
        email = details.primaryEmail
      ),
      endDate = details.taxObligationEndDate,
      deregDate = details.deregistrationDate
    )
  }
}
