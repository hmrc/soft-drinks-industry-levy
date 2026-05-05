/*
 * Copyright 2026 HM Revenue & Customs
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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Logger
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.softdrinksindustrylevy.models.{ForeignAddress, UkAddress}
import uk.gov.hmrc.softdrinksindustrylevy.utils.*

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UtilsSpec extends AnyWordSpec with Matchers {

  "endpointUrl" should {
    "join a base URL and path" in {
      endpointUrl("http://localhost:1111", "/soft-drinks/subscription") shouldBe
        "http://localhost:1111/soft-drinks/subscription"
    }
  }

  "RawHttpReads" should {
    "return the raw HttpResponse unchanged" in {
      val response = HttpResponse(Status.OK, Json.obj("foo" -> "bar"), Map.empty[String, Seq[String]])

      val result = new RawHttpReads()
        .read("GET", "http://localhost/test", response)

      result shouldBe response
    }
  }

  "loggingContext" should {
    "include operation only when no optional values are supplied" in {
      loggingContext("createSubscription") shouldBe
        "operation=createSubscription"
    }

    "include status when supplied" in {
      loggingContext("createSubscription", status = Some(Status.CREATED)) shouldBe
        "operation=createSubscription status=201"
    }

    "include duration when startTime is supplied" in {
      val startTime = System.currentTimeMillis() - 100

      val result = loggingContext("createSubscription", startTime = Some(startTime))

      result should include("operation=createSubscription")
      result should include("durationMs=")
    }

    "include error class when supplied" in {
      loggingContext("submitReturn", errorClass = Some("RuntimeException")) shouldBe
        "operation=submitReturn errorClass=RuntimeException"
    }

    "include all fields when supplied" in {
      val startTime = System.currentTimeMillis() - 100

      val result = loggingContext(
        operation = "retrieveSubscriptionDetails",
        status = Some(Status.INTERNAL_SERVER_ERROR),
        startTime = Some(startTime),
        errorClass = Some("UpstreamErrorResponse")
      )

      result should include("operation=retrieveSubscriptionDetails")
      result should include("status=500")
      result should include("durationMs=")
      result should include("errorClass=UpstreamErrorResponse")
    }
  }

  "formatAddress" should {
    "clean the lines of a UK address" in {
      val address = UkAddress(
        lines = List("1 Test Street", "Test Town"),
        postCode = "AA1 1AA"
      )

      val result = formatAddress(address)

      result shouldBe address.copy(
        lines = List("1 Test Street", "Test Town")
      )
    }

    "clean the lines of a foreign address" in {
      val address = ForeignAddress(
        lines = List("1 Rue Test", "Paris"),
        country = "FR"
      )

      val result = formatAddress(address)

      result shouldBe address.copy(
        lines = List("1 Rue Test", "Paris")
      )
    }
  }

  "recover" should {
    implicit val logger: Logger = Logger(this.getClass)

    "recover an UpstreamErrorResponse as a failed Future" in {
      val exception =
        UpstreamErrorResponse(
          s"Received ${Status.INTERNAL_SERVER_ERROR} from HIP",
          Status.INTERNAL_SERVER_ERROR,
          Status.INTERNAL_SERVER_ERROR
        )

      recover[String]("HIP", "createSubscription", System.currentTimeMillis())
        .apply(exception)
        .failed
        .map(_ shouldBe exception)
    }

    "recover a non-fatal exception as a failed Future" in {
      val exception = new RuntimeException("boom")

      recover[String]("DES", "submitReturn", System.currentTimeMillis())
        .apply(exception)
        .failed
        .map(_ shouldBe exception)
    }
  }
}
