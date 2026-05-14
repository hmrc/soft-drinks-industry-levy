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

import org.mockito.ArgumentMatchers.{any, eq as matching}
import org.mockito.Mockito.{never, reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsNull, Json}
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import sdil.models.{ReturnPeriod, SdilReturn}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.retrieve.{Credentials, EmptyRetrieval}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.softdrinksindustrylevy.connectors.{DesConnector, HipConnector}
import uk.gov.hmrc.softdrinksindustrylevy.models.*
import uk.gov.hmrc.softdrinksindustrylevy.util.FakeApplicationSpec

import java.time.{Clock, LocalDate, LocalDateTime, OffsetDateTime}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ReturnsControllerSpec extends FakeApplicationSpec with MockitoSugar with BeforeAndAfterEach with ScalaFutures {
  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockAuditing: AuditConnector = mock[AuditConnector]
  val mockDesConnector: DesConnector = mock[DesConnector]
  val mockHipConnector: HipConnector = mock[HipConnector]
  val mockServicesConfig: ServicesConfig = mock[ServicesConfig]

  val cc = app.injector.instanceOf[ControllerComponents]

  implicit def mockClock: Clock = Clock.systemDefaultZone()

  implicit val hc: HeaderCarrier = new HeaderCarrier

  val testReturnsContoller =
    new ReturnsController(
      mockAuthConnector,
      mockDesConnector,
      mockHipConnector,
      mockServicesConfig,
      subscriptions,
      returns,
      mockAuditing,
      cc
    )

  override def beforeEach(): Unit = {
    reset(mockDesConnector, mockHipConnector, mockServicesConfig)
    when(mockServicesConfig.getBoolean("features.hip.integration")).thenReturn(false)
  }

  when(mockAuthConnector.authorise[Option[Credentials]](any(), any())(using any(), any()))
    .thenReturn(Future.successful(Option(Credentials("cred-id", "GovernmentGateway"))))

  when(mockAuthConnector.authorise[Unit](any(), matching(EmptyRetrieval))(using any(), any()))
    .thenReturn(Future.successful(()))

  "variable method" should {
    "return list of variables" in {
      val testReturnPeriod = ReturnPeriod(2018, 1)
      val testUtr = "someTestUtr"
      val sdilReturn = mock[SdilReturn]
      when(returns.listVariable(any())(using any())).thenReturn(Future(Map(testReturnPeriod -> sdilReturn)))
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
      when(returns.get(any(), any())(using any())).thenReturn(Future(None))
      val response = testReturnsContoller.get(testUtr, 2018, 1)(FakeRequest())

      status(response) mustBe NOT_FOUND
    }
  }

  "checkSmallProducerStatus method" should {
    "None returned by desConnector.retrieveSubscriptionDetails" in {
      val testYear = 2018
      val testQuarter = 1

      when(mockDesConnector.retrieveSubscriptionDetails(any[String], any[String])(using any())).thenReturn(
        Future.successful(
          None
        )
      )
      when(subscriptions.list(any())(using any())).thenReturn(Future(List.empty))
      val response =
        testReturnsContoller.checkSmallProducerStatus("testIdType", "1234", testYear, testQuarter)(FakeRequest())

      status(response) mustBe OK
      contentAsString(response) mustBe "false"
    }

    "use HIP retrieve subscription details when HIP integration is enabled" in {
      val testYear = 2018
      val testQuarter = 1

      when(mockServicesConfig.getBoolean("features.hip.integration")).thenReturn(true)
      when(mockHipConnector.retrieveSubscriptionDetails(any[String], any[String])(using any())).thenReturn(
        Future.successful(None)
      )

      val response =
        testReturnsContoller.checkSmallProducerStatus("testIdType", "1234", testYear, testQuarter)(FakeRequest())

      status(response) mustBe OK
      contentAsString(response) mustBe "false"
      verify(mockHipConnector, times(1)).retrieveSubscriptionDetails(any[String], any[String])(using any())
      verify(mockDesConnector, never()).retrieveSubscriptionDetails(any[String], any[String])(using any())
    }

    "Subscription returned by desConnector.retrieveSubscriptionDetails" in {
      val testYear = 2018
      val testQuarter = 1
      val testUtr = "testUtr"
      val testSdilRef = "someSdilRef"
      when(subscriptions.list(any())(using any())).thenReturn(Future(List.empty))

      when(mockDesConnector.retrieveSubscriptionDetails(any[String], any[String])(using any())).thenReturn(
        Future.successful(
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
      testReturnsContoller
        .RichLong(testDate.toInstant(OffsetDateTime.now().getOffset).toEpochMilli)
        .asMilliseconds
        .toString
        .substring(0, 23) mustBe testDateString.substring(0, 23)
    }
  }

  "post method" should {
    "400 returned for blank body" in {
      val response = testReturnsContoller.post("", 2018, 1)(FakeRequest().withBody(JsNull))
      status(response) mustBe BAD_REQUEST
    }

    "use HIP submit return when HIP integration is enabled" in {
      val utr = "testUtr"
      val sdilRef = "XKSDIL000000022"
      val period = ReturnPeriod(2024, 1)
      val sdilReturn = SdilReturn(submittedOn = None)

      when(mockServicesConfig.getBoolean("features.hip.integration")).thenReturn(true)
      when(mockHipConnector.retrieveSubscriptionDetails(any[String], any[String])(using any())).thenReturn(
        Future.successful(
          Some(
            Subscription(
              utr,
              Some(sdilRef),
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
      )
      when(mockHipConnector.submitReturn(any[String], any[ReturnsRequest])(using any(), any()))
        .thenReturn(Future.successful(HttpResponse(CREATED, "")))
      when(mockAuditing.sendExtendedEvent(any())(using any(), any())).thenReturn(Future.successful(AuditResult.Success))
      when(returns.update(any[String], any[ReturnPeriod], any[SdilReturn])(using any())).thenReturn(
        Future.successful(())
      )

      val response = testReturnsContoller.post(utr, period.year, period.quarter)(
        FakeRequest().withBody(Json.toJson(sdilReturn))
      )

      status(response) mustBe OK
      verify(mockHipConnector, times(1)).retrieveSubscriptionDetails(any[String], any[String])(using any())
      verify(mockHipConnector, times(1)).submitReturn(any[String], any[ReturnsRequest])(using any(), any())
      verify(mockDesConnector, never()).retrieveSubscriptionDetails(any[String], any[String])(using any())
      verify(mockDesConnector, never()).submitReturn(any[String], any[ReturnsRequest])(using any(), any())
    }
  }

  "buildReturnAuditDetails method coverage" in {
    val exportedLitreBand = (109L, 110L)
    val wastedLitreBand = (111L, 112L)
    val testSmallProd1 = SmallProducerVolume("", exportedLitreBand)
    val testSmallProd2 = SmallProducerVolume("", wastedLitreBand)
    val returnsImporting = new ReturnsImporting((111L, 112L), (111L, 112L))
    val returnsPackaging = new ReturnsPackaging(
      Seq(testSmallProd1, testSmallProd2),
      (111L, 112L)
    )
    val returnsRequest = new ReturnsRequest(
      packaged = Some(returnsPackaging),
      imported = Some(returnsImporting),
      exported = Some(exportedLitreBand),
      wastage = Some(wastedLitreBand)
    )
    val sdilReturn1 = new SdilReturn((0, 0), (0, 0), Nil, (0, 0), (0, 0), (0, 0), (0, 0), None)
    val returnPeriod1 = new ReturnPeriod(2024, 2)
    when(returns.get(any(), any())(using any())).thenReturn(Future(None))
    testReturnsContoller.buildReturnAuditDetail(sdilReturn1, returnsRequest, "", returnPeriod1, None, "", "")
  }
  "pending method" should {
    "return pending periods" in {
      val testUtr = "testUtr"
      val startDate = LocalDate.of(2018, 1, 1)
      val liabilityDate = startDate

      when(mockDesConnector.retrieveSubscriptionDetails(any[String], any[String])(using any())).thenReturn(
        Future.successful(
          Some(
            Subscription(
              testUtr,
              Some("testSdilRef"),
              "someOrgName",
              None,
              mock[Address],
              mock[Activity],
              liabilityDate,
              Nil,
              Nil,
              mock[Contact],
              None,
              None
            )
          )
        )
      )

      when(returns.list(any())(using any())).thenReturn(Future.successful(Map.empty))

      val response = testReturnsContoller.pending(testUtr)(FakeRequest())

      status(response) mustBe OK
      contentAsString(response) must include("year")
      contentAsString(response) must include("quarter")
    }

    "use HIP retrieve subscription details when HIP integration is enabled" in {
      when(mockServicesConfig.getBoolean("features.hip.integration")).thenReturn(true)
      when(mockHipConnector.retrieveSubscriptionDetails(any[String], any[String])(using any())).thenReturn(
        Future.successful(None)
      )

      val response = testReturnsContoller.pending("missingUtr")(FakeRequest())

      status(response) mustBe OK
      contentAsJson(response) mustBe Json.arr()
      verify(mockHipConnector, times(1)).retrieveSubscriptionDetails(any[String], any[String])(using any())
      verify(mockDesConnector, never()).retrieveSubscriptionDetails(any[String], any[String])(using any())
    }

    "return an empty list when no subscription exists" in {
      when(mockDesConnector.retrieveSubscriptionDetails(any[String], any[String])(using any())).thenReturn(
        Future.successful(None)
      )

      val response = testReturnsContoller.pending("missingUtr")(FakeRequest())

      status(response) mustBe OK
      contentAsJson(response) mustBe Json.arr()
    }
  }
}
