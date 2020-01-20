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

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier

class TaxEnrolmentConnectorSpec extends WiremockSpec {

  object TestConnector extends TaxEnrolmentConnector(httpClient, environment.mode, servicesConfig) {
    override val callbackUrl: String = mockServerUrl
    override lazy val taxEnrolmentsUrl: String = mockServerUrl
    override val serviceName: String = "service-name"
  }

  val req = TaxEnrolmentsSubscription(None, "etmp1", "active", None)
  implicit val hc: HeaderCarrier = HeaderCarrier()

  "should get successful response back" in {
    stubFor(
      get(urlPathEqualTo("/tax-enrolments/subscriptions/1234567890"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(Json.toJson(req).toString())))

    val response: TaxEnrolmentsSubscription = TestConnector.getSubscription("1234567890").futureValue
    response mustBe req
  }

  "should subscribe successfully" in {
    stubFor(
      put(urlPathEqualTo("/tax-enrolments/subscriptions/1234/subscriber"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(Json.toJson(req).toString())))
    val res = TestConnector.subscribe("safe1", "1234").futureValue
    res.status mustBe 200
  }

  "should handle errors for create subscribtion" in {
    stubFor(
      put(urlPathEqualTo("/tax-enrolments/subscriptions/1234/subscriber"))
        .willReturn(aResponse()
          .withStatus(400)))
    val res = TestConnector.subscribe("safe1", "1234").futureValue
    res.status mustBe 400
  }
}
