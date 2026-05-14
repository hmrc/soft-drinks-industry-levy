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

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.Logger
import play.api.http.Status
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.softdrinksindustrylevy.models.{Address, ForeignAddress, UkAddress}

import java.time.Clock
import scala.concurrent.{ExecutionContext, Future}

class ConnectorHelpersSpec extends AnyWordSpec with Matchers with MockitoSugar with ScalaFutures {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val logger: Logger = Logger(getClass)

  private val servicesConfig: ServicesConfig = mock[ServicesConfig]

  private class TestConnectorHelpers extends ConnectorHelpers(servicesConfig, Clock.systemDefaultZone()) {

    def testOutboundHeaderCarrier(hc: HeaderCarrier): HeaderCarrier =
      outboundHeaderCarrier(hc)

    def testUpstreamError(system: String, operation: String, status: Int): UpstreamErrorResponse =
      upstreamError(system, operation, status)

    def testRecover[A](operation: String, startTime: Long): PartialFunction[Throwable, Future[A]] =
      recover[A](operation, startTime)

    def testEndpointUrl(apiBaseUrl: String, path: String): String =
      endpointUrl(apiBaseUrl, path)

    def testLoggingContext(
      operation: String,
      status: Option[Int] = None,
      startTime: Option[Long] = None,
      errorClass: Option[String] = None
    ): String =
      loggingContext(operation, status, startTime, errorClass)

    def testFormatAddress(address: Address): Address =
      formatAddress(address)
  }

  private val testConnectorHelpers = new TestConnectorHelpers

  "endpointUrl" should {
    "join a base URL and path" in {
      testConnectorHelpers.testEndpointUrl("http://localhost:1111", "/soft-drinks/subscription") shouldBe
        "http://localhost:1111/soft-drinks/subscription"
    }
  }

  "loggingContext" should {
    "include operation only when no optional values are supplied" in {
      testConnectorHelpers.testLoggingContext("createSubscription") shouldBe
        "operation=createSubscription"
    }

    "include status when supplied" in {
      testConnectorHelpers.testLoggingContext("createSubscription", status = Some(Status.CREATED)) shouldBe
        "operation=createSubscription status=201"
    }

    "include duration when startTime is supplied" in {
      val startTime = System.currentTimeMillis() - 100

      val result = testConnectorHelpers.testLoggingContext("createSubscription", startTime = Some(startTime))

      result should include("operation=createSubscription")
      result should include("durationMs=")
    }

    "include error class when supplied" in {
      testConnectorHelpers.testLoggingContext("submitReturn", errorClass = Some("RuntimeException")) shouldBe
        "operation=submitReturn errorClass=RuntimeException"
    }

    "include all fields when supplied" in {
      val startTime = System.currentTimeMillis() - 100

      val result = testConnectorHelpers.testLoggingContext(
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

      val result = testConnectorHelpers.testFormatAddress(address)

      result shouldBe address.copy(
        lines = List("1 Test Street", "Test Town")
      )
    }

    "clean the lines of a foreign address" in {
      val address = ForeignAddress(
        lines = List("1 Rue Test", "Paris"),
        country = "FR"
      )

      val result = testConnectorHelpers.testFormatAddress(address)

      result shouldBe address.copy(
        lines = List("1 Rue Test", "Paris")
      )
    }
  }

  "recover" should {

    "recover an UpstreamErrorResponse as a failed Future" in {
      val exception =
        UpstreamErrorResponse(
          s"Received ${Status.INTERNAL_SERVER_ERROR} from HIP",
          Status.INTERNAL_SERVER_ERROR,
          Status.INTERNAL_SERVER_ERROR
        )

      testConnectorHelpers
        .testRecover[String]("createSubscription", System.currentTimeMillis())
        .apply(exception)
        .failed
        .map(_ shouldBe exception)
    }

    "recover a non-fatal exception as a failed Future" in {
      val exception = new RuntimeException("boom")

      testConnectorHelpers
        .testRecover[String]("submitReturn", System.currentTimeMillis())
        .apply(exception)
        .failed
        .map(_ shouldBe exception)
    }
  }
}
