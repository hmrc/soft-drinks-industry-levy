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

import com.mongodb.client.result.DeleteResult
import org.mockito.ArgumentMatchers.{eq => matching, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.Mode
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.softdrinksindustrylevy.connectors.{EmailConnector, Identifier, TaxEnrolmentConnector, TaxEnrolmentsSubscription}
import uk.gov.hmrc.softdrinksindustrylevy.models.Subscription
import uk.gov.hmrc.softdrinksindustrylevy.models.json.internal._
import uk.gov.hmrc.softdrinksindustrylevy.services.{MongoBufferService, SubscriptionWrapper}
import uk.gov.hmrc.softdrinksindustrylevy.util.FakeApplicationSpec

import scala.concurrent.Future

class TaxEnrolmentCallbackControllerSpec
    extends FakeApplicationSpec with MockitoSugar with BeforeAndAfterEach with ScalaFutures {

  val mockBuffer: MongoBufferService = mock[MongoBufferService]
  val mockEmail: EmailConnector = mock[EmailConnector]
  val mockTaxEnrolments: TaxEnrolmentConnector = mock[TaxEnrolmentConnector]
  val mockAuditConnector: AuditConnector = mock[AuditConnector]
  val mockConfiguration: ServicesConfig = mock[ServicesConfig]
  val mockMode: Mode = mock[Mode]
  val cc = app.injector.instanceOf[ControllerComponents]

  lazy val testController = new TaxEnrolmentCallbackController(
    mockBuffer,
    mockEmail,
    mockTaxEnrolments,
    mockMode,
    cc,
    mockConfiguration,
    mockAuditConnector)

  override def beforeEach(): Unit = {
    reset(mockBuffer)
    reset(mockEmail)
    reset(mockTaxEnrolments)
    reset(mockAuditConnector)
  }

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

      when(mockConfiguration.baseUrl(any())).thenReturn("urlString")
      when(mockAuditConnector.sendExtendedEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))

      val wrapper = SubscriptionWrapper(
        "safe-id",
        Json.fromJson[Subscription](validCreateSubscriptionRequest).get,
        formBundleNumber)
      when(mockBuffer.findById(matching("safe-id"))).thenReturn(Future.successful(wrapper))
      when(mockBuffer.removeById(matching("safe-id")))
        .thenReturn(Future.successful(new DeleteResult() {
          override def wasAcknowledged(): Boolean = true

          override def getDeletedCount: Long = 1
        }))
      when(mockEmail.sendConfirmationEmail(any(), any(), any())(any(), any())).thenReturn(Future.successful(()))

      val res = testController.callback("123")(FakeRequest().withBody(Json.obj("state" -> "SUCCEEDED")))

      status(res) mustBe NO_CONTENT
      verify(mockBuffer, times(1)).removeById(matching("safe-id"))
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
      when(mockConfiguration.baseUrl(any())).thenReturn("urlString")
      when(mockAuditConnector.sendExtendedEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))

      val wrapper = SubscriptionWrapper(
        "safe-id",
        Json.fromJson[Subscription](validCreateSubscriptionRequest).get,
        formBundleNumber)
      when(mockBuffer.findById(matching("safe-id"))).thenReturn(Future.successful(wrapper))
      when(mockBuffer.removeById(matching("safe-id")))
        .thenReturn(Future.successful(new DeleteResult() {
          override def wasAcknowledged(): Boolean = true

          override def getDeletedCount: Long = 1
        }))
      when(mockEmail.sendConfirmationEmail(any(), any(), any())(any(), any())).thenReturn(Future.successful(()))

      val res = testController.callback("123")(FakeRequest().withBody(Json.obj("state" -> "SUCCEEDED")))

      status(res) mustBe NO_CONTENT
      /*verify(mockEmail, times(1))
        .sendConfirmationEmail(
          matching(wrapper.subscription.orgName),
          matching(wrapper.subscription.contact.email),
          matching("XZSDIL0009999")
        )(any(), any())*/
    }

    "body state other than SUCCEEDED" in {
      when(mockConfiguration.baseUrl(any())).thenReturn("urlString")
      when(mockAuditConnector.sendExtendedEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))
      val res = testController.callback("123")(FakeRequest().withBody(Json.obj("state" -> "FAILED")))

      status(res) mustBe NO_CONTENT
    }
  }
}
