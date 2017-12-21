/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.softdrinksindustrylevy.controllers

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.softdrinksindustrylevy.connectors.{RosmConnector, TaxEnrolmentConnector}
import uk.gov.hmrc.http._

import scala.concurrent.Future

class RosmControllerSpec extends PlaySpec with MockitoSugar with GuiceOneAppPerSuite with BeforeAndAfterEach
with ScalaFutures {
  val mockTaxEnrolmentConnector = mock[TaxEnrolmentConnector]
  val mockRosmConnector: RosmConnector = mock[RosmConnector]
  val mockRosmController = new RosmController(mockRosmConnector, mockTaxEnrolmentConnector)

  implicit val hc = new HeaderCarrier

  override def beforeEach() {
    reset(mockRosmConnector)
  }

  "RosmController" should {
    "return Status: OK Body: RosmRegisterResponse for successful valid Rosm lookup" in {
      when(mockRosmConnector.retrieveROSMDetails(any(), any())(any(),any()))
        .thenReturn(Future.successful(Some(validRosmRegisterResponse)))

      val response = mockRosmController.lookupRegistration("1111111111")(FakeRequest("GET", "/register/organisation/utr/:utr"))

      status(response) mustBe OK
      verify(mockRosmConnector, times(1)).retrieveROSMDetails(any(), any())(any(), any())
      contentAsJson(response) mustBe Json.toJson(validRosmRegisterResponse)
    }

    "return Status: NOT_FOUND for unknown utr" in {
      when(mockRosmConnector.retrieveROSMDetails(any(), any())(any(),any()))
        .thenReturn(Future.failed(new NotFoundException("foo")))

      val result = mockRosmController.lookupRegistration("2222222222")(FakeRequest("GET", "/register/organisation/utr/:utr"))
      whenReady(result.failed){e => e mustBe a[NotFoundException]}
    }

    "return Status: NOT_FOUND for response with missing organisation " in {
      when(mockRosmConnector.retrieveROSMDetails(any(), any())(any(),any()))
        .thenReturn(Future.successful(Some(validRosmRegisterResponse.copy(organisation = None))))

      val response = mockRosmController.lookupRegistration("1111111111")(FakeRequest("GET", "/register/organisation/utr/:utr"))

      status(response) mustBe NOT_FOUND
      verify(mockRosmConnector, times(1)).retrieveROSMDetails(any(), any())(any(), any())
    }
  }
}
