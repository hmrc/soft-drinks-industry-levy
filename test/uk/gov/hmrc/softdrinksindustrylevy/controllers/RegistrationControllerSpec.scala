/*
 * Copyright 2020 HM Revenue & Customs
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

import java.time.LocalDate

import org.mockito.ArgumentMatchers.{any, eq => matching}
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import reactivemongo.api.commands._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.retrieve.{Credentials, EmptyRetrieval}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, NotFoundException}
import uk.gov.hmrc.softdrinksindustrylevy.connectors._
import uk.gov.hmrc.softdrinksindustrylevy.models._
import uk.gov.hmrc.softdrinksindustrylevy.services.SubscriptionWrapper._
import uk.gov.hmrc.softdrinksindustrylevy.services.{MongoBufferService, SubscriptionWrapper}
import uk.gov.hmrc.softdrinksindustrylevy.util.FakeApplicationSpec
import com.softwaremill.macwire._
import uk.gov.hmrc.softdrinksindustrylevy.config.SdilComponents

import scala.concurrent.Future

class RegistrationControllerSpec extends FakeApplicationSpec with MockitoSugar with BeforeAndAfterEach {

  val mockTaxEnrolmentConnector = mock[TaxEnrolmentConnector]
  val mockDesConnector: DesConnector = mock[DesConnector]
  val mockBuffer: MongoBufferService = mock[MongoBufferService]
  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockEmailConnector: EmailConnector = mock[EmailConnector]

  lazy val cc = new SdilComponents(context).cc
  val testSdilController = wire[RegistrationController]

  implicit val hc: HeaderCarrier = new HeaderCarrier

  override def beforeEach() {
    reset(mockDesConnector)
  }

  when(mockAuthConnector.authorise[Credentials](any(), any())(any(), any()))
    .thenReturn(Future.successful(Credentials("cred-id", "GovernmentGateway")))

  when(mockAuthConnector.authorise[Unit](any(), matching(EmptyRetrieval))(any(), any()))
    .thenReturn(Future.successful(()))

  when(mockEmailConnector.sendSubmissionReceivedEmail(any(), any())(any(), any())).thenReturn(Future.successful(()))

  "SdilController" should {
    "return Status: OK Body: CreateSubscriptionResponse for successful valid subscription" in {
      import json.des.create._
      when(mockDesConnector.createSubscription(any(), any(), any())(any()))
        .thenReturn(Future.successful(validSubscriptionResponse))

      when(mockBuffer.insert(any())(any()))
        .thenReturn(Future.successful(DefaultWriteResult(true, 1, Nil, None, None, None)))
      when(mockTaxEnrolmentConnector.subscribe(any(), any())(any(), any()))
        .thenReturn(Future.successful(HttpResponse(418)))

      val response = testSdilController.submitRegistration("UTR", "0000222200", "foobar")(
        FakeRequest()
          .withBody(validCreateSubscriptionRequest))

      status(response) mustBe OK
      verify(mockDesConnector, times(1)).createSubscription(any(), any(), any())(any())
      contentAsJson(response) mustBe Json.toJson(validSubscriptionResponse)
    }

    "return Status: BAD_REQUEST for invalid request" in {
      val result = testSdilController.submitRegistration("UTR", "00002222", "barfoo")(
        FakeRequest()
          .withBody(
            Json.parse(
              """{
                |"test": "bad"
                |}
          """.stripMargin
            )))

      status(result) mustBe BAD_REQUEST
    }

    "return Status: Conflict for duplicate subscription submission" in {
      when(mockDesConnector.createSubscription(any(), any(), any())(any()))
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
        wtime = None
      )))

      val response = testSdilController.submitRegistration("UTR", "00002222", "foo")(
        FakeRequest()
          .withBody(validCreateSubscriptionRequest))

      status(response) mustBe CONFLICT
      contentAsJson(response) mustBe Json.obj("status" -> "UTR_ALREADY_SUBSCRIBED")
    }

    "return Status: Exception rethrown for error code other than 11000" in {
      when(mockDesConnector.createSubscription(any(), any(), any())(any()))
        .thenReturn(Future.successful(validSubscriptionResponse))

      val testLastError = LastError(
        ok = false,
        errmsg = None,
        code = Some(1),
        lastOp = None,
        n = 2,
        singleShard = None,
        updatedExisting = false,
        upserted = None,
        wnote = None,
        wtimeout = false,
        waited = None,
        wtime = None
      )

      when(mockBuffer.insert(any())(any())).thenReturn(Future.failed(testLastError))

      the[LastError] thrownBy contentAsString(
        testSdilController.submitRegistration("UTR", "00002222", "foo")(FakeRequest()
          .withBody(validCreateSubscriptionRequest)))

    }

    "return Status: NOT_FOUND for subscription for a sub that isn't in Des or the pending queue (Mongo)" in {
      when(mockBuffer.find(any())(any())).thenReturn(Future.successful(Nil))
      when(mockDesConnector.retrieveSubscriptionDetails(any(), any())(any()))
        .thenReturn(Future successful None)

      val response = testSdilController.checkEnrolmentStatus("123")(FakeRequest())

      status(response) mustBe NOT_FOUND
    }

    "return Status: ACCEPTED for subscription for a sub that isn't in Des but is in the pending queue (Mongo)" in {
      val wrapper = SubscriptionWrapper(
        "safe-id",
        Json.fromJson[Subscription](validCreateSubscriptionRequest).get,
        formBundleNumber)
      when(mockDesConnector.retrieveSubscriptionDetails(any(), any())(any()))
        .thenReturn(Future successful None)
      when(mockBuffer.find(any())(any())).thenReturn(Future.successful(List(wrapper)))
      val response = testSdilController.checkEnrolmentStatus("123")(FakeRequest())

      status(response) mustBe ACCEPTED
    }

    "return Status: OK for subscription for a sub that is in Des but is not in the pending queue (Mongo)" in {
      when(mockBuffer.find(any())(any())).thenReturn(Future.successful(Nil))
      when(mockDesConnector.retrieveSubscriptionDetails(any(), any())(any()))
        .thenReturn(Future successful Some(Json.fromJson[Subscription](validCreateSubscriptionRequest).get))

      val response = testSdilController.checkEnrolmentStatus("123")(FakeRequest())
      status(response) mustBe OK
    }

    "return Status: OK for subscription for a sub that has an enrolment and is in the pending queue (Mongo)" +
      " and delete from the queue" in {
      val wrapper = SubscriptionWrapper(
        "safe-id",
        Json.fromJson[Subscription](validCreateSubscriptionRequest).get,
        formBundleNumber)
      when(mockTaxEnrolmentConnector.getSubscription(any())(any(), any())).thenReturn(
        Future.successful(
          TaxEnrolmentsSubscription(
            Some(Seq(Identifier("SdilRegistrationNumber", "XZSDIL0009999"))),
            "safe-id",
            "SUCCEEDED",
            None)
        )
      )
      when(mockBuffer.find(any())(any())).thenReturn(Future.successful(List(wrapper)))
      when(mockDesConnector.retrieveSubscriptionDetails(any(), any())(any()))
        .thenReturn(Future successful Some(Json.fromJson[Subscription](validCreateSubscriptionRequest).get))
      when(mockBuffer.remove(any())(any()))
        .thenReturn(Future successful DefaultWriteResult(true, 1, Nil, None, None, None))

      val response = testSdilController.checkEnrolmentStatus("123")(FakeRequest())
      status(response) mustBe OK
    }

    "Returns OK for TaxEnrolmentsSubscription state of ERROR" in {
      val wrapper = SubscriptionWrapper(
        "safe-id",
        Json.fromJson[Subscription](validCreateSubscriptionRequest).get,
        formBundleNumber)
      when(mockTaxEnrolmentConnector.getSubscription(any())(any(), any())).thenReturn(
        Future.successful(
          TaxEnrolmentsSubscription(
            Some(Seq(Identifier("SdilRegistrationNumber", "XZSDIL0009999"))),
            "safe-id",
            "ERROR",
            None)
        )
      )
      when(mockBuffer.find(any())(any())).thenReturn(Future.successful(List(wrapper)))
      when(mockDesConnector.retrieveSubscriptionDetails(any(), any())(any()))
        .thenReturn(Future successful Some(Json.fromJson[Subscription](validCreateSubscriptionRequest).get))
      when(mockBuffer.remove(any())(any()))
        .thenReturn(Future successful DefaultWriteResult(true, 1, Nil, None, None, None))

      val response = testSdilController.checkEnrolmentStatus("123")(FakeRequest())
      status(response) mustBe OK
    }

    "Returns ACCEPTED for TaxEnrolmentsSubscription state of PENDING" in {
      val wrapper = SubscriptionWrapper(
        "safe-id",
        Json.fromJson[Subscription](validCreateSubscriptionRequest).get,
        formBundleNumber)
      when(mockTaxEnrolmentConnector.getSubscription(any())(any(), any())).thenReturn(
        Future.successful(
          TaxEnrolmentsSubscription(
            Some(Seq(Identifier("SdilRegistrationNumber", "XZSDIL0009999"))),
            "safe-id",
            "PENDING",
            None)
        )
      )
      when(mockBuffer.find(any())(any())).thenReturn(Future.successful(List(wrapper)))
      when(mockDesConnector.retrieveSubscriptionDetails(any(), any())(any()))
        .thenReturn(Future successful Some(Json.fromJson[Subscription](validCreateSubscriptionRequest).get))
      when(mockBuffer.remove(any())(any()))
        .thenReturn(Future successful DefaultWriteResult(true, 1, Nil, None, None, None))

      val response = testSdilController.checkEnrolmentStatus("123")(FakeRequest())
      status(response) mustBe ACCEPTED
    }

    "Returns OK for TaxEnrolmentsSubscription state of anything else" in {
      val wrapper = SubscriptionWrapper(
        "safe-id",
        Json.fromJson[Subscription](validCreateSubscriptionRequest).get,
        formBundleNumber)
      when(mockTaxEnrolmentConnector.getSubscription(any())(any(), any())).thenReturn(
        Future.successful(
          TaxEnrolmentsSubscription(
            Some(Seq(Identifier("SdilRegistrationNumber", "XZSDIL0009999"))),
            "safe-id",
            "INVALIDSTRING",
            None)
        )
      )
      when(mockBuffer.find(any())(any())).thenReturn(Future.successful(List(wrapper)))
      when(mockDesConnector.retrieveSubscriptionDetails(any(), any())(any()))
        .thenReturn(Future successful Some(Json.fromJson[Subscription](validCreateSubscriptionRequest).get))
      when(mockBuffer.remove(any())(any()))
        .thenReturn(Future successful DefaultWriteResult(true, 1, Nil, None, None, None))

      val response = testSdilController.checkEnrolmentStatus("123")(FakeRequest())
      status(response) mustBe OK
    }

    "return Status: OK for subscription for a sub that doesn't have an enrolment but is in the pending queue (Mongo)" +
      " n.b. an error should be logged" in {
      val wrapper = SubscriptionWrapper(
        "safe-id",
        Json.fromJson[Subscription](validCreateSubscriptionRequest).get,
        formBundleNumber)
      when(mockTaxEnrolmentConnector.getSubscription(any())(any(), any())).thenReturn(
        Future.failed(new NotFoundException("foo"))
      )
      when(mockBuffer.find(any())(any())).thenReturn(Future.successful(List(wrapper)))
      when(mockDesConnector.retrieveSubscriptionDetails(any(), any())(any()))
        .thenReturn(Future successful Some(Json.fromJson[Subscription](validCreateSubscriptionRequest).get))

      val response = testSdilController.checkEnrolmentStatus("123")(FakeRequest())
      status(response) mustBe OK
    }

    "return Status: NOT_FOUND for a subscription that has been deregistered" in {
      when(mockBuffer.find(any())(any())).thenReturn(Future.successful(Nil))
      val deregisteredSubscription =
        Json.fromJson[Subscription](validCreateSubscriptionRequest).get.copy(deregDate = Some(LocalDate.now))
      when(mockDesConnector.retrieveSubscriptionDetails(any(), any())(any()))
        .thenReturn(Future successful Some(deregisteredSubscription))

      val response = testSdilController.checkEnrolmentStatus("123")(FakeRequest())
      status(response) mustBe NOT_FOUND
    }
  }

  "retrieveSubscriptionDetails" should {
    "retrieveSubscriptionDetails returning None" in {
      when(mockDesConnector.retrieveSubscriptionDetails(any(), any())(any()))
        .thenReturn(Future successful None)

      val response = testSdilController.retrieveSubscriptionDetails("", "123")(FakeRequest())
      status(response) mustBe NOT_FOUND
    }

    "retrieveSubscriptionDetails returning Some" in {
      val deregisteredSubscription = Json.fromJson[Subscription](validCreateSubscriptionRequest).get
      when(mockDesConnector.retrieveSubscriptionDetails(any(), any())(any()))
        .thenReturn(Future successful Some(deregisteredSubscription))

      val response = testSdilController.retrieveSubscriptionDetails("", "123")(FakeRequest())
      status(response) mustBe OK
      contentAsJson(response) mustBe Json.toJson(validCreateSubscriptionRequest)
    }
  }

  "checkSmallProducerStatus" should {
    "retrieveSubscriptionDetails returning None" in {
      when(mockDesConnector.retrieveSubscriptionDetails(any(), any())(any()))
        .thenReturn(Future successful None)

      val response = testSdilController.checkSmallProducerStatus("123", "123", 2018, 1)(FakeRequest())
      status(response) mustBe OK
      contentAsString(response) mustBe "false"
    }

    "retrieveSubscriptionDetails returning Some of non-small producer" in {
      val deregisteredSubscription =
        Json.fromJson[Subscription](validCreateSubscriptionRequest).get.copy(deregDate = Some(LocalDate.now))
      when(mockDesConnector.retrieveSubscriptionDetails(any(), any())(any()))
        .thenReturn(Future successful Some(deregisteredSubscription))

      val response = testSdilController.checkSmallProducerStatus("123", "123", 2018, 1)(FakeRequest())
      status(response) mustBe OK
      contentAsString(response) mustBe "false"
    }
  }
}
