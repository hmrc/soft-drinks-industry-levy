/*
 * Copyright 2019 HM Revenue & Customs
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

import java.time.LocalDate

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.PropertyChecks
import play.api.libs.json._
import sdil.models.des
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.softdrinksindustrylevy.models
import uk.gov.hmrc.softdrinksindustrylevy.models.ActivityType.{Copackee, CopackerAll, Imported, ProducedOwnBrand}
import uk.gov.hmrc.softdrinksindustrylevy.models._
import uk.gov.hmrc.softdrinksindustrylevy.models.gen.{arbActivity, arbAddress, arbContact, arbSubRequest}

class DesConnectorSpecPropertyBased extends FunSuite with PropertyChecks with Matchers {

  import json.internal._

  test("∀ Activity: parse(toJson(x)) = x") {
    forAll { r: Activity =>
      Json.toJson(r).as[Activity] should be (r)
    }
  }

  test("∀ UkAddress: parse(toJson(x)) = x") {
    forAll { r: Address =>
      Json.toJson(r).as[Address] should be (r)
    }
  }

  test("∀ Contact: parse(toJson(x)) = x") {
    forAll { r: Contact =>
      Json.toJson(r).as[Contact] should be (r)
    }
  }

  test("∀ Subscription: parse(toJson(x)) = x") {
    forAll { r: Subscription =>
      Json.toJson(r).as[Subscription] should be (r)
    }
  }

}

class DesConnectorSpecBehavioural extends WiremockSpec with MockitoSugar {
  import play.api.test.Helpers.SERVICE_UNAVAILABLE

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.Future

  implicit val hc: HeaderCarrier = new HeaderCarrier

  object TestDesConnector extends DesConnector(httpClient, environment.mode, configuration, testPersistence, auditConnector) {
    override val desURL: String = mockServerUrl
  }

  "DesConnector" should {
    "return : None when DES returns 503 for an unknown UTR" in {

      stubFor(get(urlEqualTo("/soft-drinks/subscription/details/utr/11111111119"))
        .willReturn(aResponse().withStatus(SERVICE_UNAVAILABLE)))

      val response: Future[Option[Subscription]] = TestDesConnector.retrieveSubscriptionDetails("utr", "11111111119")
      response.map { x => x mustBe None }
    }

    "return : None financial data when nothing is returned" in {

      stubFor(get(urlEqualTo("/enterprise/financial-data/ZSDL/"))
        .willReturn(aResponse().withStatus(SERVICE_UNAVAILABLE)))

      val response: Future[Option[des.FinancialTransactionResponse]] = TestDesConnector.retrieveFinancialData("utr", None)
      response.map { x => x mustBe None }
    }

    "create subscription" in {

      stubFor(post(urlEqualTo("/soft-drinks/subscription/utr/11111111119"))
        .willReturn(aResponse().withStatus(SERVICE_UNAVAILABLE)))

      val response = the [Exception] thrownBy(TestDesConnector.createSubscription(sub, "utr", "11111111119").futureValue)
    response.getMessage must startWith("The future returned an exception of type: uk.gov.hmrc.http.Upstream5xxResponse")
    }

  }

  val sub = Subscription(
    "1234567890",
    Some("1234"),
    "org name",
    None,
    UkAddress(List("line1"), "AA11AA"),
    activity,
    LocalDate.now(),
    List(Site(UkAddress(List("line1"), "AA11AA"), None, None, None)),
    List(Site(UkAddress(List("line1"), "AA11AA"), None, None, None)),
    Contact(None, None, "0843858438", "test@test.com"),
    None,
    None
  )

  def internalActivity(produced: LitreBands = zero,
                       copackedAll: LitreBands = zero,
                       imported: LitreBands = zero,
                       copackedByOthers: LitreBands = zero) = {
    InternalActivity(
      Map(
        ProducedOwnBrand -> produced,
        CopackerAll -> copackedAll,
        Imported -> imported,
        Copackee -> copackedByOthers
      ), false
    )
  }

  lazy val zero: LitreBands = (0, 0)

  lazy val activity = internalActivity(
    produced = (1, 2),
    copackedAll = (3, 4),
    imported = (5, 6)
  )
}
