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

import java.time.{Clock, LocalDate, LocalDateTime, OffsetDateTime}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.softdrinksindustrylevy.connectors.DesConnector
import uk.gov.hmrc.softdrinksindustrylevy.util.FakeApplicationSpec
import org.mockito.ArgumentMatchers.{any, eq => matching}
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.JsNull
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers._
import sdil.models.{ReturnPeriod, SdilReturn}
import uk.gov.hmrc.auth.core.retrieve.{Credentials, EmptyRetrieval}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.softdrinksindustrylevy.models.{Activity, Address, Contact, Subscription}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class ReturnsControllerSpec extends FakeApplicationSpec with MockitoSugar with BeforeAndAfterEach with ScalaFutures {
  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockDesConnector: DesConnector = mock[DesConnector]
  val mockAuditing: AuditConnector = mock[AuditConnector]

  val cc = app.injector.instanceOf[ControllerComponents]

  implicit def mockClock: Clock = Clock.systemDefaultZone()
  implicit val hc: HeaderCarrier = new HeaderCarrier

  val testReturnsContoller =
    new ReturnsController(mockAuthConnector, mockDesConnector, subscriptions, returns, mockAuditing, cc)

  override def beforeEach(): Unit =
    reset(mockDesConnector)

  when(mockAuthConnector.authorise[Option[Credentials]](any(), any())(any(), any()))
    .thenReturn(Future.successful(Option(Credentials("cred-id", "GovernmentGateway"))))

  when(mockAuthConnector.authorise[Unit](any(), matching(EmptyRetrieval))(any(), any()))
    .thenReturn(Future.successful(()))

  "variable method" should {
    "return list of variables" in {
      val testReturnPeriod = ReturnPeriod(2018, 1)
      val testUtr = "someTestUtr"
      val sdilReturn = mock[SdilReturn]
      when(returns.listVariable(any())(any())).thenReturn(Future(Map(testReturnPeriod -> sdilReturn)))
      returns.update(testUtr, testReturnPeriod, sdilReturn)

      val response = testReturnsContoller.variable(testUtr)(FakeRequest())

      status(response) mustBe OK
      contentAsString(
        response
      ) mustBe s"""[{\"year\":${testReturnPeriod.year},\"quarter\":${testReturnPeriod.quarter}}]"""
    }
  }

  "get method" should {
    "None returned" in {
      val testUtr = "someTestUtr"
      when(returns.get(any(), any())(any())).thenReturn(Future(None))
      val response = testReturnsContoller.get(testUtr, 2018, 1)(FakeRequest())

      status(response) mustBe NOT_FOUND
    }
  }

  "checkSmallProducerStatus method" should {
    "None returned by desConnector.retrieveSubscriptionDetails" in {
      val testYear = 2018
      val testQuarter = 1

      when(mockDesConnector.retrieveSubscriptionDetails(any[String], any[String])(any())) thenReturn Future.successful(
        None
      )
      when(subscriptions.list(any())(any())).thenReturn(Future(List.empty))
      val response =
        testReturnsContoller.checkSmallProducerStatus("testIdType", "1234", testYear, testQuarter)(FakeRequest())

      status(response) mustBe OK
      contentAsString(response) mustBe "false"
    }

    "Subscription returned by desConnector.retrieveSubscriptionDetails" in {
      val testYear = 2018
      val testQuarter = 1
      val testUtr = "testUtr"
      val testSdilRef = "someSdilRef"
      when(subscriptions.list(any())(any())).thenReturn(Future(List.empty))

      when(mockDesConnector.retrieveSubscriptionDetails(any[String], any[String])(any())) thenReturn Future.successful(
        Some(
          Subscription(
            testUtr,
            Some(testSdilRef),
            "someOrgName",
            None,
            mock[Address],
            mock[Activity],
            LocalDate.now,
            Nil,
            Nil,
            mock[Contact],
            None,
            None
          )
        )
      )

      returns.update(testUtr, ReturnPeriod(2018, 1), SdilReturn(submittedOn = None))
      val response =
        testReturnsContoller.checkSmallProducerStatus("testIdType", "1234", testYear, testQuarter)(FakeRequest())

      status(response) mustBe OK
      contentAsString(response) mustBe "false"
    }
  }

  "RichLong" should {
    "asMilliseconds" in {
      val testDate = LocalDateTime.now()
      val testDateString = testDate.toString
      println("testDateString " + testDateString.substring(0, 23))
      testReturnsContoller
        .RichLong(testDate.toInstant(OffsetDateTime.now().getOffset).toEpochMilli)
        .asMilliseconds
        .toString
        .substring(0, 23) mustBe testDateString.substring(0, 23)
      println("comparisonString" + testReturnsContoller)
    }
  }

  "post method" should {
    "400 returned for blank body" in {
      val response = testReturnsContoller.post("", 2018, 1)(FakeRequest().withBody(JsNull))
      status(response) mustBe BAD_REQUEST
    }
  }
}
