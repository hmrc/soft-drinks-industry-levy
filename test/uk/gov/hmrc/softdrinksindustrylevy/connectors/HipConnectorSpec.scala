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
import com.github.tomakehurst.wiremock.stubbing.Scenario
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

  private val AUTHORIZATION_KEY: String = "Authorization"

  private val HIP_AUTHORIZATION: String = "Basic Y2xpZW50LWlkOmNsaWVudC1zZWNyZXQ="

  private val HIP_API_ROOT: String = "/etmp/RESTAdapter/soft-drinks"

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

    def hipValidationError(code: String, text: String): JsObject =
      Json.obj(
        "errors" -> Json.obj(
          "processingDate" -> "2026-03-09T12:34:46Z",
          "code"           -> code,
          "text"           -> text
        )
      )

    def nullableDate(value: Option[LocalDate]): JsValue =
      value.fold[JsValue](JsNull)(date => JsString(date.toString))

    def hipSubscriptionDetails(
      sdilRegistrationNumber: String = subscription.sdilRef.get,
      taxObligationStartDate: LocalDate = subscription.liabilityDate,
      taxObligationEndDate: Option[LocalDate] = subscription.endDate,
      tradingName: String = subscription.orgName,
      deregistrationDate: Option[LocalDate] = subscription.deregDate,
      voluntaryRegistration: Option[Boolean] = Some(subscription.activity.isVoluntaryRegistration),
      smallProducer: Option[Boolean] = Some(subscription.activity.isSmallProducer),
      largeProducer: Option[Boolean] = Some(subscription.activity.isLarge),
      contractPacker: Option[Boolean] = Some(subscription.activity.isContractPacker),
      importer: Option[Boolean] = Some(subscription.activity.isImporter),
      primaryContactName: Option[String] = subscription.contact.name,
      primaryPositionInCompany: Option[String] = subscription.contact.positionInCompany,
      primaryTelephone: Option[String] = Some(subscription.contact.phoneNumber),
      primaryMobile: Option[String] = None,
      primaryEmail: Option[String] = Some(subscription.contact.email)
    ): JsObject = {
      val requiredFields: Seq[(String, JsValue)] = Seq(
        "sdilRegistrationNumber" -> JsString(sdilRegistrationNumber),
        "taxObligationStartDate" -> JsString(taxObligationStartDate.toString),
        "taxObligationEndDate"   -> nullableDate(taxObligationEndDate),
        "tradingName"            -> JsString(tradingName),
        "deregistrationDate"     -> nullableDate(deregistrationDate)
      )

      val optionalFields: Seq[(String, JsValue)] = Seq(
        voluntaryRegistration.map(value => "voluntaryRegistration" -> JsBoolean(value)),
        smallProducer.map(value => "smallProducer" -> JsBoolean(value)),
        largeProducer.map(value => "largeProducer" -> JsBoolean(value)),
        contractPacker.map(value => "contractPacker" -> JsBoolean(value)),
        importer.map(value => "importer" -> JsBoolean(value)),
        primaryContactName.map(value => "primaryContactName" -> JsString(value)),
        primaryPositionInCompany.map(value => "primaryPositionInCompany" -> JsString(value)),
        primaryTelephone.map(value => "primaryTelephone" -> JsString(value)),
        primaryMobile.map(value => "primaryMobile" -> JsString(value)),
        primaryEmail.map(value => "primaryEmail" -> JsString(value))
      ).flatten

      JsObject(requiredFields ++ optionalFields)
    }

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
      utr: Option[String] = Some(subscription.utr),
      details: JsObject = hipSubscriptionDetails(),
      businessAddress: JsObject = hipAddress(),
      sites: Option[Seq[JsObject]] = None
    ): JsObject = {
      val success = JsObject(
        Seq[Option[(String, JsValue)]](
          utr.map(value => "utr" -> JsString(value)),
          Some("subscriptionDetails" -> details),
          Some("businessAddress"     -> businessAddress)
        ).flatten
      )

      Json.obj("success" -> sites.fold(success)(siteList => success + ("sites" -> JsArray(siteList))))
    }

    "create a subscription successfully" in {

      val createSubscriptionResponse = CreateSubscriptionResponse(
        processingDate = LocalDateTime.now(),
        formBundleNumber = "top-notch-form-bundle-number"
      )

      stubFor(
        post(urlEqualTo(s"$HIP_API_ROOT/subscription/utr/12345678910"))
          .withHeader(
            CORRELATION_ID_KEY,
            matching(
              "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
            )
          )
          .withHeader(X_ORIGINATING_SYSTEM_KEY, equalTo("SDIL"))
          .withHeader(X_RECEIPT_DATE_KEY, matching("""\d{4}-\d{2}-\d{2}T.*Z"""))
          .withHeader(X_TRANSMITTING_SYSTEM_KEY, equalTo("HIP"))
          .withHeader(AUTHORIZATION_KEY, equalTo(HIP_AUTHORIZATION))
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
        post(urlEqualTo(s"$HIP_API_ROOT/subscription/utr/00000000000"))
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

    "throw an UpstreamErrorResponse when createSubscription returns a HIP validation error" in {

      stubFor(
        post(urlEqualTo(s"$HIP_API_ROOT/subscription/utr/12345678918"))
          .willReturn(
            aResponse()
              .withStatus(UNPROCESSABLE_ENTITY)
              .withBody(hipValidationError("007", "Business partner already has active subscription").toString())
          )
      )

      val expected =
        hipConnector
          .createSubscription(subscription.copy(utr = "12345678918"), "utr", "12345678918")
          .failed
          .futureValue

      expected shouldBe a[UpstreamErrorResponse]
      expected.asInstanceOf[UpstreamErrorResponse].statusCode shouldBe UNPROCESSABLE_ENTITY
      expected.getMessage must include("007")
      expected.getMessage must include("Business partner already has active subscription")
    }

    "throw an UpstreamErrorResponse when createSubscription returns an unlisted error status" in {

      stubFor(
        post(urlEqualTo(s"$HIP_API_ROOT/subscription/utr/99999999999"))
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
        get(urlEqualTo(s"$HIP_API_ROOT/subscription/utr/12345678910"))
          .withHeader(
            CORRELATION_ID_KEY,
            matching(
              "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
            )
          )
          .withHeader(X_ORIGINATING_SYSTEM_KEY, equalTo("SDIL"))
          .withHeader(X_RECEIPT_DATE_KEY, matching("""\d{4}-\d{2}-\d{2}T.*Z"""))
          .withHeader(X_TRANSMITTING_SYSTEM_KEY, equalTo("HIP"))
          .withHeader(AUTHORIZATION_KEY, equalTo(HIP_AUTHORIZATION))
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

    "retrieve subscription details when optional subscription detail fields are omitted" in {
      val idNumber = "12345678921"
      val responseJson = hipRetrieveSubscriptionDetailsResponse(
        utr = None,
        details = hipSubscriptionDetails(
          voluntaryRegistration = None,
          smallProducer = None,
          largeProducer = None,
          contractPacker = None,
          importer = None,
          primaryContactName = None,
          primaryPositionInCompany = None,
          primaryTelephone = None,
          primaryMobile = None,
          primaryEmail = None
        )
      )

      stubFor(
        get(urlEqualTo(s"$HIP_API_ROOT/subscription/utr/$idNumber"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(responseJson.toString())
          )
      )

      val expectedSubscription = subscription.copy(
        utr = idNumber,
        orgType = None,
        activity = RetrievedActivity(
          isProducer = false,
          isLarge = false,
          isContractPacker = false,
          isImporter = false
        ),
        contact = Contact(None, None, "", "")
      )

      val result =
        hipConnector.retrieveSubscriptionDetails("utr", idNumber).futureValue

      result shouldBe Some(expectedSubscription)
    }

    "not cache retrieveSubscriptionDetails responses that return None" in {
      val idNumber = "12345678923"
      val url = s"$HIP_API_ROOT/subscription/utr/$idNumber"
      val responseJson = hipRetrieveSubscriptionDetailsResponse(utr = Some(idNumber))

      stubFor(
        get(urlEqualTo(url))
          .inScenario("retrieve subscription None is not cached")
          .whenScenarioStateIs(Scenario.STARTED)
          .willReturn(aResponse().withStatus(SERVICE_UNAVAILABLE))
          .willSetStateTo("subscription available")
      )

      stubFor(
        get(urlEqualTo(url))
          .inScenario("retrieve subscription None is not cached")
          .whenScenarioStateIs("subscription available")
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(responseJson.toString())
          )
      )

      val expectedSubscription = subscription.copy(
        utr = idNumber,
        orgType = None,
        activity = RetrievedActivity(
          isProducer = true,
          isLarge = false,
          isContractPacker = true,
          isImporter = true
        )
      )

      hipConnector.retrieveSubscriptionDetails("utr", idNumber).futureValue shouldBe None
      hipConnector.retrieveSubscriptionDetails("utr", idNumber).futureValue shouldBe Some(expectedSubscription)

      verify(2, getRequestedFor(urlEqualTo(url)))
    }

    "retrieve subscription details using primaryMobile when primaryTelephone is omitted" in {
      val responseJson = hipRetrieveSubscriptionDetailsResponse(
        details = hipSubscriptionDetails(
          primaryTelephone = None,
          primaryMobile = Some("07777666555")
        )
      )

      stubFor(
        get(urlEqualTo(s"$HIP_API_ROOT/subscription/utr/12345678922"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(responseJson.toString())
          )
      )

      val expectedSubscription = subscription.copy(
        orgType = None,
        activity = RetrievedActivity(
          isProducer = true,
          isLarge = false,
          isContractPacker = true,
          isImporter = true
        ),
        contact = subscription.contact.copy(phoneNumber = "07777666555")
      )

      val result =
        hipConnector.retrieveSubscriptionDetails("utr", "12345678922").futureValue

      result shouldBe Some(expectedSubscription)
    }

    "fail when HIP omits utr for a non-UTR lookup" in {
      val sdilRef = "XKSDIL000000022"
      val responseJson = hipRetrieveSubscriptionDetailsResponse(
        utr = None,
        details = hipSubscriptionDetails(sdilRegistrationNumber = sdilRef)
      )

      stubFor(
        get(urlEqualTo(s"$HIP_API_ROOT/subscription/sdil/$sdilRef"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(responseJson.toString())
          )
      )

      val result =
        hipConnector.retrieveSubscriptionDetails("sdil", sdilRef).failed.futureValue

      result shouldBe a[IllegalArgumentException]
      result.getMessage must include("utr")
    }

    "retrieve subscription details with sites successfully" in {
      val responseJson = hipRetrieveSubscriptionDetailsResponse(
        utr = Some("site-utr"),
        details = hipSubscriptionDetails(
          sdilRegistrationNumber = "XKSDIL000000022",
          taxObligationStartDate = LocalDate.of(2024, 4, 1),
          tradingName = "Test Org",
          primaryContactName = None,
          primaryPositionInCompany = None,
          primaryTelephone = Some("01234567890"),
          primaryEmail = Some("test@example.com")
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
        get(urlEqualTo(s"$HIP_API_ROOT/subscription/utr/12345678917"))
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
        get(urlEqualTo(s"$HIP_API_ROOT/subscription/utr/12345678911"))
          .withHeader(
            CORRELATION_ID_KEY,
            matching(
              "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
            )
          )
          .withHeader(X_ORIGINATING_SYSTEM_KEY, equalTo("SDIL"))
          .withHeader(X_RECEIPT_DATE_KEY, matching("""\d{4}-\d{2}-\d{2}T.*Z"""))
          .withHeader(X_TRANSMITTING_SYSTEM_KEY, equalTo("HIP"))
          .withHeader(AUTHORIZATION_KEY, equalTo(HIP_AUTHORIZATION))
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

    "return None when retrieveSubscriptionDetails returns a HIP ID not found validation error" in {

      stubFor(
        get(urlEqualTo(s"$HIP_API_ROOT/subscription/utr/12345678919"))
          .willReturn(
            aResponse()
              .withStatus(UNPROCESSABLE_ENTITY)
              .withBody(hipValidationError("002", "ID not Found").toString())
          )
      )

      val result =
        hipConnector.retrieveSubscriptionDetails("utr", "12345678919").futureValue

      result shouldBe None
    }

    "throw an UpstreamErrorResponse when retrieveSubscriptionDetails returns another HIP validation error" in {

      stubFor(
        get(urlEqualTo(s"$HIP_API_ROOT/subscription/utr/12345678920"))
          .willReturn(
            aResponse()
              .withStatus(UNPROCESSABLE_ENTITY)
              .withBody(hipValidationError("003", "Request could not be processed").toString())
          )
      )

      val result =
        hipConnector.retrieveSubscriptionDetails("utr", "12345678920").failed.futureValue

      result shouldBe a[UpstreamErrorResponse]
      result.asInstanceOf[UpstreamErrorResponse].statusCode shouldBe UNPROCESSABLE_ENTITY
      result.getMessage must include("003")
      result.getMessage must include("Request could not be processed")
    }

    "return None when retrieveSubscriptionDetails returns no subscription" in {

      Seq(
        NOT_FOUND           -> "12345678913",
        NO_CONTENT          -> "12345678914",
        SERVICE_UNAVAILABLE -> "12345678915",
        FORBIDDEN           -> "12345678916"
      ).foreach { case (status, idNumber) =>
        stubFor(
          get(urlEqualTo(s"$HIP_API_ROOT/subscription/utr/$idNumber"))
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
        get(urlEqualTo(s"$HIP_API_ROOT/subscription/utr/12345678912"))
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
        post(urlEqualTo(s"$HIP_API_ROOT/return/XKSDIL000000022"))
          .withHeader(
            CORRELATION_ID_KEY,
            matching(
              "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
            )
          )
          .withHeader(X_ORIGINATING_SYSTEM_KEY, equalTo("SDIL"))
          .withHeader(X_RECEIPT_DATE_KEY, matching("""\d{4}-\d{2}-\d{2}T.*Z"""))
          .withHeader(X_TRANSMITTING_SYSTEM_KEY, equalTo("HIP"))
          .withHeader(AUTHORIZATION_KEY, equalTo(HIP_AUTHORIZATION))
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

    "throw an UpstreamErrorResponse when submitReturn returns a HIP validation error" in {

      stubFor(
        post(urlEqualTo(s"$HIP_API_ROOT/return/XKSDIL000000022"))
          .willReturn(
            aResponse()
              .withStatus(UNPROCESSABLE_ENTITY)
              .withBody(hipValidationError("040", "Invalid Period Key").toString())
          )
      )

      val result =
        hipConnector.submitReturn("XKSDIL000000022", returnsRequest).failed.futureValue

      result shouldBe a[UpstreamErrorResponse]
      result.asInstanceOf[UpstreamErrorResponse].statusCode shouldBe UNPROCESSABLE_ENTITY
      result.getMessage must include("040")
      result.getMessage must include("Invalid Period Key")
    }

    "return the response when submitReturn returns an obligation already fulfilled validation error" in {

      val alreadyFulfilledResponse = hipValidationError("044", "Obligation Already Fulfilled")

      stubFor(
        post(urlEqualTo(s"$HIP_API_ROOT/return/XKSDIL000000022"))
          .willReturn(
            aResponse()
              .withStatus(UNPROCESSABLE_ENTITY)
              .withBody(alreadyFulfilledResponse.toString())
          )
      )

      val result =
        hipConnector.submitReturn("XKSDIL000000022", returnsRequest).futureValue

      result.status shouldBe UNPROCESSABLE_ENTITY
      result.json shouldBe alreadyFulfilledResponse
    }

    "throw an UpstreamErrorResponse when submitReturn fails" in {

      stubFor(
        post(urlEqualTo(s"$HIP_API_ROOT/return/XKSDIL000000022"))
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
        hipConnector.submitReturn("XKSDIL000000022", returnsRequest).failed.futureValue

      result shouldBe a[UpstreamErrorResponse]
      result.asInstanceOf[UpstreamErrorResponse].statusCode shouldBe INTERNAL_SERVER_ERROR
    }
  }
}
