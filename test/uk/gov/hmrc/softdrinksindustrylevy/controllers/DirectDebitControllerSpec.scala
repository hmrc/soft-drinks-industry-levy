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
import play.api.mvc.{ControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import uk.gov.hmrc.softdrinksindustrylevy.models.DisplayDirectDebitResponse
import uk.gov.hmrc.softdrinksindustrylevy.util.FakeApplicationSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DirectDebitControllerSpec
    extends FakeApplicationSpec with MockitoSugar with BeforeAndAfterEach with ScalaFutures {

  val mockAuthConnector: AuthConnector = mock[AuthConnector]

  val cc = app.injector.instanceOf[ControllerComponents]
  val testDirectDebitController = new DirectDebitController(mockSdilConnector, cc, mockAuthConnector)

  implicit val hc: HeaderCarrier = new HeaderCarrier

  override def beforeEach(): Unit =
    reset(mockSdilConnector)

  when(mockAuthConnector.authorise[Unit](any(), any())(using any(), any())).thenReturn(Future.successful(()))

  "DirectDebitController" should {
    "return an OK with a DirectDebitResponse" in {
      when(mockSdilConnector.displayDirectDebit(any())(using any()))
        .thenReturn(Future.successful(DisplayDirectDebitResponse(true)))

      val response = testDirectDebitController.checkDirectDebitStatus("XMSDIL000000001")(FakeRequest())

      status(response) mustBe OK
      verify(mockSdilConnector, times(1)).displayDirectDebit(any())(using any())
      contentAsJson(response) mustBe Json.parse("""{
                                                  |   "directDebitMandateFound" : true
                                                  |}""".stripMargin)
    }

    "fail with NotFoundException when des connector fails with NotFoundException" in {
      val mockedException = Future.failed(new NotFoundException(""))

      when(mockSdilConnector.displayDirectDebit(any())(using any())).thenReturn(mockedException)

      val result: Future[Result] = testDirectDebitController.checkDirectDebitStatus("XMSDIL000000001")(FakeRequest())
      val exception: Throwable = result.failed.futureValue

      exception mustBe a[uk.gov.hmrc.http.NotFoundException]
      verify(mockSdilConnector, times(1)).displayDirectDebit(any())(using any())
    }

  }

}
