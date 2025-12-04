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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.http.Status
import play.api.http.Status.*
import play.api.libs.json.*
import play.api.libs.json.Format.GenericFormat
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import sdil.models.des.FinancialTransaction.responseFormatter
import sdil.models.des.{FinancialTransaction, FinancialTransactionResponse}
import sdil.models.{ReturnPeriod, SdilReturn, SmallProducer, des}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.softdrinksindustrylevy.controllers.{activity, sub}
import uk.gov.hmrc.softdrinksindustrylevy.models.*
import uk.gov.hmrc.softdrinksindustrylevy.models.connectors.{arbActivity, arbAddress, arbContact, arbDisplayDirectDebitResponse, arbSubRequest}
import uk.gov.hmrc.softdrinksindustrylevy.models.json.des.create.createSubscriptionResponseFormat
import uk.gov.hmrc.softdrinksindustrylevy.models.json.des.returns.returnsRequestFormat
import uk.gov.hmrc.softdrinksindustrylevy.services.SubscriptionWrapper.subFormat
import uk.gov.hmrc.softdrinksindustrylevy.util.{FakeApplicationSpec, WireMockMethods}

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

class DesConnectorSpecPropertyBased
    extends FakeApplicationSpec with MockitoSugar with BeforeAndAfterEach with ScalaCheckPropertyChecks
    with HttpClientV2Helper {

  def await[A](future: Future[A])(implicit timeout: Duration): A = Await.result(future, timeout)

  import json.internal.*

  "DesConnectorSpec" should {

    "parse Activity as expected" in {
      forAll { (r: Activity) =>
        Json.toJson(r).as[Activity] mustBe r
      }
    }
  }

  "parse UkAddress as expected" in {
    forAll { (r: Address) =>
      Json.toJson(r).as[Address] mustBe r
    }
  }

  "parse Contact as expected" in {
    forAll { (r: Contact) =>
      Json.toJson(r).as[Contact] mustBe r
    }
  }

  "parse Subscription as expected" in {
    forAll { (r: Subscription) =>
      Json.toJson(r).as[Subscription] mustBe r
    }
  }

  "parse DisplayDirectDebitResponse as expected" in {
    forAll { (r: DisplayDirectDebitResponse) =>
      Json.toJson(r).as[DisplayDirectDebitResponse] mustBe r
    }
  }
}

class DesConnectorSpecBehavioural extends HttpClientV2Helper with WireMockMethods {
  val emptyJsonBody = "{}"
  implicit val period: ReturnPeriod = new ReturnPeriod(2018, 3)

  val desConnector: DesConnector = app.injector.instanceOf[DesConnector]

  implicit val hc: HeaderCarrier = new HeaderCarrier
  implicit lazy val executionContext: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  val expectedHeaders: Seq[(String, String)] = Seq("Authorization" -> s"Bearer token", "Environment" -> "environment")

  val exportedLitreBand: (Litres, Litres) = (109L, 110L)
  val wastedLitreBand: (Litres, Litres) = (111L, 112L)
  val returnsImporting = ReturnsImporting((111L, 112L), (111L, 112L))

  val returnsRequest = new ReturnsRequest(
    packaged = None,
    imported = Some(returnsImporting),
    exported = Some(exportedLitreBand),
    wastage = Some(wastedLitreBand)
  )

  "DesConnector" should {
    "return : None when DES returns 503 for an unknown UTR" in {

      when(GET, "/soft-drinks/subscription/details/utr/11111111119")
        .thenReturn(SERVICE_UNAVAILABLE)
      await(
        desConnector.retrieveSubscriptionDetails("utr", "11111111119")
      ) shouldBe None
    }

    "return : None financial data when nothing is returned" in {

      when(method = GET, uri = "/enterprise/financial-data/ZSDL/utr/ZSDL", headers = expectedHeaders.toMap)
        .thenReturn(
          SERVICE_UNAVAILABLE
        )
      await(
        desConnector.retrieveFinancialData("utr", None)
      ) shouldBe None

    }

    "displayDirectDebit should return Future true when des returns directDebitMandateResponse set to true" in {
      val uri = "/cross-regime/direct-debits/zsdl/zsdl/XMSDIL000000001"

      when(method = GET, uri = uri).thenReturn(
        status = OK,
        body = """{ "directDebitMandateFound" : true }"""
      )
      val response = await(desConnector.displayDirectDebit("XMSDIL000000001"))
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
        desConnector.displayDirectDebit("XMSDIL000000001")
      ).directDebitMandateFound mustBe false

    }

