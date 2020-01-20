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

import java.util.Base64
import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.http.HeaderCarrier
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}

class GformConnectorSpec extends WiremockSpec with FutureAwaits with DefaultAwaitTimeout {

  "Submitting a html to gform" should {
    "base64 encode the html" in {
      val rawHtml = "<p>totally a variation</p>"
      val encodedHtml = new String(Base64.getEncoder.encode(rawHtml.getBytes))

      stubFor(
        post("/gform/dms/submit")
          .willReturn(aResponse().withStatus(204))
      )

      await(testConnector.submitToDms(rawHtml, "totally an sdil number"))

      verify(
        postRequestedFor(urlEqualTo("/gform/dms/submit"))
          .withRequestBody(containing(s""""html":"$encodedHtml""""))
      )
    }

    "send the correct metadata to gform" in {
      stubFor(
        post("/gform/dms/submit")
          .willReturn(aResponse().withStatus(204))
      )

      val sdilNumber = "XZSDIL0009999"
      val expectedMetadataJson =
        """{"dmsFormId":"SDIL-VAR-1","customerId":"XZSDIL0009999","classificationType":"BT-NRU-SDIL","businessArea":"BT"}"""

      await(testConnector.submitToDms("", sdilNumber))

      verify(
        postRequestedFor(urlEqualTo("/gform/dms/submit"))
          .withRequestBody(containing(s""""metadata":$expectedMetadataJson"""))
      )
    }
  }

  lazy val testConnector = new GformConnector(httpClient, environment.mode, servicesConfig) {
    override val gformUrl = mockServerUrl
  }

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()
}
