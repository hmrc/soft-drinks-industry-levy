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

package uk.gov.hmrc.softdrinksindustrylevy.services

import java.time.LocalDate

import akka.actor.ActorSystem
import akka.testkit.TestActorRef
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterEach}
import org.scalatest.mockito.MockitoSugar
import sdil.models.{ReturnPeriod, SdilReturn}
import uk.gov.hmrc.softdrinksindustrylevy.connectors.DesConnector
import org.scalatest.concurrent.ScalaFutures
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.softdrinksindustrylevy.models.{Activity, Address, Contact, Subscription}

import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.softdrinksindustrylevy.services.DataCorrector.ReturnsCorrection
import uk.gov.hmrc.softdrinksindustrylevy.util.FakeApplicationSpec

import scala.concurrent.Future

class DataCorrectorSpec extends FakeApplicationSpec with MockitoSugar with ScalaFutures with BeforeAndAfterEach {

  val mockDesConnector = mock[DesConnector]
  override def beforeEach() {
    reset(mockDesConnector)
  }

  "ReturnsCorrection" should {
    val testSdilRef = Some("123")
    val testUtr = Some("456")

    "sdilRef and utr not defined" in {
      the[IllegalArgumentException] thrownBy ReturnsCorrection(None, None, mock[ReturnPeriod], mock[SdilReturn]) must have message ("requirement failed: Either sdilRef or utr must be defined")
    }

    "Only sdilRef defined" in {
      ReturnsCorrection(testSdilRef, None, mock[ReturnPeriod], mock[SdilReturn]).sdilRef mustBe testSdilRef
    }

    "Only utr defined" in {
      ReturnsCorrection(None, testUtr, mock[ReturnPeriod], mock[SdilReturn]).utr mustBe testUtr
    }

    "sdilRef and utr both defined" in {
      val result = ReturnsCorrection(testSdilRef, testUtr, mock[ReturnPeriod], mock[SdilReturn])
      result.sdilRef mustBe testSdilRef
      result.utr mustBe testUtr
    }
  }

  "ReturnsCorrectorWorker getUtrFromSdil" should {
    implicit val system = ActorSystem()
    val testSdilRef = "someSdilRef"
    val testReturnsCorrector = TestActorRef(new ReturnsCorrectorWorker(mockDesConnector, mock[SdilPersistence]))

    "getUtrFromSdil with None" in {
      when(mockDesConnector.retrieveSubscriptionDetails(any[String], any[String])(any())) thenReturn Future.successful(
        None)
      val result = testReturnsCorrector.underlyingActor.getUtrFromSdil(testSdilRef)

      whenReady(result.failed)(e => {
        e mustBe a[NoSuchElementException]
        e.getMessage mustBe s"Cannot find subscription with SDIL ref $testSdilRef"
      })
    }

    "getUtrFromSdil with Some" in {
      val testUtr = "someTestUtr"
      when(mockDesConnector.retrieveSubscriptionDetails(any[String], any[String])(any())) thenReturn Future.successful(
        Some(
          Subscription(
            testUtr,
            Some(testSdilRef),
            "",
            None,
            mock[Address],
            mock[Activity],
            LocalDate.now(),
            Nil,
            Nil,
            mock[Contact],
            None)))

      val result = testReturnsCorrector.underlyingActor.getUtrFromSdil(testSdilRef)

      result.map(str => str mustBe testUtr)
    }
  }
}
