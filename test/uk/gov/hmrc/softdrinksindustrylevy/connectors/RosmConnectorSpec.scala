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

import org.scalatest.matchers.should.Matchers.shouldBe
import play.api.http.Status
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.softdrinksindustrylevy.models.{RosmRegisterRequest, RosmRegisterResponse, RosmResponseAddress, RosmResponseContactDetails}
import uk.gov.hmrc.softdrinksindustrylevy.util.WireMockMethods

import scala.concurrent.ExecutionContext

class RosmConnectorSpec extends HttpClientV2Helper with WireMockMethods {

  val connector: RosmConnector = app.injector.instanceOf[RosmConnector]

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

    when(POST, "/registration/organisation/utr/1234567890")
      .thenReturn(Status.INTERNAL_SERVER_ERROR)

    intercept[UpstreamErrorResponse] {
      await {
        connector.retrieveROSMDetails("1234567890", req)
      }
    }
  }

  /*"should get an upstream5xx response if des is returning 429" in {
    when(mockHttpClient.POST[RosmRegisterRequest, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
      .thenThrow(new RuntimeException())

    val ex = the[Exception] thrownBy (connector.retrieveROSMDetails("1234567890", req))
    ex.getMessage must startWith("The future returned an exception of type: uk.gov.hmrc.http.Upstream5xxResponse")
  }*/

  "should get a response back if des available" in {
    when(POST, "/registration/organisation/utr/1234567890")
      .thenReturn(Status.OK, Json.toJson(res).toString())

    await(connector.retrieveROSMDetails("1234567890", req)) shouldBe Some(res)
  }
}
