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
import com.softwaremill.macwire.wire
import org.mockito.Mockito.{reset, times, verify, when}
import play.api.test.Helpers._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.softdrinksindustrylevy.connectors.DesConnector
import uk.gov.hmrc.softdrinksindustrylevy.models.DisplayDirectDebitResponse
import uk.gov.hmrc.softdrinksindustrylevy.util.FakeApplicationSpec
import uk.gov.hmrc.http.NotFoundException

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DirectDebitControllerSpec
    extends FakeApplicationSpec with MockitoSugar with BeforeAndAfterEach with ScalaFutures {

  val mockDesConnector: DesConnector = mock[DesConnector]
  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val testDirectDebitController = mock[DirectDebitController]

  implicit val hc: HeaderCarrier = new HeaderCarrier

  override def beforeEach() {
    reset(mockDesConnector)
  }

  when(mockAuthConnector.authorise[Unit](any(), any())(any(), any())).thenReturn(Future.successful(()))

  "DirectDebitController" should {
    "return an OK with a DirectDebitResponse" in {
      when(mockDesConnector.displayDirectDebit(any())(any()))
        .thenReturn(Future.successful(DisplayDirectDebitResponse(true)))

      val response = testDirectDebitController.checkDirectDebitStatus("XMSDIL000000001")(FakeRequest())

      status(response) mustBe OK
      verify(mockDesConnector, times(1)).displayDirectDebit(any())(any())
      contentAsJson(response) mustBe Json.parse("""{
                                                  |   "directDebitMandateFound" : true
                                                  |}""".stripMargin)
    }

    "fail with NotFoundException when des connector fails with NotFoundException" in {
      val mockedException = Future.failed(new NotFoundException(""))

      when(mockDesConnector.displayDirectDebit(any())(any())).thenReturn(mockedException)

      val result: Future[Result] = testDirectDebitController.checkDirectDebitStatus("XMSDIL000000001")(FakeRequest())
      val exception: Throwable = result.failed.futureValue

      exception mustBe a[uk.gov.hmrc.http.NotFoundException]
      verify(mockDesConnector, times(1)).displayDirectDebit(any())(any())
    }

  }

}
