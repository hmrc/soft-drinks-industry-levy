/*
 * Copyright 2026 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers.shouldBe
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthProviders, MissingBearerToken}
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.softdrinksindustrylevy.util.FakeApplicationSpec

import scala.concurrent.{ExecutionContext, Future}

class LevyCalculationControllerSpec extends FakeApplicationSpec with BeforeAndAfterEach {
  implicit lazy val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  val mockAuthConnector: AuthConnector = mock[AuthConnector]

  private val cc = Helpers.stubControllerComponents()
  private val controller = new LevyCalculationController(cc, mockAuthConnector)

  private def calculateLevyRequest(actionBody: JsValue): Future[Result] =
    controller.calculateLevy.apply(
      FakeRequest(POST, "/levy/calculate")
        .withHeaders(CONTENT_TYPE -> "application/json")
        .withBody(AnyContentAsJson(actionBody))
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(
      mockAuthConnector.authorise[Unit](eqTo(AuthProviders(GovernmentGateway)), any())(using
        any[HeaderCarrier],
        any[ExecutionContext]
      )
    ).thenReturn(Future.successful(()))
  }

  "LevyCalculationController.calculateLevy" should {

    "return 200 OK and levy amounts for a valid request" in {
      val levyCalculationRequestJson = Json.parse("""
        {
          "lowLitres": 1000,
          "highLitres": 500,
          "returnPeriod": { "year": 2025, "quarter": 0 }
        }
      """)

      val result = calculateLevyRequest(levyCalculationRequestJson)

      status(result) shouldBe OK
      contentType(result) shouldBe Some("application/json")

      val levyCalculationResponse = contentAsJson(result)
      (levyCalculationResponse \ "lowBandLevy").as[BigDecimal] shouldBe a[BigDecimal]
      (levyCalculationResponse \ "highBandLevy").as[BigDecimal] shouldBe a[BigDecimal]
      (levyCalculationResponse \ "totalLevy").as[BigDecimal] shouldBe a[BigDecimal]
      (levyCalculationResponse \ "totalRoundedDown").as[BigDecimal] shouldBe a[BigDecimal]
    }

    "return 400 BadRequest when request body is not valid JSON for the model (missing fields)" in {
      val levyCalculationRequestJson = Json.parse("""{ "lowLitres": 1000 }""")

      val result = calculateLevyRequest(levyCalculationRequestJson)

      status(result) shouldBe BAD_REQUEST
      val levyCalculationResponse = contentAsJson(result)
      (levyCalculationResponse \ "code").as[String] shouldBe "INVALID_REQUEST"
    }

    "return 400 BadRequest when litres are negative" in {
      val levyCalculationRequestJson = Json.parse("""
        {
          "lowLitres": -1,
          "highLitres": 500,
          "returnPeriod": { "year": 2025, "quarter": 1 }
        }
      """)

      val result = calculateLevyRequest(levyCalculationRequestJson)

      status(result) shouldBe BAD_REQUEST
      val errorJson = contentAsJson(result)
      (errorJson \ "code").as[String] shouldBe "INVALID_REQUEST"
      (errorJson \ "message").as[String] shouldBe "Invalid request payload"

      val details = (errorJson \ "details").toString
      details must include("must be non-negative")
    }

    "return 400 BadRequest when tax year is unsupported" in {
      val levyCalculationRequestJson = Json.parse("""
        {
          "lowLitres": 1,
          "highLitres": 1,
          "returnPeriod": { "year": 2099, "quarter": 1 }
        }
      """)

      val result = calculateLevyRequest(levyCalculationRequestJson)

      status(result) shouldBe BAD_REQUEST
      val levyCalculationResponse = contentAsJson(result)
      (levyCalculationResponse \ "code").as[String] shouldBe "INVALID_REQUEST"
      (levyCalculationResponse \ "message").as[String] must include("Unsupported tax year")
    }

    "return 400 BadRequest when band rates are missing for a supported year (e.g. Year2026 not configured)" in {
      val levyCalculationRequestJson = Json.parse("""
        {
          "lowLitres": 1,
          "highLitres": 1,
          "returnPeriod": { "year": 2026, "quarter": 1 }
        }
      """)

      val result = calculateLevyRequest(levyCalculationRequestJson)

      status(result) shouldBe BAD_REQUEST
      val levyCalculationResponse = contentAsJson(result)
      (levyCalculationResponse \ "code").as[String] shouldBe "INVALID_REQUEST"
      (levyCalculationResponse \ "message").as[String] must include("No band rates found for tax year")
    }

    "return 401 Unauthorized when the request is not authenticated" in {
      when(
        mockAuthConnector.authorise(
          eqTo(AuthProviders(GovernmentGateway)),
          any()
        )(using any[HeaderCarrier], any[ExecutionContext])
      ).thenReturn(Future.failed(MissingBearerToken()))

      val levyCalculationRequestJson = Json.obj(
        "lowLitres"    -> 1000,
        "highLitres"   -> 500,
        "returnPeriod" -> Json.obj("year" -> 2025, "quarter" -> 0)
      )

      val result = calculateLevyRequest(levyCalculationRequestJson)

      status(result) shouldBe UNAUTHORIZED
    }
  }
}
