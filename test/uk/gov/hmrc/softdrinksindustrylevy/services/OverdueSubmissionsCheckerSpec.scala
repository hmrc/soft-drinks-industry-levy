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

package uk.gov.hmrc.softdrinksindustrylevy.services

import akka.actor.ActorSystem
import org.mockito.ArgumentMatchers.{any, contains, eq => mEq}
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import uk.gov.hmrc.mongo.lock.MongoLockRepository
import uk.gov.hmrc.softdrinksindustrylevy.connectors.ContactFrontendConnector
import uk.gov.hmrc.softdrinksindustrylevy.util.FakeApplicationSpec

import scala.concurrent.ExecutionContext

class OverdueSubmissionsCheckerSpec extends FakeApplicationSpec with MockitoSugar with ScalaFutures {
  val minutesTestVal: Long = 59

  "vals" should {
    val configMock: Configuration = mock[Configuration]
    when(configMock.getOptional[Boolean](contains("enabled"))(any())) thenReturn Option(false)
    when(configMock.getOptional[Long](contains("Minutes"))(any())) thenReturn Option(minutesTestVal)
    val overdueSubmissionsCheckerMock = new OverdueSubmissionsChecker(
      mock[MongoLockRepository],
      mock[MongoBufferService],
      configMock,
      mock[ActorSystem],
      mock[ContactFrontendConnector])(mock[ExecutionContext])

    "jobEnabled correct" in {
      overdueSubmissionsCheckerMock.jobEnabled mustBe false
    }

    "jobStartDelay correct" in {
      overdueSubmissionsCheckerMock.jobStartDelay.toString() mustBe s"$minutesTestVal minutes"
    }

    "overduePeriod correct" in {
      overdueSubmissionsCheckerMock.overduePeriod.getStandardMinutes mustBe minutesTestVal
    }

    "jobInterval correct" in {
      overdueSubmissionsCheckerMock.jobInterval.getStandardMinutes mustBe minutesTestVal
    }
  }

  "exceptions" should {

    "jobEnabled throw exception" in {
      val configMock: Configuration = mock[Configuration]
      when(configMock.getOptional[Boolean](contains("enabled"))(any())) thenReturn None

      the[MissingConfiguration] thrownBy new OverdueSubmissionsChecker(
        mock[MongoLockRepository],
        mock[MongoBufferService],
        configMock,
        mock[ActorSystem],
        mock[ContactFrontendConnector])(mock[ExecutionContext]) must have message "Missing configuration value overdueSubmissions.enabled"
    }

    "jobStartDelay throw exception" in {
      val configMock: Configuration = mock[Configuration]
      when(configMock.getOptional[Long](mEq("overdueSubmissions.startDelayMinutes"))(any())) thenReturn None
      when(configMock.getOptional[Boolean](contains("enabled"))(any())) thenReturn Option(false)

      the[MissingConfiguration] thrownBy new OverdueSubmissionsChecker(
        mock[MongoLockRepository],
        mock[MongoBufferService],
        configMock,
        mock[ActorSystem],
        mock[ContactFrontendConnector])(mock[ExecutionContext]) must have message "Missing configuration value overdueSubmissions.startDelayMinutes"
    }

    "overduePeriod throw exception" in {
      val configMock: Configuration = mock[Configuration]
      when(configMock.getOptional[Long](mEq("overdueSubmissions.startDelayMinutes"))(any())) thenReturn Option(
        minutesTestVal)
      when(configMock.getOptional[Long](mEq("overdueSubmissions.overduePeriodMinutes"))(any())) thenReturn None
      when(configMock.getOptional[Boolean](contains("enabled"))(any())) thenReturn Option(false)

      the[MissingConfiguration] thrownBy new OverdueSubmissionsChecker(
        mock[MongoLockRepository],
        mock[MongoBufferService],
        configMock,
        mock[ActorSystem],
        mock[ContactFrontendConnector])(mock[ExecutionContext]) must have message "Missing configuration value overdueSubmissions.overduePeriodMinutes"
    }

    "jobInterval throw exception" in {
      val configMock: Configuration = mock[Configuration]
      when(configMock.getOptional[Long](mEq("overdueSubmissions.startDelayMinutes"))(any())) thenReturn Option(
        minutesTestVal)
      when(configMock.getOptional[Long](mEq("overdueSubmissions.overduePeriodMinutes"))(any())) thenReturn Option(
        minutesTestVal)
      when(configMock.getOptional[Long](mEq("overdueSubmissions.jobIntervalMinutes"))(any())) thenReturn None
      when(configMock.getOptional[Boolean](contains("enabled"))(any())) thenReturn Option(false)

      the[MissingConfiguration] thrownBy new OverdueSubmissionsChecker(
        mock[MongoLockRepository],
        mock[MongoBufferService],
        configMock,
        mock[ActorSystem],
        mock[ContactFrontendConnector])(mock[ExecutionContext]) must have message "Missing configuration value overdueSubmissions.jobIntervalMinutes"
    }
  }
}
