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

import akka.actor.ActorSystem
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.softdrinksindustrylevy.util.FakeApplicationSpec
import play.api.Configuration
import uk.gov.hmrc.mongo.MongoConnector
import uk.gov.hmrc.softdrinksindustrylevy.connectors.ContactFrontendConnector
import org.mockito.Mockito.when
import org.mockito.ArgumentMatchers.any
import uk.gov.hmrc.softdrinksindustrylevy.models.Subscription

import scala.concurrent.{ExecutionContext, Future}

class OverdueSubmissionsCheckerSpec extends FakeApplicationSpec with MockitoSugar {
  val minutesTestVal: Long = 59

  "vals" should {
    val configMock: Configuration = mock[Configuration]
    when(configMock.getLong(any[String])) thenReturn Option(minutesTestVal)
    when(configMock.getBoolean(any[String])) thenReturn Option(false)
    val overdueSubmissionsCheckerMock = new OverdueSubmissionsChecker(
      configMock,
      mock[MongoConnector],
      mock[ActorSystem],
      mock[MongoBufferService],
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
      when(configMock.getBoolean(any[String])) thenReturn None

      the[MissingConfiguration] thrownBy new OverdueSubmissionsChecker(
        configMock,
        mock[MongoConnector],
        mock[ActorSystem],
        mock[MongoBufferService],
        mock[ContactFrontendConnector])(mock[ExecutionContext]) must have message "Missing configuration value overdueSubmissions.enabled"
    }

    "jobStartDelay throw exception" in {
      val configMock: Configuration = mock[Configuration]
      when(configMock.getLong("overdueSubmissions.startDelayMinutes")) thenReturn None
      when(configMock.getBoolean(any[String])) thenReturn Option(false)

      the[MissingConfiguration] thrownBy new OverdueSubmissionsChecker(
        configMock,
        mock[MongoConnector],
        mock[ActorSystem],
        mock[MongoBufferService],
        mock[ContactFrontendConnector])(mock[ExecutionContext]) must have message "Missing configuration value overdueSubmissions.startDelayMinutes"
    }

    "overduePeriod throw exception" in {
      val configMock: Configuration = mock[Configuration]
      when(configMock.getLong("overdueSubmissions.startDelayMinutes")) thenReturn Option(minutesTestVal)
      when(configMock.getLong("overdueSubmissions.overduePeriodMinutes")) thenReturn None
      when(configMock.getBoolean(any[String])) thenReturn Option(false)

      the[MissingConfiguration] thrownBy new OverdueSubmissionsChecker(
        configMock,
        mock[MongoConnector],
        mock[ActorSystem],
        mock[MongoBufferService],
        mock[ContactFrontendConnector])(mock[ExecutionContext]) must have message "Missing configuration value overdueSubmissions.overduePeriodMinutes"
    }

    "jobInterval throw exception" in {
      val configMock: Configuration = mock[Configuration]
      when(configMock.getLong("overdueSubmissions.startDelayMinutes")) thenReturn Option(minutesTestVal)
      when(configMock.getLong("overdueSubmissions.overduePeriodMinutes")) thenReturn Option(minutesTestVal)
      when(configMock.getLong("overdueSubmissions.jobIntervalMinutes")) thenReturn None
      when(configMock.getBoolean(any[String])) thenReturn Option(false)

      the[MissingConfiguration] thrownBy new OverdueSubmissionsChecker(
        configMock,
        mock[MongoConnector],
        mock[ActorSystem],
        mock[MongoBufferService],
        mock[ContactFrontendConnector])(mock[ExecutionContext]) must have message "Missing configuration value overdueSubmissions.jobIntervalMinutes"
    }
  }
}
