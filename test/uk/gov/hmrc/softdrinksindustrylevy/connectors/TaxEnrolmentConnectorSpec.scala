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
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.softdrinksindustrylevy.models.TaxEnrolments.TaxEnrolmentsSubscription
import uk.gov.hmrc.softdrinksindustrylevy.util.WireMockMethods

import scala.concurrent.ExecutionContext

class TaxEnrolmentConnectorSpec extends HttpClientV2Helper with WireMockMethods {

  val connector: TaxEnrolmentConnector = app.injector.instanceOf[TaxEnrolmentConnector]

  val req = TaxEnrolmentsSubscription(None, "etmp1", "active", None)
  implicit lazy val hc: HeaderCarrier = new HeaderCarrier

  implicit lazy val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  "should get successful response back" in {

    when(GET, "/tax-enrolments/subscriptions/1234567890")
      .thenReturn(Status.OK, Json.toJson(req).toString())

    await(connector.getSubscription("1234567890")) shouldBe req
  }

  "should subscribe successfully" in {

    when(PUT, "/tax-enrolments/subscriptions/1234/subscriber")
      .thenReturn(Status.OK, Json.toJson(req).toString())

    val response = await(connector.subscribe("safe1", "1234"))
    response.status shouldBe Status.OK
  }

  "should handle errors for create subscribtion" in {
    when(PUT, "/tax-enrolments/subscriptions/1234/subscriber")
      .thenReturn(Status.BAD_REQUEST)

    await(connector.subscribe("safe1", "1234")).status shouldBe Status.BAD_REQUEST
  }
  "should handle unauthorized error for subscription" in {
    when(PUT, "/tax-enrolments/subscriptions/1234/subscriber")
      .thenReturn(Status.UNAUTHORIZED)

    await(connector.subscribe("safe1", "1234")).status shouldBe Status.UNAUTHORIZED
  }

  "should handle errors for get subscription" in {
    when(PUT, "/tax-enrolments/subscriptions/1234/subscriber")
      .thenReturn(Status.NOT_FOUND)

    await(connector.subscribe("safe1", "1234")).status shouldBe Status.NOT_FOUND
  }
}
