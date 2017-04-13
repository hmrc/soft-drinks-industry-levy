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
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.softdrinksindustrylevy.connectors.DesConnector
import uk.gov.hmrc.softdrinksindustrylevy.models.DesSubmissionResult
import uk.gov.hmrc.softdrinksindustrylevy.modelsFormat._
import uk.gov.hmrc.softdrinksindustrylevy.services.DesSubmissionService

import scala.concurrent.Future

class HelloWorldControllerSpec extends PlaySpec with MockitoSugar with GuiceOneAppPerSuite with BeforeAndAfterEach {
  val mockDesSubmissionService: DesSubmissionService = mock[DesSubmissionService]
  val mockDesConnector: DesConnector = mock[DesConnector]
  val mockHelloWorldController = new HelloWorldController(mockDesSubmissionService, mockDesConnector)

  override def beforeEach() {
    reset(mockDesSubmissionService, mockDesConnector)
  }

  "HelloWorldController" should {
    "return Status: OK Body: DesSubmissionResult(true) for successful valid submitDesRequest" in {
      implicit val hc = new HeaderCarrier

      when(mockDesConnector.submitDesRequest(any())(any())).thenReturn(Future.successful(DesSubmissionResult(true)))
      val response = mockHelloWorldController.hello()(FakeRequest("GET", "/hello-world"))

      status(response) mustBe OK
      verify(mockDesConnector, times(1)).submitDesRequest(any())(any())
      Json.fromJson[DesSubmissionResult](contentAsJson(response)).getOrElse(DesSubmissionResult(false)) mustBe DesSubmissionResult(true)
    }

    "return Status: OK Body: DesSubmissionResult(false) for successful invalid submitDesRequest" in {
      implicit val hc = new HeaderCarrier

      when(mockDesConnector.submitDesRequest(any())(any())).thenReturn(Future.successful(DesSubmissionResult(false)))
      val response = mockHelloWorldController.hello()(FakeRequest("GET", "/hello-world"))

      status(response) mustBe OK
      verify(mockDesConnector, times(1)).submitDesRequest(any())(any())
      Json.fromJson[DesSubmissionResult](contentAsJson(response)).getOrElse(DesSubmissionResult(true)) mustBe DesSubmissionResult(false)
    }
  }
}
