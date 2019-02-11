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

import java.time.{Instant, LocalDate}

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, stubFor, urlPathEqualTo, urlEqualTo}
import org.mockito.Mockito
import play.api.test.Helpers.SERVICE_UNAVAILABLE
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.softdrinksindustrylevy.models.ActivityType.{Copackee, CopackerAll, Imported, ProducedOwnBrand}
import uk.gov.hmrc.softdrinksindustrylevy.models.{Contact, InternalActivity, LitreBands, Site, Subscription, UkAddress}

import scala.util.{Failure, Success, Try}

class ContactFrontendConnectorSpec extends WiremockSpec {

  object TestContactConnector extends ContactFrontendConnector(httpClient, environment.mode, configuration) {
    override lazy val contactFrontendUrl: String = mockServerUrl
  }

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

  val activity = internalActivity(
    produced = (1, 2),
    copackedAll = (3, 4),
    imported = (5, 6)
  )
  lazy val zero: LitreBands = (0, 0)

  implicit val hc: HeaderCarrier = HeaderCarrier()
  val subscription = Subscription(
    "1234567890",
    Some("asfdsfdsfd"),
    "org name",
    None,
    UkAddress(List("line1"), "AA11AA"),
    activity,
    LocalDate.now,
    List(Site(UkAddress(List("line1"), "AA11AA"), None, None, None)),
    List(Site(UkAddress(List("line1"), "AA11AA"), None, None, None)),
    Contact(None, None, "0843858438", "test@test.com"),
    None,
    None
  )

  "attempted contact form should fail if contact service is not available" in {
    stubFor(post(urlPathEqualTo("/contact/contact-hmrc/form?resubmitUrl=/"))
      .willReturn(aResponse().withStatus(500)))

    Try(TestContactConnector.raiseTicket(subscription, "safeid1", Instant.now()).futureValue) match {
      case Success(_) => fail
      case Failure(_) =>
    }
  }

  "attempted contact form should succeed if contact service is available" in {
    stubFor(post(urlEqualTo("/contact/contact-hmrc/form?resubmitUrl=/"))
      .willReturn(aResponse().withStatus(200).withBody("")))
    Try(TestContactConnector.raiseTicket(subscription, "test1", Instant.now()).futureValue) match {
      case Success(_) =>
      case Failure(_) => fail
    }
  }
}
