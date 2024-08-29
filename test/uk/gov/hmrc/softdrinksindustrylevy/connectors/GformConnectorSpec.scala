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

import org.mockito.Mockito.when
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.Mode
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

class GformConnectorSpec
    extends HttpClientV2Helper with ScalaCheckPropertyChecks with FutureAwaits with DefaultAwaitTimeout {

  val mode = mock[Mode]

  val connector = new GformConnector(mockHttpClient, mode, mockServicesConfig)

  implicit lazy val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  "Submitting a html to gform" should {
    "base64 encode the html" in {
      val rawHtml = "<p>totally a variation</p>"

      when(requestBuilderExecute[HttpResponse])
        .thenReturn(Future.successful(HttpResponse(204, "204")))

      await(connector.submitToDms(rawHtml, "totally an sdil number"))
    }

    "send the correct metadata to gform" in {

      val sdilNumber = "XZSDIL0009999"

      when(requestBuilderExecute[HttpResponse])
        .thenReturn(Future.successful(HttpResponse(204, "204")))

      await(connector.submitToDms("", sdilNumber))
    }
  }

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()
}
