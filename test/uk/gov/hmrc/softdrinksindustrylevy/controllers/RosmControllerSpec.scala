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

package uk.gov.hmrc.softdrinksindustrylevy.controllers

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Configuration, Mode}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.softdrinksindustrylevy.connectors.{RosmConnector, TaxEnrolmentConnector}
import uk.gov.hmrc.softdrinksindustrylevy.util.FakeApplicationSpec

import java.time.Clock
import scala.concurrent.Future

class RosmControllerSpec extends FakeApplicationSpec with MockitoSugar with BeforeAndAfterEach with ScalaFutures {

  val mockTaxEnrolmentConnector = mock[TaxEnrolmentConnector]
  val mockRosmConnector: RosmConnector = mock[RosmConnector]
  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockMode: Mode = mock[Mode]
  val mockConfiguration: Configuration = mock[Configuration]
  implicit def mockClock: Clock = Clock.systemDefaultZone()
  implicit val hc: HeaderCarrier = new HeaderCarrier
  val cc = app.injector.instanceOf[ControllerComponents]
  val serviceConfig = mock[ServicesConfig]
  val testRosmController =
    new RosmController(mockAuthConnector, mockRosmConnector, mockMode, cc, serviceConfig)

  override def beforeEach(): Unit =
    reset(mockRosmConnector)

  when(mockAuthConnector.authorise[Unit](any(), any())(any(), any())).thenReturn(Future.successful(()))
  when(serviceConfig.getString(any())).thenReturn("someBaseUrl")

  "RosmController" should {
    "return Status: OK Body: RosmRegisterResponse for successful valid Rosm lookup" in {
      when(mockRosmConnector.retrieveROSMDetails(any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(validRosmRegisterResponse)))

      val response = testRosmController.lookupRegistration("1111111111")(FakeRequest())

      status(response) mustBe OK
      verify(mockRosmConnector, times(1)).retrieveROSMDetails(any(), any())(any(), any())
      contentAsJson(response) mustBe Json.toJson(validRosmRegisterResponse)
    }

    "return Status: NOT_FOUND for response with missing organisation " in {
      when(mockRosmConnector.retrieveROSMDetails(any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(validRosmRegisterResponse.copy(organisation = None))))

      val response = testRosmController.lookupRegistration("1111111111")(FakeRequest())

      status(response) mustBe NOT_FOUND
      verify(mockRosmConnector, times(1)).retrieveROSMDetails(any(), any())(any(), any())
    }

    "return Not Found when there is no ROSM record" in {
      when(mockRosmConnector.retrieveROSMDetails(any(), any())(any(), any())).thenReturn(Future.successful(None))

      val res = testRosmController.lookupRegistration("3334445556")(FakeRequest())
      status(res) mustBe NOT_FOUND
    }
  }
}
