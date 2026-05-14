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
import org.mockito.ArgumentMatchers.any
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status
import play.api.http.Status.*
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import sdil.models.ReturnPeriod
import sdil.models.des.FinancialTransaction.responseFormatter
import sdil.models.des.{FinancialTransaction, FinancialTransactionResponse}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.softdrinksindustrylevy.models.connectors.activity
import uk.gov.hmrc.softdrinksindustrylevy.models.json.des.create.*
import uk.gov.hmrc.softdrinksindustrylevy.models.{Contact, CreateSubscriptionResponse, RetrievedActivity, Site, Subscription, UkAddress}
import uk.gov.hmrc.softdrinksindustrylevy.util.{FakeApplicationSpec, WireMockMethods}

import java.time.format.DateTimeFormatter
import java.time.{Clock, Instant, LocalDate, LocalDateTime}
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class HipSubscriptionConnectorSpec
    extends FakeApplicationSpec with MockitoSugar with HttpClientV2Helper with WireMockMethods {

  private val CORRELATION_ID_KEY: String = "correlationid"
  private val CORRELATION_ID_VALUE: String = UUID.randomUUID().toString()

  private val X_ORIGINATING_SYSTEM_KEY: String = "X-Originating-System"
  private val X_ORIGINATING_SYSTEM_VALUE: String = "SDIL"

  private val X_RECEIPT_DATE_KEY: String = "X-Receipt-Date"
  private val X_RECEIPT_DATE_VALUE: String =
    DateTimeFormatter.ISO_INSTANT.format(Instant.now(Clock.systemDefaultZone()))

  private val X_TRANSMITTING_SYSTEM_KEY: String = "X-Transmitting-System"
  private val X_TRANSMITTING_SYSTEM_VALUE: String = "HIP"

  val sdilConnector: SdilConnector = app.injector.instanceOf[SdilConnector]

  implicit val hc: HeaderCarrier = new HeaderCarrier
  implicit lazy val executionContext: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  private val TWENTY_TWENTY_FOUR = 2024
  private val FIRST_QUARTER = 1
  implicit val period: ReturnPeriod = ReturnPeriod(TWENTY_TWENTY_FOUR, FIRST_QUARTER)

  val expectedHeaders: Seq[(String, String)] =
    Seq(
      CORRELATION_ID_KEY        -> CORRELATION_ID_VALUE,
      X_ORIGINATING_SYSTEM_KEY  -> X_ORIGINATING_SYSTEM_VALUE,
      X_RECEIPT_DATE_KEY        -> X_RECEIPT_DATE_VALUE,
      X_TRANSMITTING_SYSTEM_KEY -> X_TRANSMITTING_SYSTEM_VALUE
    )

  "HipSubscriptionConnector" should {

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
          "sdilRegistrationNumber"   -> subscription.sdilRef.get,
          "taxObligationStartDate"   -> subscription.liabilityDate.toString,
          "taxObligationEndDate"     -> subscription.endDate.map(_.toString),
          "tradingName"              -> subscription.orgName,
          "deregistrationDate"       -> subscription.deregDate.map(_.toString),
          "voluntaryRegistration"    -> subscription.activity.isVoluntaryRegistration,
          "smallProducer"            -> subscription.activity.isSmallProducer,
          "largeProducer"            -> subscription.activity.isLarge,
          "contractPacker"           -> subscription.activity.isContractPacker,
          "importer"                 -> subscription.activity.isImporter,
          "primaryContactName"       -> subscription.contact.name,
          "primaryPositionInCompany" -> subscription.contact.positionInCompany,
          "primaryTelephone"         -> subscription.contact.phoneNumber,
          "primaryEmail"             -> subscription.contact.email
        ),
        "businessAddress" -> Json.obj(
          "line1"    -> "Glenfield Road",
          "line2"    -> "Park View",
          "postCode" -> "SE12 4FH",
          "country"  -> "GB"
        ),
        "sites" -> Json.arr()
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
                      "processingDate"   -> createSubscriptionResponse.processingDate,
                      "formBundleNumber" -> "top-notch-form-bundle-number"
                    )
                  )
                  .toString()
              )
          )
      )
      await(
        sdilConnector.createSubscription(subscription, "utr", "12345678910")
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
        sdilConnector
          .createSubscription(subscription.copy(utr = "00000000000"), "utr", "00000000000")
          .failed
          .futureValue

      expected shouldBe a[UpstreamErrorResponse]
      expected.asInstanceOf[UpstreamErrorResponse].statusCode shouldBe INTERNAL_SERVER_ERROR
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
        sdilConnector.retrieveSubscriptionDetails("utr", "12345678910").futureValue

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
        sdilConnector.retrieveSubscriptionDetails("utr", "12345678911").failed.futureValue

      result shouldBe a[UpstreamErrorResponse]
      result.asInstanceOf[UpstreamErrorResponse].statusCode shouldBe INTERNAL_SERVER_ERROR
    }

    /*
    All the tests are borrowed from the DesConnector for completeness of coverage and to make sure that the endpoints
    that will not be migrated to the HIP will continue to work as expected.
    The tests for the endpoints that are being migrated will be removed once the migration is complete and the
    endpoints are fully served by the HIP.
     */

    "return : None financial data when nothing is returned" in {

      when(method = GET, uri = "/enterprise/financial-data/ZSDL/utr/ZSDL", headers = expectedHeaders.toMap)
        .thenReturn(
          SERVICE_UNAVAILABLE
        )
      await(
        sdilConnector.retrieveFinancialData("utr", None)
      ) shouldBe None

    }

    "displayDirectDebit should return Future true when des returns directDebitMandateResponse set to true" in {
      val uri = "/cross-regime/direct-debits/zsdl/zsdl/XMSDIL000000001"

      when(method = GET, uri = uri).thenReturn(
        status = OK,
        body = """{ "directDebitMandateFound" : true }"""
      )
      val response = await(sdilConnector.displayDirectDebit("XMSDIL000000001"))
      response.directDebitMandateFound mustBe true
    }

    "displayDirectDebit should return Future false when des returns directDebitMandateResponse set to false" in {
      when(
        method = GET,
        uri = "/cross-regime/direct-debits/zsdl/zsdl/XMSDIL000000001"
      ).thenReturn(
        status = OK,
        body = """{ "directDebitMandateFound" : false }"""
      )
      await(
        sdilConnector.displayDirectDebit("XMSDIL000000001")
      ).directDebitMandateFound mustBe false

    }

    "displayDirectDebit should return Failed future when Des returns a 404" in {

      when(method = GET, uri = "/cross-regime/direct-debits/zsdl/zsdl/XMSDIL000000001")
        .thenReturn(status = NOT_FOUND)

      val result = sdilConnector.displayDirectDebit("XMSDIL000000001")

      result.failed.futureValue shouldBe a[uk.gov.hmrc.http.UpstreamErrorResponse]

    }

    "return: 5xxUpstreamResponse when DES returns 429 for too many requests for financial data" in {
      when(GET, "/enterprise/financial-data/ZSDL/utr/ZSDL").thenReturn(
        body = "Too many requests",
        status = TOO_MANY_REQUESTS
      )
      val result = sdilConnector.retrieveFinancialData("utr", None)
      result.failed.futureValue shouldBe a[uk.gov.hmrc.http.UpstreamErrorResponse]
    }

    "retrieveFinancialData should successfully fetch financial data from DES" in {
      val financialData = FinancialTransactionResponse(
        idType = "ZSDL",
        idNumber = "utr",
        regimeType = "ZSDL",
        processingDate = LocalDateTime.now(),
        financialTransactions = List.empty[FinancialTransaction]
      )
      val year = 2023

      when(GET, "/enterprise/financial-data/ZSDL/utr/ZSDL")
        .thenReturn(Status.OK, Some(financialData))
      org.mockito.Mockito
        .when(mockAuditConnector.sendExtendedEvent(any())(using any(), any()))
        .thenReturn(Future.successful(AuditResult.Success))

      val response = await(sdilConnector.retrieveFinancialData("utr", Some(year)))

      response.get shouldBe financialData

    }

    "retrieveFinancialData should audit successful response" in {
      val financialData = FinancialTransactionResponse(
        idType = "ZSDL",
        idNumber = "utr",
        regimeType = "ZSDL",
        processingDate = LocalDateTime.now(),
        financialTransactions = List.empty[FinancialTransaction]
      )

      when(
        method = GET,
        uri = "/enterprise/financial-data/ZSDL/utr/ZSDL",
        headers = Seq("Authorization" -> s"Bearer token", "Environment" -> "environment").toMap
      )
        .thenReturn(OK, Some(financialData))
      org.mockito.Mockito
        .when(mockAuditConnector.sendExtendedEvent(any())(using any(), any()))
        .thenReturn(Future.successful(AuditResult.Success))

      val year = 2023

      await(sdilConnector.retrieveFinancialData("utr", Some(year))) shouldBe Some(financialData)
    }

    "handle DES returning 404 for financial data" in {
      when(method = GET, uri = "enterprise/financial-data/ZSDL/utr/ZSDL", headers = expectedHeaders.toMap)
        .thenReturn(NOT_FOUND, "Not found")
      await(
        sdilConnector.retrieveFinancialData("utr", None)
      ) mustBe None
    }

    "handle DES returning 429 for displayDirectDebit" in {
      when(method = GET, uri = "/cross-regime/direct-debits/zsdl/zsdl/XMSDIL000000001")
        .thenReturn(status = TOO_MANY_REQUESTS, "Rate limited")

      intercept[UpstreamErrorResponse] {
        await {
          sdilConnector.displayDirectDebit("XMSDIL000000001")
        }
      }
    }

  }

}
