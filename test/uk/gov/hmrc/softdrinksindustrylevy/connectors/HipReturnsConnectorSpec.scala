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

import com.github.tomakehurst.wiremock.client.WireMock.*
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.*
import play.api.libs.json.*
import sdil.models.ReturnPeriod
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.softdrinksindustrylevy.models.*
import uk.gov.hmrc.softdrinksindustrylevy.util.{FakeApplicationSpec, WireMockMethods}

import java.time.*
import java.time.format.DateTimeFormatter
import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.language.postfixOps

class HipReturnsConnectorSpec
    extends FakeApplicationSpec with MockitoSugar with HttpClientV2Helper with WireMockMethods {

  private val CORRELATION_ID_KEY: String = "correlationid"
  private val CORRELATION_ID_VALUE: String = UUID.randomUUID().toString()

  private val X_ORIGINATING_SYSTEM_KEY: String = "X-Originating-System"
  private val X_ORIGINATING_SYSTEM_VALUE: String = "SDIL"

  private val X_RECEIPT_DATE_KEY: String = "X-Receipt-Date"
  private val X_RECEIPT_DATE_VALUE: String =
    DateTimeFormatter.ISO_INSTANT.format(Instant.now(Clock.systemDefaultZone()))

  private val X_TRANSMITTING_SYSTEM_KEY: String = "X-Transmitting-System"
  private val X_TRANSMITTING_SYSTEM_VALUE: String = "HIP"

  val sdilConnector: SdilConnector = app.injector.instanceOf[SdilConnector]

  implicit val hc: HeaderCarrier = new HeaderCarrier
  implicit lazy val executionContext: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  private val TWENTY_TWENTY_FOUR = 2024
  private val FIRST_QUARTER = 1
  implicit val period: ReturnPeriod = ReturnPeriod(TWENTY_TWENTY_FOUR, FIRST_QUARTER)

  val expectedHeaders: Seq[(String, String)] =
    Seq(
      CORRELATION_ID_KEY        -> CORRELATION_ID_VALUE,
      X_ORIGINATING_SYSTEM_KEY  -> X_ORIGINATING_SYSTEM_VALUE,
      X_RECEIPT_DATE_KEY        -> X_RECEIPT_DATE_VALUE,
      X_TRANSMITTING_SYSTEM_KEY -> X_TRANSMITTING_SYSTEM_VALUE
    )

  "HipReturnsConnector" should {

    val exportedLitreBand: (Litres, Litres) = (109L, 110L)
    val wastedLitreBand: (Litres, Litres) = (111L, 112L)
    val returnsImporting = ReturnsImporting((111L, 112L), (111L, 112L))

    val returnsRequest = new ReturnsRequest(
      packaged = None,
      imported = Some(returnsImporting),
      exported = Some(exportedLitreBand),
      wastage = Some(wastedLitreBand)
    )

    val returnResponseByStatus: Map[Int, JsObject] = Map(
      CREATED -> Json.obj(
        "success" -> Json.obj(
          "formBundleNumber" -> "123456789019"
        )
      ),
      INTERNAL_SERVER_ERROR -> Json.obj(
        "error" -> Json.obj(
          "code"    -> "500",
          "message" -> "Just a simple error message"
        )
      )
    )

    "submit a return and return the raw HttpResponse" in {

      stubFor(
        post(urlEqualTo("/soft-drinks/XKSDIL000000022/return"))
          .withHeader(
            CORRELATION_ID_KEY,
            matching(
              "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
            )
          )
          .withHeader(X_ORIGINATING_SYSTEM_KEY, equalTo("SDIL"))
          .withHeader(X_RECEIPT_DATE_KEY, matching("""\d{4}-\d{2}-\d{2}T.*Z"""))
          .withHeader(X_TRANSMITTING_SYSTEM_KEY, equalTo("HIP"))
          .willReturn(
            aResponse()
              .withStatus(CREATED)
              .withBody(returnResponseByStatus(CREATED).toString)
          )
      )

      val result =
        sdilConnector.submitReturn("XKSDIL000000022", returnsRequest).futureValue

      result.status shouldBe CREATED
      result.json shouldBe returnResponseByStatus(CREATED)
    }

    "propagate failure from submitReturn" in {

      stubFor(
        post(urlEqualTo("/soft-drinks/XKSDIL000000022/return"))
          .withHeader(
            CORRELATION_ID_KEY,
            matching(
              "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
            )
          )
          .withHeader(X_ORIGINATING_SYSTEM_KEY, equalTo("SDIL"))
          .withHeader(X_RECEIPT_DATE_KEY, matching("""\d{4}-\d{2}-\d{2}T.*Z"""))
          .withHeader(X_TRANSMITTING_SYSTEM_KEY, equalTo("HIP"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
              .withBody(returnResponseByStatus(INTERNAL_SERVER_ERROR).toString)
          )
      )

      val result =
        sdilConnector.submitReturn("XKSDIL000000022", returnsRequest).futureValue

      result.status shouldBe INTERNAL_SERVER_ERROR
      result.json shouldBe returnResponseByStatus(INTERNAL_SERVER_ERROR)
    }
  }
}
