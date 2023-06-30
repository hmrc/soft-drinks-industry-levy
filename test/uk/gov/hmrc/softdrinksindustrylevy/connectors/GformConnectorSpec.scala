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

import java.util.Base64
import com.github.tomakehurst.wiremock.client.WireMock._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.Mode
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.softdrinksindustrylevy.util.FakeApplicationSpec

import scala.concurrent.{ExecutionContext, Future}

class GformConnectorSpec
    extends FakeApplicationSpec with MockitoSugar with BeforeAndAfterEach with ScalaCheckPropertyChecks
    with FutureAwaits with DefaultAwaitTimeout {

  val mockServicesConfig: ServicesConfig = mock[ServicesConfig]

  val mode = mock[Mode]

  val mockHttpClient = mock[HttpClient]

  val connector = new GformConnector(mockHttpClient, mode, mockServicesConfig)

  implicit lazy val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  "Submitting a html to gform" should {
    "base64 encode the html" in {
      val rawHtml = "<p>totally a variation</p>"
      val encodedHtml = new String(Base64.getEncoder.encode(rawHtml.getBytes))

      when(mockHttpClient.POST[DmsHtmlSubmission, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(204, "204")))

      await(connector.submitToDms(rawHtml, "totally an sdil number"))
    }

    "send the correct metadata to gform" in {

      val sdilNumber = "XZSDIL0009999"
      val expectedMetadataJson =
        """{"dmsFormId":"SDIL-VAR-1","customerId":"XZSDIL0009999","classificationType":"BT-NRU-SDIL","businessArea":"BT"}"""

      when(mockHttpClient.POST[DmsHtmlSubmission, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(204, "204")))

      await(connector.submitToDms("", sdilNumber))
    }
  }

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()
}
