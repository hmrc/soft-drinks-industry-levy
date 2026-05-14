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
import play.api.http.Status.*
import play.api.libs.json.*
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import sdil.models.ReturnPeriod
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.softdrinksindustrylevy.models.connectors.activity
import uk.gov.hmrc.softdrinksindustrylevy.models.json.des.create.*
import uk.gov.hmrc.softdrinksindustrylevy.models.*
import uk.gov.hmrc.softdrinksindustrylevy.util.{FakeApplicationSpec, WireMockMethods}

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.ExecutionContext
import scala.language.postfixOps

class HipConnectorSpec extends FakeApplicationSpec with HttpClientV2Helper with WireMockMethods {

  private val CORRELATION_ID_KEY: String = "correlationid"

  private val X_ORIGINATING_SYSTEM_KEY: String = "X-Originating-System"

  private val X_RECEIPT_DATE_KEY: String = "X-Receipt-Date"

  private val X_TRANSMITTING_SYSTEM_KEY: String = "X-Transmitting-System"

  val hipConnector: HipConnector = app.injector.instanceOf[HipConnector]

  implicit val hc: HeaderCarrier = new HeaderCarrier
  implicit lazy val executionContext: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  private val TWENTY_TWENTY_FOUR = 2024
  private val FIRST_QUARTER = 1
  implicit val period: ReturnPeriod = ReturnPeriod(TWENTY_TWENTY_FOUR, FIRST_QUARTER)

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
          "code"    -> "500",
          "message" -> "Just a simple error message"
        )
      )
    )

    def nullableString(value: Option[String]): JsValue =
      value.fold[JsValue](JsNull)(JsString.apply)

    def nullableDate(value: Option[LocalDate]): JsValue =
      value.fold[JsValue](JsNull)(date => JsString(date.toString))

    def hipSubscriptionDetails(
      sdilRegistrationNumber: String = subscription.sdilRef.get,
      taxObligationStartDate: LocalDate = subscription.liabilityDate,
      taxObligationEndDate: Option[LocalDate] = subscription.endDate,
      tradingName: String = subscription.orgName,
      deregistrationDate: Option[LocalDate] = subscription.deregDate,
      voluntaryRegistration: Boolean = subscription.activity.isVoluntaryRegistration,
      smallProducer: Boolean = subscription.activity.isSmallProducer,
      largeProducer: Boolean = subscription.activity.isLarge,
      contractPacker: Boolean = subscription.activity.isContractPacker,
      importer: Boolean = subscription.activity.isImporter,
      primaryContactName: Option[String] = subscription.contact.name,
      primaryPositionInCompany: Option[String] = subscription.contact.positionInCompany,
      primaryTelephone: String = subscription.contact.phoneNumber,
      primaryEmail: String = subscription.contact.email
    ): JsObject =
      Json.obj(
        "sdilRegistrationNumber"   -> sdilRegistrationNumber,
        "taxObligationStartDate"   -> taxObligationStartDate.toString,
        "taxObligationEndDate"     -> nullableDate(taxObligationEndDate),
        "tradingName"              -> tradingName,
        "deregistrationDate"       -> nullableDate(deregistrationDate),
        "voluntaryRegistration"    -> voluntaryRegistration,
        "smallProducer"            -> smallProducer,
        "largeProducer"            -> largeProducer,
        "contractPacker"           -> contractPacker,
        "importer"                 -> importer,
        "primaryContactName"       -> nullableString(primaryContactName),
        "primaryPositionInCompany" -> nullableString(primaryPositionInCompany),
        "primaryTelephone"         -> primaryTelephone,
        "primaryEmail"             -> primaryEmail
      )

    def hipAddress(
      line1: String = "Glenfield Road",
      line2: JsValue = JsString("Park View"),
      line3: JsValue = JsNull,
      line4: JsValue = JsNull,
      postCode: JsValue = JsString("SE12 4FH"),
      country: JsValue = JsString("GB")
    ): JsObject =
      Json.obj(
        "line1"    -> line1,
        "line2"    -> line2,
        "line3"    -> line3,
        "line4"    -> line4,
        "postCode" -> postCode,
        "country"  -> country
      )

    def hipSite(
      siteType: String,
      siteReference: String,
      siteAddress: JsObject,
      tradingName: JsValue = JsNull,
      closureDate: JsValue = JsNull
    ): JsObject =
      Json.obj(
        "siteReference" -> siteReference,
        "tradingName"   -> tradingName,
        "siteAddress"   -> siteAddress,
        "closureDate"   -> closureDate,
        "siteType"      -> siteType
      )

    def hipRetrieveSubscriptionDetailsResponse(
      utr: String = subscription.utr,
      details: JsObject = hipSubscriptionDetails(),
      businessAddress: JsObject = hipAddress(),
      sites: Option[Seq[JsObject]] = None
    ): JsObject = {
      val success = Json.obj(
        "utr"                 -> utr,
        "subscriptionDetails" -> details,
        "businessAddress"     -> businessAddress
      )

      Json.obj("success" -> sites.fold(success)(siteList => success + ("sites" -> JsArray(siteList))))
    }

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
                      "processingDate"   -> createSubscriptionResponse.processingDate,
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

    "throw an UpstreamErrorResponse when createSubscription returns an internal server error" in {

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
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      val expected =
        hipConnector
          .createSubscription(subscription.copy(utr = "00000000000"), "utr", "00000000000")
          .failed
          .futureValue

      expected shouldBe a[UpstreamErrorResponse]
      expected.asInstanceOf[UpstreamErrorResponse].statusCode shouldBe INTERNAL_SERVER_ERROR
    }

    "throw an UpstreamErrorResponse when createSubscription returns an unlisted error status" in {

      stubFor(
        post(urlEqualTo("/soft-drinks/subscription/utr/99999999999"))
          .willReturn(
            aResponse()
              .withStatus(TOO_MANY_REQUESTS)
              .withBody("Rate limited")
          )
      )

      val expected =
        hipConnector
          .createSubscription(subscription.copy(utr = "99999999999"), "utr", "99999999999")
          .failed
          .futureValue

      expected shouldBe a[UpstreamErrorResponse]
      expected.asInstanceOf[UpstreamErrorResponse].statusCode shouldBe TOO_MANY_REQUESTS
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
              .withBody(hipRetrieveSubscriptionDetailsResponse().toString())
          )
      )

      val result =
        hipConnector.retrieveSubscriptionDetails("utr", "12345678910").futureValue

      result shouldBe Some(expectedSubscription)
    }

    "retrieve subscription details with sites successfully" in {
      val responseJson = hipRetrieveSubscriptionDetailsResponse(
        utr = "site-utr",
        details = hipSubscriptionDetails(
          sdilRegistrationNumber = "XKSDIL000000022",
          taxObligationStartDate = LocalDate.of(2024, 4, 1),
          tradingName = "Test Org",
          primaryContactName = None,
          primaryPositionInCompany = None,
          primaryTelephone = "01234567890",
          primaryEmail = "test@example.com"
        ),
        businessAddress = hipAddress(
          line1 = "Business line 1",
          line2 = JsString(""),
          line4 = JsString("  "),
          postCode = JsNull,
          country = JsNull
        ),
        sites = Some(
          Seq(
            hipSite(
              siteType = "1",
              siteReference = "warehouse-ref",
              tradingName = JsString("Warehouse"),
              siteAddress = hipAddress(
                line1 = "Warehouse line 1",
                line2 = JsString("Warehouse line 2"),
                postCode = JsString("AA1 1AA")
              )
            ),
            hipSite(
              siteType = "2",
              siteReference = "production-ref",
              siteAddress = hipAddress(
                line1 = "Factory line 1",
                line2 = JsNull,
                line3 = JsString("Factory line 3"),
                postCode = JsNull,
                country = JsString("FR")
              ),
              closureDate = JsString("2025-01-01")
            )
          )
        )
      )

      stubFor(
        get(urlEqualTo("/soft-drinks/subscription/details/utr/12345678917"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(responseJson.toString())
          )
      )

      val expectedSubscription = Subscription(
        utr = "site-utr",
        sdilRef = Some("XKSDIL000000022"),
        orgName = "Test Org",
        orgType = None,
        address = UkAddress(List("Business line 1"), ""),
        activity = RetrievedActivity(
          isProducer = true,
          isLarge = false,
          isContractPacker = true,
          isImporter = true
        ),
        liabilityDate = LocalDate.of(2024, 4, 1),
        productionSites = List(
          Site(
            ForeignAddress(List("Factory line 1", "Factory line 3"), "FR"),
            Some("production-ref"),
            None,
            Some(LocalDate.of(2025, 1, 1))
          )
        ),
        warehouseSites = List(
          Site(
            UkAddress(List("Warehouse line 1", "Warehouse line 2"), "AA1 1AA"),
            Some("warehouse-ref"),
            Some("Warehouse"),
            None
          )
        ),
        contact = Contact(None, None, "01234567890", "test@example.com"),
        endDate = None,
        deregDate = None
      )

      val result =
        hipConnector.retrieveSubscriptionDetails("utr", "12345678917").futureValue

      result shouldBe Some(expectedSubscription)
    }

    "throw an UpstreamErrorResponse when retrieveSubscriptionDetails returns an internal server error" in {

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
              .withStatus(INTERNAL_SERVER_ERROR)
              .withBody(Json.toJson(subscription).toString())
          )
      )

      val result =
        hipConnector.retrieveSubscriptionDetails("utr", "12345678911").failed.futureValue

      result shouldBe a[UpstreamErrorResponse]
      result.asInstanceOf[UpstreamErrorResponse].statusCode shouldBe INTERNAL_SERVER_ERROR
    }

    "return None when retrieveSubscriptionDetails returns no subscription" in {

      Seq(
        NOT_FOUND           -> "12345678913",
        NO_CONTENT          -> "12345678914",
        SERVICE_UNAVAILABLE -> "12345678915",
        FORBIDDEN           -> "12345678916"
      ).foreach { case (status, idNumber) =>
        stubFor(
          get(urlEqualTo(s"/soft-drinks/subscription/details/utr/$idNumber"))
            .willReturn(
              aResponse()
                .withStatus(status)
            )
        )

        val result =
          hipConnector.retrieveSubscriptionDetails("utr", idNumber).futureValue

        result shouldBe None
      }
    }

    "throw an UpstreamErrorResponse when retrieveSubscriptionDetails returns an unlisted error status" in {

      stubFor(
        get(urlEqualTo("/soft-drinks/subscription/details/utr/12345678912"))
          .willReturn(
            aResponse()
              .withStatus(TOO_MANY_REQUESTS)
              .withBody("Rate limited")
          )
      )

      val result =
        hipConnector.retrieveSubscriptionDetails("utr", "12345678912").failed.futureValue

      result shouldBe a[UpstreamErrorResponse]
      result.asInstanceOf[UpstreamErrorResponse].statusCode shouldBe TOO_MANY_REQUESTS
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
