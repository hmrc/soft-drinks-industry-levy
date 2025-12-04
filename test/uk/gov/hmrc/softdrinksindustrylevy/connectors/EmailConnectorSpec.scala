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

import org.scalatest.matchers.should.Matchers.shouldBe
import play.api.http.Status
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.softdrinksindustrylevy.util.WireMockMethods

import scala.concurrent.ExecutionContext

class EmailConnectorSpec extends HttpClientV2Helper with WireMockMethods {

  val connector: EmailConnector = app.injector.instanceOf[EmailConnector]
  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit lazy val executionContext: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  "attempted email should succeed" in {
    when(POST, "/hmrc/email")
      .thenReturn(Status.OK)

    await(connector.sendConfirmationEmail("test", "test", "dfg")) shouldBe ()
  }

  "attempted email should fail when the response from email service is a failure" in {
    when(POST, "/hmrc/email")
      .thenReturn(Status.INTERNAL_SERVER_ERROR)
    await(connector.sendConfirmationEmail("test", "test", "dfg")) shouldBe ()
  }

  "attempted submission email should succeed" in {
    when(POST, "/hmrc/email")
      .thenReturn(Status.OK)
    await(connector.sendSubmissionReceivedEmail("test", "test")) shouldBe ()
  }

  "attempted submission email should fail if email service fails" in {
    when(POST, "/hmrc/email")
      .thenReturn(Status.INTERNAL_SERVER_ERROR)
    await(connector.sendSubmissionReceivedEmail("test", "test")) shouldBe ()
  }

}
