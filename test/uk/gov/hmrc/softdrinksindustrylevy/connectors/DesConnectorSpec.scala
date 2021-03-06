/*
 * Copyright 2021 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json._
import sdil.models.des
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.softdrinksindustrylevy.models._
import uk.gov.hmrc.softdrinksindustrylevy.models.connectors.{arbActivity, arbAddress, arbContact, arbDisplayDirectDebitResponse, arbSubRequest, sub}

class DesConnectorSpecPropertyBased extends FunSuite with ScalaCheckPropertyChecks with Matchers {

  import json.internal._

  test("∀ Activity: parse(toJson(x)) = x") {
    forAll { r: Activity =>
      Json.toJson(r).as[Activity] should be(r)
    }
  }

  test("∀ UkAddress: parse(toJson(x)) = x") {
    forAll { r: Address =>
      Json.toJson(r).as[Address] should be(r)
    }
  }

  test("∀ Contact: parse(toJson(x)) = x") {
    forAll { r: Contact =>
      Json.toJson(r).as[Contact] should be(r)
    }
  }

  test("∀ Subscription: parse(toJson(x)) = x") {
    forAll { r: Subscription =>
      Json.toJson(r).as[Subscription] should be(r)
    }
  }

  test("∀ DisplayDirectDebitResponse: parse(toJson(x)) = x") {
    forAll { r: DisplayDirectDebitResponse =>
      Json.toJson(r).as[DisplayDirectDebitResponse] should be(r)
    }
  }

}

class DesConnectorSpecBehavioural extends WiremockSpec {

  import play.api.test.Helpers.SERVICE_UNAVAILABLE

  import scala.concurrent.Future

  implicit val hc: HeaderCarrier = new HeaderCarrier

  object TestDesConnector
      extends DesConnector(httpClient, environment.mode, servicesConfig, testPersistence, auditConnector) {
    override val desURL: String = mockServerUrl
    override val desDirectDebitUrl: String = mockServerUrl
  }

  "DesConnector" should {
    "return : None when DES returns 503 for an unknown UTR" in {

      stubFor(
        get(urlEqualTo("/soft-drinks/subscription/details/utr/11111111119"))
          .willReturn(aResponse().withStatus(SERVICE_UNAVAILABLE)))

      val response: Future[Option[Subscription]] = TestDesConnector.retrieveSubscriptionDetails("utr", "11111111119")
      response.map { x =>
        x mustBe None
      }
    }

    "return : None financial data when nothing is returned" in {

      stubFor(
        get(urlEqualTo("/enterprise/financial-data/ZSDL/"))
          .willReturn(aResponse().withStatus(SERVICE_UNAVAILABLE)))

      val response: Future[Option[des.FinancialTransactionResponse]] =
        TestDesConnector.retrieveFinancialData("utr", None)
      response.map { x =>
        x mustBe None
      }
    }

    "return: 5xxUpstreamResponse when DES returns 429 for too many requests for financial data" in {

      stubFor(
        get(urlEqualTo(
          "/enterprise/financial-data/ZSDL/utr/ZSDL?onlyOpenItems=true&includeLocks=false&calculateAccruedInterest=true&customerPaymentInformation=true"))
          .willReturn(aResponse().withStatus(429)))

      lazy val ex = the[Exception] thrownBy (TestDesConnector.retrieveFinancialData("utr", None).futureValue)
      ex.getMessage must startWith("The future returned an exception of type: uk.gov.hmrc.http.Upstream5xxResponse")

    }

    "create subscription should throw an exception if des is unavailable" in {

      stubFor(
        post(urlEqualTo("/soft-drinks/subscription/utr/11111111119"))
          .willReturn(aResponse().withStatus(SERVICE_UNAVAILABLE)))

      val response = the[Exception] thrownBy (TestDesConnector
        .createSubscription(sub, "utr", "11111111119")
        .futureValue)
      response.getMessage must startWith(
        "The future returned an exception of type: uk.gov.hmrc.http.Upstream5xxResponse")
    }

    "create subscription should throw an exception if des is returning 429" in {

      stubFor(
        post(urlEqualTo("/soft-drinks/subscription/utr/11111111119"))
          .willReturn(aResponse().withStatus(429)))

      lazy val response = the[Exception] thrownBy (TestDesConnector
        .createSubscription(sub, "utr", "11111111119")
        .futureValue)
      response.getMessage must startWith(
        "The future returned an exception of type: uk.gov.hmrc.http.Upstream5xxResponse")
    }

    "displayDirectDebit should return Future true when des returns directDebitMandateResponse set to true" in {
      stubFor(
        get(urlEqualTo("/cross-regime/direct-debits/zsdl/zsdl/XMSDIL000000001"))
          .willReturn(aResponse().withBody("""{ "directDebitMandateFound" : true }""").withStatus(200)))
      val response = TestDesConnector.displayDirectDebit("XMSDIL000000001").futureValue
      response.directDebitMandateFound mustBe true
    }

    "displayDirectDebit should return Future false when des returns directDebitMandateResponse set to false" in {
      stubFor(
        get(urlEqualTo("/cross-regime/direct-debits/zsdl/zsdl/XMSDIL000000001"))
          .willReturn(aResponse().withBody("""{ "directDebitMandateFound" : false }""").withStatus(200)))
      val response = TestDesConnector.displayDirectDebit("XMSDIL000000001").futureValue
      response.directDebitMandateFound mustBe false
    }

    "displayDirectDebit should return Failed future when Des returns a 404" in {
      stubFor(
        get(urlEqualTo("/cross-regime/direct-debits/zsdl/zsdl/XMSDIL000000001"))
          .willReturn(aResponse().withStatus(404)))
      val response = the[Exception] thrownBy (TestDesConnector
        .displayDirectDebit("XMSDIL000000001")
        .futureValue)
      response.getMessage must startWith("The future returned an exception of type: uk.gov.hmrc.http.NotFoundException")
    }
  }

  "429 response" should {
    "return : 5xxUpstreamResponse when DES returns 429 for too many requests" in {

      stubFor(
        get(urlEqualTo("/soft-drinks/subscription/details/utr/11111111120"))
          .willReturn(aResponse().withStatus(429)))

      lazy val ex = the[Exception] thrownBy (TestDesConnector
        .retrieveSubscriptionDetails("utr", "11111111120")
        .futureValue)
      ex.getMessage must startWith("The future returned an exception of type: uk.gov.hmrc.http.Upstream5xxResponse")
    }
  }

}
