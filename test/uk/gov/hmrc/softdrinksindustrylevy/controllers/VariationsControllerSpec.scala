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

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, argThat, eq => meq}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{ControllerComponents, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import sdil.models.{ReturnPeriod, ReturnVariationData, SdilReturn}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.softdrinksindustrylevy.connectors.GformConnector
import uk.gov.hmrc.softdrinksindustrylevy.models.{ReturnsVariationRequest, UkAddress, VariationsContact, VariationsRequest, VariationsSubmissionEvent}
import uk.gov.hmrc.softdrinksindustrylevy.services.{ReturnsAdjustmentSubmissionService, ReturnsVariationSubmissionService, VariationSubmissionService}
import uk.gov.hmrc.softdrinksindustrylevy.util.FakeApplicationSpec

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.CollectionHasAsScala

class VariationsControllerSpec extends FakeApplicationSpec with MockitoSugar with BeforeAndAfterEach with ScalaFutures {

  implicit val messages: Messages = messagesApi.preferred(request)
  implicit lazy val request: Request[?] = FakeRequest()
  implicit val hc: HeaderCarrier = new HeaderCarrier

  implicit lazy val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  val mockGformConnector = mock[GformConnector]
  val mockVariationSubmissionService = mock[VariationSubmissionService]
  val mockReturnsVariationSubmissionService = mock[ReturnsVariationSubmissionService]
  val mockReturnsAdjustmentSubmissionService = mock[ReturnsAdjustmentSubmissionService]
  val mockAuditing: AuditConnector = mock[AuditConnector]
  val cc = app.injector.instanceOf[ControllerComponents]

  val controller: VariationsController = new VariationsController(
    messagesApi,
    mockGformConnector,
    mockAuditing,
    mockVariationSubmissionService,
    mockReturnsVariationSubmissionService,
    mockReturnsAdjustmentSubmissionService,
    cc
  )

  override def beforeEach(): Unit = {
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
    emailAddress = Some("aa@bb.cc")
  )
  val sdilNumber = "XCSDIL000000000"

  "generateVariations" should {

    val variationRequest = VariationsRequest(
      displayOrgName = tradingName,
      ppobAddress = address,
      correspondenceContact = Some(contactDetails)
    )

    "204 successfully generate Variations (and audit SUCCESS)" in {
      val requestInput = FakeRequest().withBody(Json.toJson(variationRequest))

      when(mockGformConnector.submitToDms(any(), meq(sdilNumber))(using any(), any()))
        .thenReturn(Future.unit)
      when(mockVariationSubmissionService.save(any(), meq(sdilNumber)))
        .thenReturn(Future.unit)
      when(mockAuditing.sendExtendedEvent(any())(using any(), any()))
        .thenReturn(Future.successful(AuditResult.Success))

      val result = controller.generateVariations(sdilNumber)(requestInput)
      status(result) mustBe NO_CONTENT

      val captor = ArgumentCaptor.forClass(classOf[VariationsSubmissionEvent])
      verify(mockAuditing).sendExtendedEvent(captor.capture())(using any(), any())
      val detail = captor.getValue.detail
      (detail \ "outcome").as[String] mustBe "SUCCESS"
      (detail \ "sdilNumber").as[String] mustBe sdilNumber
      (detail \ "formTemplateId").as[String] mustBe "SDIL-VAR-1"
    }

    "400 bad request when requestBody json does not match the VariationRequest model (no side effects)" in {
      val requestInput = FakeRequest().withBody(Json.obj("wrongJson" -> "not the same model"))

      val result = controller.generateVariations(sdilNumber)(requestInput)
      status(result) mustBe BAD_REQUEST

      verify(mockGformConnector, never()).submitToDms(any(), any())(using any(), any())
      verify(mockVariationSubmissionService, never()).save(any(), any())
    }

    "throwException when gform connector throws exception (and audit ERROR)" in {
      val requestInput = FakeRequest().withBody(Json.toJson(variationRequest))
      val ex = new RuntimeException("dms error")

      when(mockGformConnector.submitToDms(any(), any())(using any(), any()))
        .thenReturn(Future.failed(ex))
      when(mockAuditing.sendExtendedEvent(any())(using any(), any()))
        .thenReturn(Future.successful(AuditResult.Success))

      val generatedVariation = controller.generateVariations(sdilNumber)(requestInput)
      whenReady(generatedVariation.failed)(_ mustBe ex)

      verify(mockAuditing, times(1)).sendExtendedEvent(
        argThat[VariationsSubmissionEvent](evt =>
          (evt.detail \ "outcome").as[String] == "ERROR" &&
            (evt.detail \ "error").as[String].contains("dms error")
        )
      )(using any(), any())
    }

    "throwException when repository service throws exception (and audit ERROR)" in {
      reset(mockAuditing)

      val requestInput = FakeRequest().withBody(Json.toJson(variationRequest))
      val ex = new RuntimeException("save error")

      when(mockGformConnector.submitToDms(any(), any())(using any(), any()))
        .thenReturn(Future.unit)
      when(mockVariationSubmissionService.save(any(), any()))
        .thenReturn(Future.failed(ex))
      when(mockAuditing.sendExtendedEvent(any())(using any(), any()))
        .thenReturn(Future.successful(AuditResult.Success))

      val generatedVariation = controller.generateVariations(sdilNumber)(requestInput)
      whenReady(generatedVariation.failed)(_ mustBe ex)

      val captor = ArgumentCaptor.forClass(classOf[VariationsSubmissionEvent])
      verify(mockAuditing, atLeastOnce()).sendExtendedEvent(captor.capture())(using any(), any())

      val events = captor.getAllValues.asScala.toList

      val errorEvents = events.filter(e => (e.detail \ "outcome").as[String] == "ERROR")
      errorEvents.size mustBe 1
      (errorEvents.head.detail \ "error").as[String] must include("save error")

      val successEvents = events.filter(e => (e.detail \ "outcome").as[String] == "SUCCESS")
      successEvents.size mustBe 0
    }

    "fail the request if SUCCESS-path auditing fails" in {
      val requestInput = FakeRequest().withBody(Json.toJson(variationRequest))

      when(mockGformConnector.submitToDms(any(), any())(using any(), any()))
        .thenReturn(Future.unit)
      when(mockVariationSubmissionService.save(any(), any()))
        .thenReturn(Future.unit)
      when(mockAuditing.sendExtendedEvent(any())(using any(), any()))
        .thenReturn(Future.failed(new RuntimeException("audit failed")))

      val generatedVariation = controller.generateVariations(sdilNumber)(requestInput)
      whenReady(generatedVariation.failed)(e => e.getMessage must include("audit failed"))
    }

    "surface audit error if upstream fails and ERROR-path auditing also fails" in {
      val requestInput = FakeRequest().withBody(Json.toJson(variationRequest))
      val orig = new RuntimeException("upstream boom")
      val auditEx = new RuntimeException("audit failed")

      when(mockGformConnector.submitToDms(any(), any())(using any(), any()))
        .thenReturn(Future.failed(orig))
      when(mockAuditing.sendExtendedEvent(any())(using any(), any()))
        .thenReturn(Future.failed(auditEx))

      val generatedVariation = controller.generateVariations(sdilNumber)(requestInput)
      whenReady(generatedVariation.failed)(_ mustBe auditEx)
    }

    "propagate request.uri into VariationsSubmissionEvent" in {
      when(mockGformConnector.submitToDms(any(), any())(using any(), any()))
        .thenReturn(Future.unit)
      when(mockVariationSubmissionService.save(any(), any()))
        .thenReturn(Future.unit)
      when(mockAuditing.sendExtendedEvent(any())(using any(), any()))
        .thenReturn(Future.successful(AuditResult.Success))

      val requestInput = FakeRequest("POST", "/variations?x=1").withBody(Json.toJson(variationRequest))
      await(controller.generateVariations(sdilNumber)(requestInput))

    }

    "include deviceId only when present in HeaderCarrier (unit of buildVariationsAudit)" in {
      implicit val hcWith = HeaderCarrier(deviceID = Some("abc"))
      val js1 = controller.buildVariationsAudit(variationRequest, sdilNumber, "SUCCESS")(using hcWith).as[JsObject]
      (js1 \ "deviceId").as[String] mustBe "abc"

      implicit val hcWithout = HeaderCarrier(deviceID = None)
      val js2 = controller.buildVariationsAudit(variationRequest, sdilNumber, "SUCCESS")(using hcWithout).as[JsObject]
      (js2 \ "deviceId").toOption mustBe None
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
      BigDecimal("1.1")
    )

    "204 when successfully" in {
      val requestInput = FakeRequest().withBody(Json.toJson(returnsVariationRequest))

      when(mockGformConnector.submitToDms(any(), any())(using any(), any()))
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

      when(mockGformConnector.submitToDms(any(), any())(using any(), any()))
        .thenThrow(new RuntimeException)

      an[RuntimeException] shouldBe thrownBy(controller.returnsVariation(sdilNumber)(requestInput))
    }

    "throwException when repository service throws exception" in {
      val requestInput = FakeRequest().withBody(Json.toJson(returnsVariationRequest))

      when(mockGformConnector.submitToDms(any(), any())(using any(), any()))
        .thenReturn(Future.successful(()))
      when(mockReturnsVariationSubmissionService.save(any(), any()))
        .thenThrow(new RuntimeException)

      val result = controller.returnsVariation(sdilNumber)(requestInput)

      whenReady(result.failed)(e => e mustBe a[RuntimeException])
    }
  }

  "varyReturn" should {
    val testOriginal = SdilReturn(submittedOn = None)
    val testRevised = SdilReturn((3, 3), (3, 3), Nil, (3, 3), (3, 3), (3, 3), (3, 3), None)
    val returnVariationData =
      ReturnVariationData(testOriginal, testRevised, ReturnPeriod(2018, 1), "testOrg", address, "", None)

    "204 when successfully" in {
      val requestInput = FakeRequest().withBody(Json.toJson(returnVariationData))

      when(mockGformConnector.submitToDms(any(), any())(using any(), any()))
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

      when(mockGformConnector.submitToDms(any(), any())(using any(), any()))
        .thenThrow(new RuntimeException)

      an[RuntimeException] shouldBe thrownBy(controller.varyReturn(sdilNumber)(requestInput))
    }

    "throwException when repository service throws exception" in {
      val requestInput = FakeRequest().withBody(Json.toJson(returnVariationData))

      when(mockGformConnector.submitToDms(any(), any())(using any(), any()))
        .thenReturn(Future.successful(()))
      when(mockReturnsAdjustmentSubmissionService.save(any(), any()))
        .thenThrow(new RuntimeException)

      val result = controller.varyReturn(sdilNumber)(requestInput)

      whenReady(result.failed)(e => e mustBe a[RuntimeException])
    }
  }
}
