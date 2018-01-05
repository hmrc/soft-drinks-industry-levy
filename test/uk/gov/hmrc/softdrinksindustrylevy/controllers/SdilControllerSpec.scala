/*
 * Copyright 2018 HM Revenue & Customs
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
import reactivemongo.api.commands._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.softdrinksindustrylevy.connectors.{DesConnector, TaxEnrolmentConnector}
import uk.gov.hmrc.softdrinksindustrylevy.models._
import uk.gov.hmrc.softdrinksindustrylevy.services.{DesSubmissionService, MongoBufferService, SubscriptionWrapper}

import scala.concurrent.Future

class SdilControllerSpec extends PlaySpec with MockitoSugar with GuiceOneAppPerSuite with BeforeAndAfterEach {
  val mockDesSubmissionService: DesSubmissionService = mock[DesSubmissionService]
  val mockTaxEnrolmentConnector = mock[TaxEnrolmentConnector]
  val mockDesConnector: DesConnector = mock[DesConnector]
  val mockBuffer: MongoBufferService = mock[MongoBufferService]
  val testSdilController = new SdilController(
    mockDesSubmissionService, mockTaxEnrolmentConnector, mockDesConnector, mockBuffer
  )

  implicit val hc = new HeaderCarrier

  override def beforeEach() {
    reset(mockDesSubmissionService, mockDesConnector)
  }

  "SdilController" should {
    "return Status: OK Body: CreateSubscriptionResponse for successful valid subscription" in {
      import json.des.create._
      when(mockDesConnector.createSubscription(any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(validSubscriptionResponse))

      when(mockBuffer.insert(any())(any())).thenReturn(Future.successful(DefaultWriteResult(true, 1, Nil, None, None, None)))
      when(mockTaxEnrolmentConnector.subscribe(any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(418)))

      val response = testSdilController.submitRegistration("UTR", "00002222", "foobar")(FakeRequest()
        .withBody(validCreateSubscriptionRequest))

      status(response) mustBe OK
      verify(mockDesConnector, times(1)).createSubscription(any(), any(), any())(any(), any())
      contentAsJson(response) mustBe Json.toJson(validSubscriptionResponse)
    }

    "return Status: BAD_REQUEST for invalid request" in {
      val result = testSdilController.submitRegistration("UTR", "00002222", "barfoo")(FakeRequest()
        .withBody(invalidCreateSubscriptionRequest))

      status(result) mustBe BAD_REQUEST
    }

    "return Status: Conflict for duplicate subscription submission" in {
      when(mockDesConnector.createSubscription(any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(validSubscriptionResponse))

      when(mockBuffer.insert(any())(any())).thenReturn(Future.failed(LastError(
        ok = false,
        errmsg = None,
        code = Some(11000),
        lastOp = None,
        n = 2,
        singleShard = None,
        updatedExisting = false,
        upserted = None,
        wnote = None,
        wtimeout = false,
        waited = None,
        wtime = None)))

      val response = testSdilController.submitRegistration("UTR", "00002222", "foo")(FakeRequest()
        .withBody(validCreateSubscriptionRequest))

      status(response) mustBe CONFLICT
      verify(mockDesConnector, times(0)).createSubscription(any(), any(), any())(any(), any())
      contentAsJson(response) mustBe Json.obj("status" -> "UTR_ALREADY_SUBSCRIBED")
    }

    "return Status: OK for subscription found in pending queue" in {
      import uk.gov.hmrc.softdrinksindustrylevy.models.json.internal._

      val sub = SubscriptionWrapper("safe-id", Json.fromJson[Subscription](validCreateSubscriptionRequest).get)

      when(mockBuffer.find(any())(any())).thenReturn(Future.successful(List(sub)))

      val response = testSdilController.checkPendingSubscription("00002222")(FakeRequest())

      status(response) mustBe OK
      contentAsJson(response) mustBe Json.obj("status" -> "SUBSCRIPTION_PENDING")
    }

    "return Status: NOT_FOUND for subscription not in pending queue" in {
      when(mockBuffer.find(any())(any())).thenReturn(Future.successful(Nil))

      val response = testSdilController.checkPendingSubscription("00002222")(FakeRequest())

      status(response) mustBe NOT_FOUND
      contentAsJson(response) mustBe Json.obj("status" -> "SUBSCRIPTION_NOT_FOUND")
    }
  }
}
