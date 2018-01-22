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

import org.mockito.ArgumentMatchers.{eq => matching, _}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import reactivemongo.api.commands.DefaultWriteResult
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.softdrinksindustrylevy.connectors.{EmailConnector, Identifier, TaxEnrolmentConnector, TaxEnrolmentsSubscription}
import uk.gov.hmrc.softdrinksindustrylevy.models.Subscription
import uk.gov.hmrc.softdrinksindustrylevy.models.json.internal._
import uk.gov.hmrc.softdrinksindustrylevy.services.{MongoBufferService, SubscriptionWrapper}

import scala.concurrent.Future

class TaxEnrolmentCallbackControllerSpec extends UnitSpec with GuiceOneAppPerSuite with MockitoSugar {

  "POST /tax-enrolment" should {
    "remove the buffer record on success" in {
      when(mockTaxEnrolments.getSubscription(matching("123"))(any(), any())).thenReturn(
        Future
          .successful(
            TaxEnrolmentsSubscription(
              Some(Seq(Identifier("SdilRegistrationNumber", "XXSDIL0009999"))),
              "safe-id",
              "SUCCEEDED",
              None))
      )

      val wrapper = SubscriptionWrapper(
        "safe-id",
        Json.fromJson[Subscription](validCreateSubscriptionRequest).get,
        formBundleNumber)
      when(mockBuffer.findById(matching("safe-id"), any())(any())).thenReturn(Future.successful(Some(wrapper)))
      when(mockBuffer.removeById(matching("safe-id"), any())(any()))
        .thenReturn(Future.successful(DefaultWriteResult(true, 1, Nil, None, None, None)))
      when(mockEmail.sendConfirmationEmail(any(), any(), any())(any(), any())).thenReturn(Future.successful(()))

      val res = testController.callback("123")(FakeRequest().withBody(Json.obj("state" -> "SUCCEEDED")))

      status(res) shouldBe NO_CONTENT
      verify(mockBuffer, times(1)).removeById(matching("safe-id"), any())(any())
    }

    "send a notification email on success" in {
      when(mockTaxEnrolments.getSubscription(matching("123"))(any(), any())).thenReturn(
        Future.successful(
          TaxEnrolmentsSubscription(
            Some(Seq(Identifier("SdilRegistrationNumber", "XZSDIL0009999"))),
            "safe-id",
            "SUCCEEDED",
            None))
      )

      val wrapper = SubscriptionWrapper(
        "safe-id",
        Json.fromJson[Subscription](validCreateSubscriptionRequest).get,
        formBundleNumber)
      when(mockBuffer.findById(matching("safe-id"), any())(any())).thenReturn(Future.successful(Some(wrapper)))
      when(mockBuffer.removeById(matching("safe-id"), any())(any()))
        .thenReturn(Future.successful(DefaultWriteResult(true, 1, Nil, None, None, None)))
      when(mockEmail.sendConfirmationEmail(any(), any(), any())(any(), any())).thenReturn(Future.successful(()))

      val res = testController.callback("123")(FakeRequest().withBody(Json.obj("state" -> "SUCCEEDED")))

      status(res) shouldBe NO_CONTENT
      verify(mockEmail, times(1))
        .sendConfirmationEmail(
          matching(wrapper.subscription.orgName),
          matching(wrapper.subscription.contact.email),
          matching("XZSDIL0009999")
        )(any(), any())
    }
  }

  val mockBuffer = mock[MongoBufferService]
  val mockEmail = mock[EmailConnector]
  val mockTaxEnrolments = mock[TaxEnrolmentConnector]
  lazy val testController = new TaxEnrolmentCallbackController(mockBuffer, mockEmail, mockTaxEnrolments)
}
