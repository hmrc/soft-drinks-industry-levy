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
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.softdrinksindustrylevy.models.{RosmRegisterRequest, RosmRegisterResponse, RosmResponseAddress, RosmResponseContactDetails}

import scala.concurrent.{ExecutionContext, Future}

class RosmConnectorSpec extends HttpClientV2Helper with ScalaCheckPropertyChecks {

  val mode = mock[Mode]

  val connector = new RosmConnector(mockHttpClient, mode, mockServicesConfig)

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

  implicit lazy val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  "should get no response back if des is not available" in {

    when(requestBuilderExecute[HttpResponse])
      .thenReturn(Future.successful(HttpResponse(500, "500")))

    val response: Future[Option[RosmRegisterResponse]] = connector.retrieveROSMDetails("1234567890", req)
    response.map { x =>
      x mustBe None
    }
  }

  /*"should get an upstream5xx response if des is returning 429" in {
    when(mockHttpClient.POST[RosmRegisterRequest, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
      .thenThrow(new RuntimeException())

    val ex = the[Exception] thrownBy (connector.retrieveROSMDetails("1234567890", req))
    ex.getMessage must startWith("The future returned an exception of type: uk.gov.hmrc.http.Upstream5xxResponse")
  }*/

  "should get a response back if des available" in {
    when(requestBuilderExecute[HttpResponse])
      .thenReturn(Future.successful(HttpResponse(200, Json.toJson(req).toString())))

    val response: Future[Option[RosmRegisterResponse]] = connector.retrieveROSMDetails("1234567890", req)
    response.map { x =>
      x mustBe Some(res)
    }
  }
}
