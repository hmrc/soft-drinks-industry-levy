/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.softdrinksindustrylevy.connectors.dms

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.http.Fault
import com.typesafe.config.ConfigFactory
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers.{AUTHORIZATION, await, defaultAwaitTimeout}
import play.api.{Application, Configuration}
import uk.gov.hmrc.http.test.WireMockSupport
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.softdrinksindustrylevy.models.dms.*
import uk.gov.hmrc.softdrinksindustrylevy.services.dms.PdfGenerationService

import java.time.format.DateTimeFormatter
import java.time.{Clock, Instant, LocalDateTime, ZoneId}

class DmsConnectorSpec extends AnyWordSpec with Matchers with WireMockSupport with GuiceOneAppPerSuite {
  private val config = Configuration(
    ConfigFactory.parseString(
      s"""
         |microservice {
         |  services {
         |        dms {
         |            port = $wireMockPort
         |        }
         |  }
         |}
         |create-internal-auth-token-on-start = false
         |""".stripMargin
    )
  )

  private val blankHtml =
    """
      |<!DOCTYPE html>
      |<html lang="en">
      |
      |<head>
      |    <title>Blah</title>
      |</head>
      |
      |<body>
      |
      |<div class="container">
      |   <p>Blah</p>
      |</div>
      |
      |</body>
      |</html>
      |""".stripMargin

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private val clock = Clock.fixed(Instant.parse("2014-12-22T10:15:30Z"), ZoneId.of("UTC"))

  private val mockPdfGenerationService = mock[PdfGenerationService]

  private val mockPdfBytes = "some pdf stuff".getBytes

  when(mockPdfGenerationService.generatePDFBytes(blankHtml)).thenReturn(mockPdfBytes)

  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .configure(config)
    .overrides(
      bind[Clock] to clock,
      bind[PdfGenerationService] to mockPdfGenerationService
    )
    .build()

  class Setup {
    val connector: DmsConnector = app.injector.instanceOf[DmsConnector]
  }

  "Dms Connector" should {
    val sdilNumber = "XZSDIL0009999"
    "return an error if the http call fails" in new Setup {
      stubFor(
        stubHttpRequest(sdilNumber).willReturn(
          aResponse().withFault(Fault.EMPTY_RESPONSE)
        )
      )

      intercept[Throwable](
        await(connector.submitToDms(blankHtml, sdilNumber)) shouldBe Left(Error(""))
      )
    }

    "return an error if the downstream returns an error" in new Setup {
      List(
        HttpResponse(400, "Received response status 400 from dms service"),
        HttpResponse(500, "Received response status 500 from dms service")
      ).foreach { httpResponse =>
        withClue(s"For http response [${httpResponse.toString}]") {
          stubFor(
            stubHttpRequest(sdilNumber).willReturn(
              aResponse().withStatus(httpResponse.status).withBody(httpResponse.body)
            )
          )

          intercept[UpstreamErrorResponse](await(connector.submitToDms(blankHtml, sdilNumber)))
        }
      }
    }

    "return an envelope id if downstream returns success" in new Setup {
      stubFor(
        stubHttpRequest(sdilNumber).willReturn(
          aResponse().withStatus(202).withBody(Json.obj("id" -> "test envelope id").toString())
        )
      )

      await(connector.submitToDms(blankHtml, sdilNumber)) shouldBe
        DmsEnvelopeId("test envelope id")
    }
  }

  private def stubHttpRequest(sdilNumber: String) =
    post(urlPathMatching("/dms-submission/submit"))
      .withHeader(AUTHORIZATION, equalTo("2c66ca76-80bc-4050-ae75-4ec16cc95aee"))
      .withMultipartRequestBody(
        aMultipart()
          .withName("callbackUrl")
          .withBody(equalTo("http://localhost:8701/soft-drinks-industry-levy/dms/callback"))
      )
      .withMultipartRequestBody(aMultipart().withName("metadata.source").withBody(equalTo("sidl")))
      .withMultipartRequestBody(
        aMultipart()
          .withName("metadata.timeOfReceipt")
          .withBody(equalTo(DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now(clock))))
      )
      .withMultipartRequestBody(
        aMultipart().withName("metadata.formId").withBody(equalTo("SDIL-VAR-1"))
      )
      .withMultipartRequestBody(
        aMultipart().withName("metadata.customerId").withBody(equalTo(sdilNumber))
      )
      .withMultipartRequestBody(
        aMultipart()
          .withName("metadata.classificationType")
          .withBody(equalTo("BT-NRU-SDIL"))
      )
      .withMultipartRequestBody(
        aMultipart()
          .withName("metadata.businessArea")
          .withBody(equalTo("BI"))
      )
      .withMultipartRequestBody(aMultipart().withName("form").withBody(binaryEqualTo(mockPdfBytes)))
}