    "displayDirectDebit should return Failed future when Des returns a 404" in {

      when(method = GET, uri = "/cross-regime/direct-debits/zsdl/zsdl/XMSDIL000000001")
        .thenReturn(status = NOT_FOUND)

      val result = desConnector.displayDirectDebit("XMSDIL000000001")

      result.failed.futureValue shouldBe a[uk.gov.hmrc.http.UpstreamErrorResponse]

    }

    "create subscription should throw an exception if des is unavailable" in {
      when(
        method = POST,
        uri = "/soft-drinks/subscription/utr/11111111119",
        body = Some(Json.toJson(sub).toString())
      ).thenReturn(SERVICE_UNAVAILABLE)
      val response = desConnector
        .createSubscription(sub, "utr", "11111111119")

      response.onComplete {
        case Success(_) => fail()
        case Failure(_) =>
          response.failed.futureValue shouldBe a[uk.gov.hmrc.http.UpstreamErrorResponse]
      }
    }

    "return : 5xxUpstreamResponse when DES returns 429 for too many requests" in {
      when(GET, "/soft-drinks/subscription/details/utr/11111111120")
        .thenReturn(TOO_MANY_REQUESTS, "Too many requests")

      val response = desConnector.retrieveSubscriptionDetails("utr", "11111111120")
      response.failed.futureValue shouldBe a[uk.gov.hmrc.http.UpstreamErrorResponse]
    }

    "return: 5xxUpstreamResponse when DES returns 429 for too many requests for financial data" in {
      when(GET, "/enterprise/financial-data/ZSDL/utr/ZSDL").thenReturn(
        body = "Too many requests",
        status = TOO_MANY_REQUESTS
      )
      val result = desConnector.retrieveFinancialData("utr", None)
      result.failed.futureValue shouldBe a[uk.gov.hmrc.http.UpstreamErrorResponse]
    }

    "submitReturn should handle DES rate limit (429) by converting to 503" in {
      val returnsRequest = ReturnsRequest(
        SdilReturn(
          ownBrand = (0L, 0L),
          packLarge = (0L, 0L),
          packSmall = List.empty[SmallProducer],
          importSmall = (0L, 0L),
          importLarge = (0L, 0L),
          `export` = (0L, 0L),
          wastage = (0L, 0L),
          submittedOn = None
        )
      )
      when(POST, "/soft-drinks/sdilRef/return")
        .thenReturn(TOO_MANY_REQUESTS)

      val result = await(desConnector.submitReturn("sdilRef", returnsRequest)).status
      result shouldBe TOO_MANY_REQUESTS
    }

    "submitReturn should successfully send return details to DES" in {

      val returnsRequest = ReturnsRequest(
        SdilReturn(
          ownBrand = (0L, 0L),
          packLarge = (0L, 0L),
          packSmall = List.empty[SmallProducer],
          importSmall = (0L, 0L),
          importLarge = (0L, 0L),
          `export` = (0L, 0L),
          wastage = (0L, 0L),
          submittedOn = None
        )
      )

      val returnUrl: String = "/soft-drinks/sdilRef/return"
      val httpResponse = HttpResponse(Status.OK, emptyJsonBody)
      when(
        method = POST,
        uri = returnUrl
      ).thenReturn(httpResponse.status, httpResponse.body)
      val result = await(desConnector.submitReturn("sdilRef", returnsRequest))
      result.status mustBe OK
    }

    "retrieveFinancialData should successfully fetch financial data from DES" in {
      val financialData = des.FinancialTransactionResponse(
        idType = "ZSDL",
        idNumber = "utr",
        regimeType = "ZSDL",
        processingDate = LocalDateTime.now(),
        financialTransactions = List.empty[des.FinancialTransaction]
      )
      val year = 2023

      when(GET, "/enterprise/financial-data/ZSDL/utr/ZSDL")
        .thenReturn(Status.OK, Some(financialData))

      val response = await(desConnector.retrieveFinancialData("utr", Some(year)))

      response.get shouldBe financialData

    }

