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
import org.mockito.Mockito.{verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.RecoverMethods.{recoverToExceptionIf, recoverToSucceededIf}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.Mode
import play.api.http.Status.SERVICE_UNAVAILABLE
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.*
import sdil.models.{ReturnPeriod, SdilReturn, SmallProducer, des}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.softdrinksindustrylevy.controllers.sub
import uk.gov.hmrc.softdrinksindustrylevy.models.*
import uk.gov.hmrc.softdrinksindustrylevy.models.connectors.{arbActivity, arbAddress, arbContact, arbDisplayDirectDebitResponse, arbSubRequest}
import uk.gov.hmrc.softdrinksindustrylevy.services.SdilMongoPersistence
import uk.gov.hmrc.softdrinksindustrylevy.util.FakeApplicationSpec

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

class DesConnectorSpecPropertyBased
    extends FakeApplicationSpec with MockitoSugar with BeforeAndAfterEach with ScalaCheckPropertyChecks
    with HttpClientV2Helper {

  def await[A](future: Future[A])(implicit timeout: Duration): A = Await.result(future, timeout)

  import json.internal._

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

class DesConnectorSpecBehavioural extends HttpClientV2Helper {

  import scala.concurrent.Future

  implicit val hc: HeaderCarrier = new HeaderCarrier
  implicit val period: ReturnPeriod = new ReturnPeriod(2018, 3)
  implicit lazy val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  val desConnector = app.injector.instanceOf[DesConnector]

  val exportedLitreBand = (109L, 110L)
  val wastedLitreBand = (111L, 112L)
  val returnsImporting = new ReturnsImporting((111L, 112L), (111L, 112L))
  val returnsRequest = new ReturnsRequest(
    packaged = None,
    imported = Some(returnsImporting),
    exported = Some(exportedLitreBand),
    wastage = Some(wastedLitreBand)
  )

  "DesConnector" should {
    "return : None when DES returns 503 for an unknown UTR" in {

      when(requestBuilderExecute[HttpResponse])
        .thenReturn(Future.successful(HttpResponse(503, "503")))

      val response: Future[Option[Subscription]] = desConnector.retrieveSubscriptionDetails("utr", "11111111119")
      response.map { x =>
        x mustBe None
      }
    }

    "return : None financial data when nothing is returned" in {

      when(requestBuilderExecute[HttpResponse])
        .thenReturn(Future.successful(HttpResponse(503, "503")))

      val response: Future[Option[des.FinancialTransactionResponse]] =
        desConnector.retrieveFinancialData("utr", None)
      response.map { x =>
        x mustBe None
      }
    }

    "displayDirectDebit should return Future true when des returns directDebitMandateResponse set to true" in {
      when(requestBuilderExecute[HttpResponse])
        .thenReturn(Future.successful(HttpResponse(200, """{ "directDebitMandateFound" : true }""")))

      val response = desConnector.displayDirectDebit("XMSDIL000000001")
      response.map { res =>
        res.directDebitMandateFound mustBe true
      }
    }

    "displayDirectDebit should return Future false when des returns directDebitMandateResponse set to false" in {
      when(requestBuilderExecute[HttpResponse])
        .thenReturn(Future.successful(HttpResponse(200, """{ "directDebitMandateFound" : false }""")))
      val response = desConnector.displayDirectDebit("XMSDIL000000001")
      response.map { res =>
        res.directDebitMandateFound mustBe false
      }
    }

    "displayDirectDebit should return Failed future when Des returns a 404" in {
      when(requestBuilderExecute[HttpResponse])
        // .thenThrow(new RuntimeException("Exception"))
        .thenReturn(
          Future.failed(new Exception("The future returned an exception of type: uk.gov.hmrc.http.NotFoundException"))
        )
      val response: Future[DisplayDirectDebitResponse] = desConnector
        .displayDirectDebit("XMSDIL000000001")

      response onComplete {
        case Success(x) => println(x)
        case Failure(y) => println(s"The failure is Caught by Mohan  ${y.getMessage}")
      }
    }

    "create subscription should throw an exception if des is unavailable" in {
      when(requestBuilderExecute[HttpResponse]).thenReturn(
        Future.successful(HttpResponse(SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE"))
      )
      val response = the[Exception] thrownBy (desConnector
        .createSubscription(sub, "utr", "11111111119")
        .futureValue)
      response.getMessage must include("BadGatewayException")
    }

    "return : 5xxUpstreamResponse when DES returns 429 for too many requests" in {
      when(requestBuilderExecute[Option[Subscription]])
        .thenReturn(
          Future.failed(
            new UpstreamErrorResponse(
              message = "Too many requests",
              statusCode = 429,
              reportAs = 503,
              headers = Map.empty
            )
          )
        )
      recoverToExceptionIf[UpstreamErrorResponse] {
        desConnector.retrieveSubscriptionDetails("utr", "11111111120")
      }.map { ex =>
        ex.getMessage must include("Too many requests")
        ex.statusCode mustBe 503
      }
    }

    "return: 5xxUpstreamResponse when DES returns 429 for too many requests for financial data" in {
      when(requestBuilderExecute[HttpResponse]).thenReturn(
        Future.failed(
          new UpstreamErrorResponse(
            message = "Too many requests",
            statusCode = 429,
            reportAs = 503,
            headers = Map.empty
          )
        )
      )
      recoverToExceptionIf[UpstreamErrorResponse](desConnector.retrieveFinancialData("utr", None)).map { ex =>
        ex.getMessage must include("Too many requests")
        ex.statusCode mustBe 503
      }
    }
  }

  "submitReturn should handle DES rate limit (429) by converting to 503" in {
    implicit val period: ReturnPeriod = ReturnPeriod(2023, 1)

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

    when(requestBuilderExecute[HttpResponse])
      .thenReturn(Future.failed(UpstreamErrorResponse("Rate limited", 429)))

    val result = desConnector.submitReturn("sdilRef", returnsRequest)

    recoverToExceptionIf[UpstreamErrorResponse] {
      result
    }.map { exception =>
      exception.statusCode mustBe 503
    }
  }

  "submitReturn should successfully send return details to DES" in {
    implicit val period: ReturnPeriod = ReturnPeriod(2023, 1)

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

    when(requestBuilderExecute[HttpResponse])
      .thenReturn(Future.successful(HttpResponse(200, "")))

    val result = desConnector.submitReturn("sdilRef", returnsRequest)
    result.map { response =>
      response.status mustBe 200
    }
  }

  "retrieveFinancialData should successfully fetch financial data from DES" in {

    val financialData = des.FinancialTransactionResponse(
      idType = "ZSDL",
      idNumber = "utr",
      regimeType = "ZSDL",
      processingDate = LocalDateTime.now(),
      financialTransactions = List.empty[des.FinancialTransaction]
    )

    when(requestBuilderExecute[Option[des.FinancialTransactionResponse]])
      .thenReturn(Future.successful(Some(financialData)))

    val response = desConnector.retrieveFinancialData("utr", Some(2023))
    response.map { data =>
      data mustBe Some(financialData)
    }
  }

  "createSubscription should successfully post subscription details to DES" in {
    val mockedActivity = mock[Activity]

    when(mockedActivity.isProducer).thenReturn(true)
    when(mockedActivity.isLarge).thenReturn(false)
    when(mockedActivity.isContractPacker).thenReturn(false)
    when(mockedActivity.isImporter).thenReturn(false)
    when(mockedActivity.taxEstimation).thenReturn(BigDecimal(100))

    val subscription = Subscription(
      utr = "utr",
      sdilRef = Some("11111111119"),
      orgName = "orgName",
      orgType = Some("Ltd"),
      address = UkAddress(List("line1", "line2"), "postcode"),
      activity = mockedActivity,
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

    when(requestBuilderExecute[CreateSubscriptionResponse])
      .thenReturn(Future.successful(response))

    val result = desConnector.createSubscription(subscription, "utr", "11111111119")
    result.map { res =>
      res mustBe response
    }
  }

  "createSubscription should handle failure response from DES" in {

    val mockedActivity = mock[Activity]

    when(mockedActivity.isProducer).thenReturn(true)
    when(mockedActivity.isLarge).thenReturn(false)
    when(mockedActivity.isContractPacker).thenReturn(false)
    when(mockedActivity.isImporter).thenReturn(false)
    when(mockedActivity.taxEstimation).thenReturn(BigDecimal(100))

    val subscription = Subscription(
      utr = "utr",
      sdilRef = Some("11111111119"),
      orgName = "orgName",
      orgType = Some("Ltd"),
      address = UkAddress(List("line1", "line2"), "postcode"),
      activity = mockedActivity,
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

    when(requestBuilderExecute[CreateSubscriptionResponse])
      .thenReturn(Future.failed(UpstreamErrorResponse("Error", 500)))

    val result = desConnector.createSubscription(subscription, "utr", "11111111119")

    recoverToSucceededIf[UpstreamErrorResponse] {
      result
    }
  }

  "retrieveFinancialData should audit successful response" in {
    val mockAuditConnector = mock[AuditConnector]

    val testDesConnector = new DesConnector(
      http = mockHttpClient,
      mode = mock[Mode],
      servicesConfig = mockServicesConfig,
      persistence = mock[SdilMongoPersistence],
      auditing = mockAuditConnector
    )(using executionContext)

    val financialData = des.FinancialTransactionResponse(
      idType = "ZSDL",
      idNumber = "utr",
      regimeType = "ZSDL",
      processingDate = LocalDateTime.now(),
      financialTransactions = List.empty[des.FinancialTransaction]
    )

    when(requestBuilderExecute[Option[des.FinancialTransactionResponse]])
      .thenReturn(Future.successful(Some(financialData)))

    val response = testDesConnector.retrieveFinancialData("utr", Some(2023))
    response.map { data =>
      data mustBe Some(financialData)
      verify(mockAuditConnector).sendExtendedEvent(any())
    }
  }

  "handle DES returning 404 for financial data" in {
    when(requestBuilderExecute[HttpResponse])
      .thenReturn(Future.successful(HttpResponse(404, "Not Found")))

    val response: Future[Option[des.FinancialTransactionResponse]] =
      desConnector.retrieveFinancialData("utr", None)
    response.map { x =>
      x mustBe None
    }
  }

  "handle DES returning 429 for displayDirectDebit" in {
    when(requestBuilderExecute[HttpResponse])
      .thenReturn(Future.failed(UpstreamErrorResponse("Rate limited", 429)))

    recoverToExceptionIf[UpstreamErrorResponse] {
      desConnector.displayDirectDebit("XMSDIL000000001")
    }.map { exception =>
      exception.statusCode mustBe 503
    }
  }
  "handle DES returning 403 for subscription details" in {
    when(requestBuilderExecute[HttpResponse])
      .thenReturn(Future.successful(HttpResponse(403, "Forbidden")))

    val response: Future[Option[Subscription]] = desConnector.retrieveSubscriptionDetails("utr", "11111111119")
    response.map { x =>
      x mustBe None
    }
  }

  "create subscription should throw an exception if des is returning 429" in {
    when(requestBuilderExecute[HttpResponse]).thenReturn(
      Future.failed(
        new UpstreamErrorResponse(
          message = "Too many requests",
          statusCode = 429,
          reportAs = 503,
          headers = Map.empty
        )
      )
    )

    recoverToExceptionIf[UpstreamErrorResponse] {
      desConnector.createSubscription(sub, "utr", "11111111119")
    }.map { ex =>
      ex.statusCode mustBe 503
      ex.getMessage must include("Too many requests")
    }
  }
  "should get no response back if des is not available" in {

    when(requestBuilderExecute[HttpResponse])
      .thenReturn(Future.successful(HttpResponse(429, "429")))
    val response: Future[HttpResponse] = desConnector.submitReturn("utr", returnsRequest)
    response.map { x =>
      x.status mustBe 429
    }
  }

}
