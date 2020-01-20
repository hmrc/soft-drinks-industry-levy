/*
 * Copyright 2020 HM Revenue & Customs
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

import java.time.Instant
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, stubFor, urlEqualTo, urlPathEqualTo}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.softdrinksindustrylevy.models.connectors._
import scala.util.{Failure, Success, Try}

class ContactFrontendConnectorSpec extends WiremockSpec {

  object TestContactConnector extends ContactFrontendConnector(httpClient, environment.mode, configuration, runMode) {
    override lazy val contactFrontendUrl: String = mockServerUrl
  }

  implicit val hc: HeaderCarrier = HeaderCarrier()

  "attempted contact form should fail if contact service is not available" in {
    stubFor(
      post(urlPathEqualTo("/contact/contact-hmrc/form?resubmitUrl=/"))
        .willReturn(aResponse().withStatus(500)))

    Try(TestContactConnector.raiseTicket(sub, "safeid1", Instant.now()).futureValue) match {
      case Success(_) => fail
      case Failure(_) =>
    }
  }

  "attempted contact form should succeed if contact service is available" in {
    stubFor(
      post(urlEqualTo("/contact/contact-hmrc/form?resubmitUrl=/"))
        .willReturn(aResponse().withStatus(200).withBody("")))
    Try(TestContactConnector.raiseTicket(sub, "test1", Instant.now()).futureValue) match {
      case Success(_) =>
      case Failure(_) => fail
    }
  }

}