    "createSubscription should successfully post subscription details to DES" in {
      val subscription = Subscription(
        utr = "utr",
        sdilRef = Some("11111111119"),
        orgName = "orgName",
        orgType = Some("Ltd"),
        address = UkAddress(List("line1", "line2"), "postcode"),
        activity = activity,
        liabilityDate = LocalDate.now(),
        productionSites = List.empty[Site],
        warehouseSites = List.empty[Site],
        contact = Contact(
          name = Some("John Doe"),
          positionInCompany = Some("Manager"),
          phoneNumber = "1234567890",
          email = "john.doe@example.com"
        ),
        endDate = None,
        deregDate = None
      )

      val response = CreateSubscriptionResponse(
        processingDate = LocalDateTime.now(),
        formBundleNumber = "bundle123"
      )
      when(method = POST, uri = "/soft-drinks/subscription/utr/11111111119")
        .thenReturn(OK, Some(response))

      await(
        desConnector.createSubscription(subscription, "utr", "11111111119")
      ) mustBe response
    }

    "createSubscription should handle failure response from DES" in {
      val subscription = Subscription(
        utr = "utr",
        sdilRef = Some("11111111119"),
        orgName = "orgName",
        orgType = Some("Ltd"),
        address = UkAddress(List("line1", "line2"), "postcode"),
        activity = activity,
        liabilityDate = LocalDate.now(),
        productionSites = List.empty[Site],
        warehouseSites = List.empty[Site],
        contact = Contact(
          name = Some("John Doe"),
          positionInCompany = Some("Manager"),
          phoneNumber = "1234567890",
          email = "john.doe@example.com"
        ),
        endDate = None,
        deregDate = None
      )
      when(method = POST, uri = "/soft-drinks/subscription/utr/11111111119").thenReturn(INTERNAL_SERVER_ERROR)

      intercept[UpstreamErrorResponse] {
        await {
          desConnector.createSubscription(subscription, "utr", "11111111119")
        }
      }
    }

    "retrieveFinancialData should audit successful response" in {
      val financialData = des.FinancialTransactionResponse(
        idType = "ZSDL",
        idNumber = "utr",
        regimeType = "ZSDL",
        processingDate = LocalDateTime.now(),
        financialTransactions = List.empty[des.FinancialTransaction]
      )

      when(method = GET, uri = "/enterprise/financial-data/ZSDL/utr/ZSDL", headers = expectedHeaders.toMap)
        .thenReturn(OK, Some(financialData))

      val year = 2023

      await(desConnector.retrieveFinancialData("utr", Some(year))) shouldBe Some(financialData)
      verify(mockAuditConnector, times(2)).sendExtendedEvent(any())(using any(), any())
    }
    "handle DES returning 404 for financial data" in {
      when(method = GET, uri = "enterprise/financial-data/ZSDL/utr/ZSDL", headers = expectedHeaders.toMap)
        .thenReturn(NOT_FOUND, "Not found")
      await(
        desConnector.retrieveFinancialData("utr", None)
      ) mustBe None
    }

    "handle DES returning 429 for displayDirectDebit" in {
      when(method = GET, uri = "/cross-regime/direct-debits/zsdl/zsdl/XMSDIL000000001")
        .thenReturn(status = TOO_MANY_REQUESTS, "Rate limited")

      intercept[UpstreamErrorResponse] {
        await {
          desConnector.displayDirectDebit("XMSDIL000000001")
        }
      }
    }

    "handle DES returning 403 for subscription details" in {
      when(GET, "/soft-drinks/subscription/details/utr/11111111119")
        .thenReturn(FORBIDDEN, "Forbidden")

      await(
        desConnector.retrieveSubscriptionDetails("utr", "11111111119")
      ) mustBe None
    }
  }

  "create subscription should throw an exception if des is returning 429" in {
    when(
      method = POST,
      uri = "/soft-drinks/subscription/utr/11111111119",
      body = Some(Json.toJson(sub).toString())
    ).thenReturn(
      TOO_MANY_REQUESTS,
      "Too many requests"
    )
    intercept[UpstreamErrorResponse] {
      await {
        desConnector.createSubscription(sub, "utr", "11111111119")
      }
    }
  }

  "should get no response back if des is not available" in {

    when(
      method = POST,
      uri = "/soft-drinks/utr/return",
      body = Some(Json.toJson(returnsRequest).toString()),
      headers = expectedHeaders.toMap
    ).thenReturn(SERVICE_UNAVAILABLE)

    await(
      desConnector.submitReturn("utr", returnsRequest)
    ).status shouldBe SERVICE_UNAVAILABLE
  }
}
