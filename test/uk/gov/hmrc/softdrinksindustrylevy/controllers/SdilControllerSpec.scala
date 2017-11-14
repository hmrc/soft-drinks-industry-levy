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
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}
import uk.gov.hmrc.softdrinksindustrylevy.connectors.DesConnector
import uk.gov.hmrc.softdrinksindustrylevy.models.CreateSubscriptionResponse
import uk.gov.hmrc.softdrinksindustrylevy.services.DesSubmissionService

import scala.concurrent.Future

class SdilControllerSpec extends PlaySpec with MockitoSugar with GuiceOneAppPerSuite with BeforeAndAfterEach {
  val mockDesSubmissionService: DesSubmissionService = mock[DesSubmissionService]
  val mockDesConnector: DesConnector = mock[DesConnector]
  val mockSdilController = new SdilController(mockDesSubmissionService, mockDesConnector)

  implicit val hc = new HeaderCarrier

  override def beforeEach() {
    reset(mockDesSubmissionService, mockDesConnector)
  }

  "SdilController" should {
    "return Status: OK Body: CreateSubscriptionResponse for successful valid submitDesRequest" in {

      when(mockDesConnector.createSubscription(any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(validSubscriptionResponse))

      val response = mockSdilController.submitRegistration("UTR", "00002222")(FakeRequest("POST", "/create-subscription/:idType/:idNumber")
        .withBody(validCreateSubscriptionRequest))

      status(response) mustBe OK
      verify(mockDesConnector, times(1)).createSubscription(any(), any(), any())(any(), any())
      contentAsJson(response) mustBe Json.toJson(validSubscriptionResponse)
    }

    "return Status: BAD_REQUEST and redirect to error page for any other exception" in {

      val result = mockSdilController.submitRegistration("UTR", "00002222")(FakeRequest("POST", "/create-subscription/:idType/:idNumber")
        .withBody(invalidCreateSubscriptionRequest))

      status(result) mustBe BAD_REQUEST
    }
  }
}
