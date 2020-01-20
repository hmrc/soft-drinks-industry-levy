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

package uk.gov.hmrc.softdrinksindustrylevy.connectors

import uk.gov.hmrc.http.HeaderCarrier
import scala.util.{Failure, Success, Try}

class EmailConnectorSpec extends WiremockSpec {

  object TestEmailConnector extends EmailConnector(httpClient, environment.mode, servicesConfig) {
    override val emailUrl: String = mockServerUrl
  }

  implicit val hc: HeaderCarrier = HeaderCarrier()

  "attempted email should succeed" in {
    EmailResponses.send(true)
    Try(TestEmailConnector.sendConfirmationEmail("test", "test", "dfg").futureValue) match {
      case Success(_) =>
      case Failure(_) => fail
    }
  }

  "attempted email should fail when the response from email service is a failure" in {
    EmailResponses.send(false)
    Try(TestEmailConnector.sendConfirmationEmail("test", "test", "dfg").futureValue) match {
      case Success(_) => fail
      case Failure(_) => // do nothing
    }
  }

  "attempted submission email should succeed" in {
    EmailResponses.send(true)
    Try(TestEmailConnector.sendSubmissionReceivedEmail("test", "test").futureValue) match {
      case Success(_) =>
      case Failure(_) => fail
    }
  }

  "attempted submission email should fail if email service fails" in {
    EmailResponses.send(false)
    Try(TestEmailConnector.sendSubmissionReceivedEmail("test", "test").futureValue) match {
      case Success(_) => fail
      case Failure(_) => // do nothing
    }
  }

}
