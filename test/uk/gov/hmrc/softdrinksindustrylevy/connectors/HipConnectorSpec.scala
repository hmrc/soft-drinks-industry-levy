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

import com.github.tomakehurst.wiremock.client.WireMock.*
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.*
import play.api.libs.json.*
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import sdil.models.ReturnPeriod
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.softdrinksindustrylevy.models.*
import uk.gov.hmrc.softdrinksindustrylevy.models.connectors.activity
import uk.gov.hmrc.softdrinksindustrylevy.models.json.des.create.*
import uk.gov.hmrc.softdrinksindustrylevy.util.{FakeApplicationSpec, WireMockMethods}

import java.time.*
import java.time.format.DateTimeFormatter
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class HipConnectorSpec extends FakeApplicationSpec with MockitoSugar with HttpClientV2Helper with WireMockMethods {

  private val CORRELATION_ID_KEY: String = "correlationid"
  private val CORRELATION_ID_VALUE: String = UUID.randomUUID().toString()

  private val X_ORIGINATING_SYSTEM_KEY: String = "X-Originating-System"
  private val X_ORIGINATING_SYSTEM_VALUE: String = "SDIL"

  private val X_RECEIPT_DATE_KEY: String = "X-Receipt-Date"
  private val X_RECEIPT_DATE_VALUE: String =
    DateTimeFormatter.ISO_INSTANT.format(Instant.now(Clock.systemDefaultZone()))

  private val X_TRANSMITTING_SYSTEM_KEY: String = "X-Transmitting-System"
  private val X_TRANSMITTING_SYSTEM_VALUE: String = "HIP"

  val hipConnector: HipConnector = app.injector.instanceOf[HipConnector]

  implicit val hc: HeaderCarrier = new HeaderCarrier
  implicit lazy val executionContext: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  private val TWENTY_TWENTY_FOUR = 2024
  private val FIRST_QUARTER = 1
  implicit val period: ReturnPeriod = ReturnPeriod(TWENTY_TWENTY_FOUR, FIRST_QUARTER)

  val expectedHeaders: Seq[(String, String)] =
    Seq(
      CORRELATION_ID_KEY -> CORRELATION_ID_VALUE,
      X_ORIGINATING_SYSTEM_KEY -> X_ORIGINATING_SYSTEM_VALUE,
      X_RECEIPT_DATE_KEY -> X_RECEIPT_DATE_VALUE,
      X_TRANSMITTING_SYSTEM_KEY -> X_TRANSMITTING_SYSTEM_VALUE
    )


  "HipConnector" should {

    val subscription: Subscription = Subscription(
      utr = "utr",
      sdilRef = Some("12345678910"),
      orgName = "IBM",
      orgType = Some("Ltd"),
      address = UkAddress(List("Glenfield Road", "Park View"), "SE12 4FH"),
      activity = activity,
      liabilityDate = LocalDate.now(),
      productionSites = List.empty[Site],
      warehouseSites = List.empty[Site],
      contact = Contact(
        name = Some("Mike Tyson"),
        positionInCompany = Some("Manager"),
        phoneNumber = "1234567890",
        email = "mike.tyson@boxing.com"
      ),
      endDate = None,
      deregDate = None
    )

    val hipRetrieveSubscriptionDetailsSuccessResponseJson = Json.obj(
      "success" -> Json.obj(
        "utr" -> subscription.utr,
        "subscriptionDetails" -> Json.obj(
          "sdilRegistrationNumber" -> subscription.sdilRef.get,
          "taxObligationStartDate" -> subscription.liabilityDate.toString,
          "taxObligationEndDate" -> subscription.endDate.map(_.toString),
          "tradingName" -> subscription.orgName,
          "deregistrationDate" -> subscription.deregDate.map(_.toString),
          "voluntaryRegistration" -> subscription.activity.isVoluntaryRegistration,
          "smallProducer" -> subscription.activity.isSmallProducer,
          "largeProducer" -> subscription.activity.isLarge,
          "contractPacker" -> subscription.activity.isContractPacker,
          "importer" -> subscription.activity.isImporter,
          "primaryContactName" -> subscription.contact.name,
          "primaryPositionInCompany" -> subscription.contact.positionInCompany,
          "primaryTelephone" -> subscription.contact.phoneNumber,
          "primaryEmail" -> subscription.contact.email
        ),
        "businessAddress" -> Json.obj(
          "line1" -> "Glenfield Road",
          "line2" -> "Park View",
          "postCode" -> "SE12 4FH",
          "country" -> "GB"
        ),
        "sites" -> Json.arr()
      )
    )

    val exportedLitreBand: (Litres, Litres) = (109L, 110L)
    val wastedLitreBand: (Litres, Litres) = (111L, 112L)
    val returnsImporting = ReturnsImporting((111L, 112L), (111L, 112L))

    val returnsRequest = new ReturnsRequest(
      packaged = None,
      imported = Some(returnsImporting),
      exported = Some(exportedLitreBand),
      wastage = Some(wastedLitreBand)
    )

    val returnResponseByStatus: Map[Int, JsObject] = Map(
      CREATED -> Json.obj(
        "success" -> Json.obj(
          "formBundleNumber" -> "123456789019"
        )
      ),
      INTERNAL_SERVER_ERROR -> Json.obj(
        "error" -> Json.obj(
          "code" -> "500",
          "message" -> "Just a simple error message"
        )
      )
    )

    "create a subscription successfully" in {

      val createSubscriptionResponse = CreateSubscriptionResponse(
        processingDate = LocalDateTime.now(),
        formBundleNumber = "top-notch-form-bundle-number"
      )

      stubFor(
        post(urlEqualTo("/soft-drinks/subscription/utr/12345678910"))
          .withHeader(
            CORRELATION_ID_KEY,
            matching(
              "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
            )
          )
          .withHeader(X_ORIGINATING_SYSTEM_KEY, equalTo("SDIL"))
          .withHeader(X_RECEIPT_DATE_KEY, matching("""\d{4}-\d{2}-\d{2}T.*Z"""))
          .withHeader(X_TRANSMITTING_SYSTEM_KEY, equalTo("HIP"))
          .withRequestBody(equalToJson(Json.toJson(subscription).toString))
          .willReturn(
            aResponse()
              .withStatus(CREATED)
              .withBody(
                Json
                  .obj(
                    "success" -> Json.obj(
                      "processingDate" -> createSubscriptionResponse.processingDate,
                      "formBundleNumber" -> "top-notch-form-bundle-number"
                    )
                  )
                  .toString()
              )
          )
      )
      await(
        hipConnector.createSubscription(subscription, "utr", "12345678910")
      ).formBundleNumber mustBe createSubscriptionResponse.formBundleNumber
    }

    "convert 429 from createSubscription into 503 UpstreamErrorResponse" in {

      stubFor(
        post(urlEqualTo("/soft-drinks/subscription/utr/00000000000"))
          .withHeader(
            CORRELATION_ID_KEY,
            matching(
              "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
            )
          )
          .withHeader(X_ORIGINATING_SYSTEM_KEY, equalTo("SDIL"))
          .withHeader(X_RECEIPT_DATE_KEY, matching("""\d{4}-\d{2}-\d{2}T.*Z"""))
          .withHeader(X_TRANSMITTING_SYSTEM_KEY, equalTo("HIP"))
          .withRequestBody(equalToJson(Json.toJson(subscription.copy(utr = "00000000000")).toString()))
          .willReturn(
            aResponse()
              .withStatus(TOO_MANY_REQUESTS)
          )
      )

      val expected =
        hipConnector.createSubscription(subscription.copy(utr = "00000000000"), "utr", "00000000000").failed.futureValue

      expected shouldBe a[UpstreamErrorResponse]
      expected.asInstanceOf[UpstreamErrorResponse].statusCode shouldBe SERVICE_UNAVAILABLE
    }

    "retrieve subscription details successfully" in {

      val expectedSubscription = subscription.copy(
        orgType = None,
        activity = RetrievedActivity(
          isProducer = true,
          isLarge = false,
          isContractPacker = true,
          isImporter = true
        )
      )

      stubFor(
        get(urlEqualTo("/soft-drinks/subscription/details/utr/12345678910"))
          .withHeader(
            CORRELATION_ID_KEY,
            matching(
              "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
            )
          )
          .withHeader(X_ORIGINATING_SYSTEM_KEY, equalTo("SDIL"))
          .withHeader(X_RECEIPT_DATE_KEY, matching("""\d{4}-\d{2}-\d{2}T.*Z"""))
          .withHeader(X_TRANSMITTING_SYSTEM_KEY, equalTo("HIP"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(hipRetrieveSubscriptionDetailsSuccessResponseJson.toString())
          )
      )

      org.mockito.Mockito.when(subscriptions.list(subscription.utr)).thenReturn(Future.successful(List.empty))
      org.mockito.Mockito.when(subscriptions.insert(subscription.utr, subscription)).thenReturn(Future.successful(()))

      val result =
        hipConnector.retrieveSubscriptionDetails("utr", "12345678910").futureValue

      result shouldBe Some(expectedSubscription)
    }

    "return None when retrieveSubscriptionDetails receives 204" in {

      stubFor(
        get(urlEqualTo("/soft-drinks/subscription/details/utr/12345678911"))
          .withHeader(
            CORRELATION_ID_KEY,
            matching(
              "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
            )
          )
          .withHeader(X_ORIGINATING_SYSTEM_KEY, equalTo("SDIL"))
          .withHeader(X_RECEIPT_DATE_KEY, matching("""\d{4}-\d{2}-\d{2}T.*Z"""))
          .withHeader(X_TRANSMITTING_SYSTEM_KEY, equalTo("HIP"))
          .willReturn(
            aResponse()
              .withStatus(NO_CONTENT)
              .withBody(Json.toJson(subscription).toString())
          )
      )

      val result =
        hipConnector.retrieveSubscriptionDetails("utr", "12345678911").futureValue

      result shouldBe None
    }

    "return None when retrieveSubscriptionDetails receives 404" in {
      stubFor(
        get(urlEqualTo("/soft-drinks/subscription/details/utr/12345678912"))
          .withHeader(
            CORRELATION_ID_KEY,
            matching(
              "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
            )
          )
          .withHeader(X_ORIGINATING_SYSTEM_KEY, equalTo("SDIL"))
          .withHeader(X_RECEIPT_DATE_KEY, matching("""\d{4}-\d{2}-\d{2}T.*Z"""))
          .withHeader(X_TRANSMITTING_SYSTEM_KEY, equalTo("HIP"))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
              .withBody(Json.toJson(subscription).toString())
          )
      )

      val result =
        hipConnector.retrieveSubscriptionDetails("utr", "12345678912").futureValue

      result shouldBe None
    }

    "convert 429 from retrieveSubscriptionDetails into 503 UpstreamErrorResponse" in {

      stubFor(
        get(urlEqualTo("/soft-drinks/subscription/details/utr/1234567891013"))
          .withHeader(
            CORRELATION_ID_KEY,
            matching(
              "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
            )
          )
          .withHeader(X_ORIGINATING_SYSTEM_KEY, equalTo("SDIL"))
          .withHeader(X_RECEIPT_DATE_KEY, matching("""\d{4}-\d{2}-\d{2}T.*Z"""))
          .withHeader(X_TRANSMITTING_SYSTEM_KEY, equalTo("HIP"))
          .willReturn(
            aResponse()
              .withStatus(TOO_MANY_REQUESTS)
              .withBody(Json.toJson(subscription).toString())
          )
      )

      val result =
        hipConnector.retrieveSubscriptionDetails("utr", "1234567891013").failed.futureValue

      result shouldBe a[UpstreamErrorResponse]
      result.asInstanceOf[UpstreamErrorResponse].statusCode shouldBe SERVICE_UNAVAILABLE
    }

    "submit a return and return the raw HttpResponse" in {

      stubFor(
        post(urlEqualTo("/soft-drinks/XKSDIL000000022/return"))
          .withHeader(
            CORRELATION_ID_KEY,
            matching(
              "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
            )
          )
          .withHeader(X_ORIGINATING_SYSTEM_KEY, equalTo("SDIL"))
          .withHeader(X_RECEIPT_DATE_KEY, matching("""\d{4}-\d{2}-\d{2}T.*Z"""))
          .withHeader(X_TRANSMITTING_SYSTEM_KEY, equalTo("HIP"))
          .willReturn(
            aResponse()
              .withStatus(CREATED)
              .withBody(returnResponseByStatus(CREATED).toString)
          )
      )

      val result =
        hipConnector.submitReturn("XKSDIL000000022", returnsRequest).futureValue

      result.status shouldBe CREATED
      result.json shouldBe returnResponseByStatus(CREATED)
    }

    "propagate failure from submitReturn" in {

      stubFor(
        post(urlEqualTo("/soft-drinks/XKSDIL000000022/return"))
          .withHeader(
            CORRELATION_ID_KEY,
            matching(
              "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
            )
          )
          .withHeader(X_ORIGINATING_SYSTEM_KEY, equalTo("SDIL"))
          .withHeader(X_RECEIPT_DATE_KEY, matching("""\d{4}-\d{2}-\d{2}T.*Z"""))
          .withHeader(X_TRANSMITTING_SYSTEM_KEY, equalTo("HIP"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
              .withBody(returnResponseByStatus(INTERNAL_SERVER_ERROR).toString)
          )
      )

      val result =
        hipConnector.submitReturn("XKSDIL000000022", returnsRequest).futureValue

      result.status shouldBe INTERNAL_SERVER_ERROR
      result.json shouldBe returnResponseByStatus(INTERNAL_SERVER_ERROR)
    }
  }
}
