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

package uk.gov.hmrc.softdrinksindustrylevy.connectors

import play.api.http.Status
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.{BadGatewayException, HeaderCarrier}
import uk.gov.hmrc.softdrinksindustrylevy.models.connectors.*
import uk.gov.hmrc.softdrinksindustrylevy.util.WireMockMethods

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class ContactFrontendConnectorSpec extends HttpClientV2Helper with WireMockMethods {

  val connector: ContactFrontendConnector = app.injector.instanceOf[ContactFrontendConnector]

  implicit val hc: HeaderCarrier = HeaderCarrier()

  implicit lazy val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  "attempted contact form should fail if contact service is not available" in {

    stopWireMock()

    intercept[BadGatewayException] {
      await {
        connector.raiseTicket(sub, "safeid1")
      }
    }
    startWireMock()
  }

  "attempted contact form should succeed if contact service is available" in {
    when(POST, "/contact/contact-hmrc/form")
      .thenReturn(Status.OK)

    connector.raiseTicket(sub, "test1") onComplete {
      case Success(_) =>
      case Failure(_) => fail()
    }
  }

}
