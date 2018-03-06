package uk.gov.hmrc.softdrinksindustrylevy.connectors
import java.util.Base64

import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.http.HeaderCarrier
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}

class GformConnectorSpec extends WiremockSpec with FutureAwaits with DefaultAwaitTimeout {

  "Submitting a html to gform" should {
    "base64 encode the html" in {
      val rawHtml = "<p>totally a variation</p>"
      val encodedHtml = new String(Base64.getEncoder.encode(rawHtml.getBytes))

      stubFor(
        post("/gform/dms/submit")
          .willReturn(aResponse().withStatus(204))
      )

      await(testConnector.submitToDms(rawHtml, "totally an sdil number"))

      verify(
        postRequestedFor(urlEqualTo("/gform/dms/submit"))
          .withRequestBody(containing(s""""html":"$encodedHtml""""))
      )
    }

    "send the correct metadata to gform" in {
      stubFor(
        post("/gform/dms/submit")
          .willReturn(aResponse().withStatus(204))
      )

      val sdilNumber = "XZSDIL0009999"
      val expectedMetadataJson = """{"dmsFormId":"SDIL-VAR-1","customerId":"XZSDIL0009999","classificationType":"BT-NRU-SDIL","businessArea":"BT"}"""

      await(testConnector.submitToDms("", sdilNumber))

      verify(
        postRequestedFor(urlEqualTo("/gform/dms/submit"))
          .withRequestBody(containing(s""""metadata":$expectedMetadataJson"""))
      )
    }
  }

  lazy val testConnector = new GformConnector {
    override val gformUrl = mockServerUrl
  }

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()
}
