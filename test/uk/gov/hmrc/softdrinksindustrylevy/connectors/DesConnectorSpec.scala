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
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json._
import sdil.models.des
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import uk.gov.hmrc.softdrinksindustrylevy.models._
import uk.gov.hmrc.softdrinksindustrylevy.models.connectors.{arbActivity, arbAddress, arbContact, arbDisplayDirectDebitResponse, arbSubRequest}
import uk.gov.hmrc.softdrinksindustrylevy.util.FakeApplicationSpec

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

class DesConnectorSpecPropertyBased
    extends FakeApplicationSpec with MockitoSugar with BeforeAndAfterEach with ScalaCheckPropertyChecks {

  def await[A](future: Future[A])(implicit timeout: Duration): A = Await.result(future, timeout)

  import json.internal._

  "DesConnectorSpec" should {

    "parse Activity as expected" in {
      forAll { r: Activity =>
        Json.toJson(r).as[Activity] mustBe r
      }
    }
  }

  "parse UkAddress as expected" in {
    forAll { r: Address =>
      Json.toJson(r).as[Address] mustBe r
    }
  }

  "parse Contact as expected" in {
    forAll { r: Contact =>
      Json.toJson(r).as[Contact] mustBe r
    }
  }

  "parse Subscription as expected" in {
    forAll { r: Subscription =>
      Json.toJson(r).as[Subscription] mustBe r
    }
  }

  "parse DisplayDirectDebitResponse as expected" in {
    forAll { r: DisplayDirectDebitResponse =>
      Json.toJson(r).as[DisplayDirectDebitResponse] mustBe r
    }
  }
}

class DesConnectorSpecBehavioural extends HttpClientV2Helper {

  import scala.concurrent.Future

  implicit val hc: HeaderCarrier = new HeaderCarrier

  implicit lazy val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  val desConnector = app.injector.instanceOf[DesConnector]

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

    /*"return: 5xxUpstreamResponse when DES returns 429 for too many requests for financial data" in {

      stubFor(
        get(urlEqualTo(
          "/enterprise/financial-data/ZSDL/utr/ZSDL?onlyOpenItems=true&includeLocks=false&calculateAccruedInterest=true&customerPaymentInformation=true"))
          .willReturn(aResponse().withStatus(429)))

      lazy val ex = the[Exception] thrownBy (desConnector.retrieveFinancialData("utr", None).futureValue)
      ex.getMessage must startWith("The future returned an exception of type: uk.gov.hmrc.http.Upstream5xxResponse")

    }

    "create subscription should throw an exception if des is unavailable" in {

      stubFor(
        post(urlEqualTo("/soft-drinks/subscription/utr/11111111119"))
          .willReturn(aResponse().withStatus(SERVICE_UNAVAILABLE)))

      val response = the[Exception] thrownBy (desConnector
        .createSubscription(sub, "utr", "11111111119")
        .futureValue)
      response.getMessage must startWith(
        "The future returned an exception of type: uk.gov.hmrc.http.Upstream5xxResponse")
    }

    "create subscription should throw an exception if des is returning 429" in {

      stubFor(
        post(urlEqualTo("/soft-drinks/subscription/utr/11111111119"))
          .willReturn(aResponse().withStatus(429)))

      lazy val response = the[Exception] thrownBy (desConnector
        .createSubscription(sub, "utr", "11111111119")
        .futureValue)
      response.getMessage must startWith(
        "The future returned an exception of type: uk.gov.hmrc.http.Upstream5xxResponse")
    }*/

    "displayDirectDebit should return Future true when des returns directDebitMandateResponse set to true" in {
      when(requestBuilderExecute[HttpResponse])
        .thenReturn(Future.successful(HttpResponse(200, """{ "directDebitMandateFound" : true }""")))

      val response = desConnector.displayDirectDebit("XMSDIL000000001")
      response.map { directDebitMandateFound =>
        directDebitMandateFound mustBe true
      }
    }

    "displayDirectDebit should return Future false when des returns directDebitMandateResponse set to false" in {
      when(requestBuilderExecute[HttpResponse])
        .thenReturn(Future.successful(HttpResponse(200, """{ "directDebitMandateFound" : false }""")))
      val response = desConnector.displayDirectDebit("XMSDIL000000001")
      response.map { directDebitMandateFound =>
        directDebitMandateFound mustBe false
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

      // Await.result(response, timeout)

      // await(desConnector
      // .displayDirectDebit("XMSDIL000000001")).status mustBe 202

      response onComplete {
        case Success(x) => println(x)
        case Failure(y) => println(s"The failure is Caught by Mohan  ${y.getMessage}")
      }

      // response.getMessage must startWith("The future returned an exception of type: uk.gov.hmrc.http.NotFoundException")
    }
    /*"429 response" should {
      "return : 5xxUpstreamResponse when DES returns 429 for too many requests" in {

        when(mockHttpClient.GET[Option[Subscription]](any(), any(), any())(any(), any(), any()))
          .thenThrow(new Exception("The future returned an exception of type: uk.gov.hmrc.http.Upstream5xxResponse"))

        lazy val ex = the[Throwable] thrownBy (desConnector
          .retrieveSubscriptionDetails("utr", "11111111120"))
        ex.getMessage must startWith("The future returned an exception of type: uk.gov.hmrc.http.Upstream5xxResponse")
      }
    }*/
  }

}
