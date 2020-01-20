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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, stubFor, urlPathEqualTo}
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, Upstream4xxResponse, Upstream5xxResponse}
import uk.gov.hmrc.softdrinksindustrylevy.models.{RosmRegisterRequest, RosmRegisterResponse, RosmResponseAddress, RosmResponseContactDetails}

import scala.concurrent.Future

class RosmConnectorSpec extends WiremockSpec {

  object TestConnector extends RosmConnector(httpClient, environment.mode, servicesConfig) {
    override val desURL: String = mockServerUrl
  }

  val req = RosmRegisterRequest("CT", false, false)
  val res = RosmRegisterResponse(
    "safe1",
    None,
    true,
    false,
    true,
    None,
    None,
    RosmResponseAddress("line1", None, None, None, "EN", "AA11AA"),
    RosmResponseContactDetails(None, None, None, None)
  )
  implicit val hc: HeaderCarrier = HeaderCarrier()

  "should get no response back if des is not available" in {
    stubFor(
      post(urlPathEqualTo("/registration/organisation/utr/1234567890"))
        .willReturn(aResponse()
          .withStatus(500)))

    val response: Future[Option[RosmRegisterResponse]] = TestConnector.retrieveROSMDetails("1234567890", req)
    response.map { x =>
      x mustBe None
    }
  }

  "should get an upstream5xx response if des is returning 429" in {
    stubFor(
      post(urlPathEqualTo("/registration/organisation/utr/1234567890"))
        .willReturn(aResponse().withStatus(429)))

    val ex = the[Exception] thrownBy (TestConnector.retrieveROSMDetails("1234567890", req).futureValue)
    ex.getMessage must startWith("The future returned an exception of type: uk.gov.hmrc.http.Upstream5xxResponse")
  }

  "should get a response back if des available" in {
    stubFor(
      post(urlPathEqualTo("/registration/organisation/utr/1234567890"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(Json.toJson(req).toString())))

    val response: Future[Option[RosmRegisterResponse]] = TestConnector.retrieveROSMDetails("1234567890", req)
    response.map { x =>
      x mustBe Some(res)
    }
  }
}
