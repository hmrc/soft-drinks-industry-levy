/*
 * Copyright 2021 HM Revenue & Customs
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
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{ControllerComponents, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import sdil.models.{ReturnPeriod, ReturnVariationData, SdilReturn}
import uk.gov.hmrc.softdrinksindustrylevy.connectors.GformConnector
import uk.gov.hmrc.softdrinksindustrylevy.models.{ReturnsVariationRequest, UkAddress, VariationsContact, VariationsRequest}
import uk.gov.hmrc.softdrinksindustrylevy.services.{ReturnsAdjustmentSubmissionService, ReturnsVariationSubmissionService, VariationSubmissionService}
import uk.gov.hmrc.softdrinksindustrylevy.util.FakeApplicationSpec
import views.html.{returns_variation_pdf, variations_pdf}

import scala.concurrent.Future

class VariationsControllerSpec extends FakeApplicationSpec with MockitoSugar with BeforeAndAfterEach with ScalaFutures {

  implicit val messages: Messages = messagesApi.preferred(request)
  implicit lazy val request: Request[_] = FakeRequest()

  val mockGformConnector = mock[GformConnector]
  val mockVariationSubmissionService = mock[VariationSubmissionService]
  val mockReturnsVariationSubmissionService = mock[ReturnsVariationSubmissionService]
  val mockReturnsAdjustmentSubmissionService = mock[ReturnsAdjustmentSubmissionService]
  val cc = mock[ControllerComponents]

  val controller: VariationsController = new VariationsController(
    messagesApi,
    mockGformConnector,
    mockVariationSubmissionService,
    mockReturnsVariationSubmissionService,
    mockReturnsAdjustmentSubmissionService,
    cc)

  override def beforeEach() {
    reset(mockGformConnector)
    reset(mockVariationSubmissionService)
    reset(mockReturnsVariationSubmissionService)
    reset(mockReturnsAdjustmentSubmissionService)
  }

  val tradingName = "Generic Soft Drinks Company Inc Ltd LLC Intl GB UK"
  val address = UkAddress(List("My House", "My Lane"), "AA111A")
  val contactDetails = VariationsContact(
    addressLine1 = Some("line 1"),
    addressLine2 = Some("line 2"),
    postCode = Some("AA11 1AA"),
    telephoneNumber = Some("999"),
    emailAddress = Some("aa@bb.cc"))
  val sdilNumber = "XCSDIL000000000"

  "generateVariations" should {
    val variationRequest = VariationsRequest(
      displayOrgName = tradingName,
      ppobAddress = address,
      correspondenceContact = Some(contactDetails))
    val page = variations_pdf(variationRequest, sdilNumber).toString()

    "204 successfully generate Variations" in {
      implicit val requestInput = FakeRequest().withBody(Json.toJson(variationRequest))

      when(mockGformConnector.submitToDms(any[String](), any[String]())(any(), any()))
        .thenReturn(Future.successful(()))

      when(mockVariationSubmissionService.save(any(), any[String]()))
        .thenReturn(Future.successful(()))

      println(s"MOHAN MOHAN controller = $controller")

      val result = controller.generateVariations(sdilNumber)(requestInput)

      status(result) mustBe 204
    }

    "400 bad request when requestBody json does not match the VariationRequest model" in {
      val requestInput = FakeRequest().withBody(Json.obj("wrongJson" -> "not the same model"))

      val result = controller.generateVariations(sdilNumber)(requestInput)

      status(result) mustBe 400
    }

    "throwException when gform connector throws exception" in {
      val requestInput = FakeRequest().withBody(Json.toJson(variationRequest))

      when(mockGformConnector.submitToDms(any(), any())(any(), any()))
        .thenThrow(new RuntimeException)

      an[RuntimeException] shouldBe thrownBy(controller.generateVariations(sdilNumber)(requestInput))
    }

    "throwException when repository service throws exception" in {
      val requestInput = FakeRequest().withBody(Json.toJson(variationRequest))

      when(mockGformConnector.submitToDms(any(), any())(any(), any()))
        .thenReturn(Future.successful(()))
      when(mockVariationSubmissionService.save(any(), any()))
        .thenThrow(new RuntimeException)

      val result = controller.generateVariations(sdilNumber)(requestInput)

      whenReady(result.failed)(e => {
        e mustBe a[RuntimeException]
      })
    }
  }

  "returnsVariation" should {
    val returnsVariationRequest = ReturnsVariationRequest(
      tradingName,
      address,
      (false, (0, 0)),
      (false, (0, 0)),
      Nil,
      Nil,
      "",
      "email",
      BigDecimal("1.1"))
    val page = returns_variation_pdf(returnsVariationRequest, sdilNumber).toString()

    "204 when successfully" in {
      val requestInput = FakeRequest().withBody(Json.toJson(returnsVariationRequest))

      when(mockGformConnector.submitToDms(any(), any())(any(), any()))
        .thenReturn(Future.successful(()))
      when(mockReturnsVariationSubmissionService.save(any(), any()))
        .thenReturn(Future.successful(()))

      val result = controller.returnsVariation(sdilNumber)(requestInput)

      status(result) mustBe 204
    }

    "400 bad request when requestBody json does not match the ReturnsVariationRequest model" in {
      val requestInput = FakeRequest().withBody(Json.obj("wrongJson" -> "not the same model"))

      val result = controller.returnsVariation(sdilNumber)(requestInput)

      status(result) mustBe 400
    }

    "throwException when gform connector throws exception" in {
      val requestInput = FakeRequest().withBody(Json.toJson(returnsVariationRequest))

      when(mockGformConnector.submitToDms(any(), any())(any(), any()))
        .thenThrow(new RuntimeException)

      an[RuntimeException] shouldBe thrownBy(controller.returnsVariation(sdilNumber)(requestInput))
    }

    "throwException when repository service throws exception" in {
      val requestInput = FakeRequest().withBody(Json.toJson(returnsVariationRequest))

      when(mockGformConnector.submitToDms(any(), any())(any(), any()))
        .thenReturn(Future.successful(()))
      when(mockReturnsVariationSubmissionService.save(any(), any()))
        .thenThrow(new RuntimeException)

      val result = controller.returnsVariation(sdilNumber)(requestInput)

      whenReady(result.failed)(e => {
        e mustBe a[RuntimeException]
      })
    }
  }

  "varyReturn" should {
    val testOriginal = SdilReturn(submittedOn = None)
    val testRevised = SdilReturn((3, 3), (3, 3), Nil, (3, 3), (3, 3), (3, 3), (3, 3), None)
    val returnVariationData =
      ReturnVariationData(testOriginal, testRevised, ReturnPeriod(2018, 1), "testOrg", address, "", None)

    val page = views.html.return_variation_pdf(returnVariationData, sdilNumber).toString()
    "204 when successfully" in {
      val requestInput = FakeRequest().withBody(Json.toJson(returnVariationData))

      when(mockGformConnector.submitToDms(any(), any())(any(), any()))
        .thenReturn(Future.successful(()))
      when(mockReturnsAdjustmentSubmissionService.save(any(), any()))
        .thenReturn(Future.successful(()))

      val result = controller.varyReturn(sdilNumber)(requestInput)

      status(result) mustBe 204
    }

    "400 bad request when requestBody json does not match the ReturnVariationData model" in {
      val requestInput = FakeRequest().withBody(Json.obj("wrongJson" -> "not the same model"))

      val result = controller.varyReturn(sdilNumber)(requestInput)

      status(result) mustBe 400
    }

    "throwException when gform connector throws exception" in {
      val requestInput = FakeRequest().withBody(Json.toJson(returnVariationData))

      when(mockGformConnector.submitToDms(any(), any())(any(), any()))
        .thenThrow(new RuntimeException)

      an[RuntimeException] shouldBe thrownBy(controller.varyReturn(sdilNumber)(requestInput))
    }

    "throwException when repository service throws exception" in {
      val requestInput = FakeRequest().withBody(Json.toJson(returnVariationData))

      when(mockGformConnector.submitToDms(any(), any())(any(), any()))
        .thenReturn(Future.successful(()))
      when(mockReturnsAdjustmentSubmissionService.save(any(), any()))
        .thenThrow(new RuntimeException)

      val result = controller.varyReturn(sdilNumber)(requestInput)

      whenReady(result.failed)(e => {
        e mustBe a[RuntimeException]
      })
    }
  }
}
