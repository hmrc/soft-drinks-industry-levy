package uk.gov.hmrc.softdrinksindustrylevy.connectors

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.play.it.Port

trait WiremockSpec extends PlaySpec with BeforeAndAfterAll with BeforeAndAfterEach with GuiceOneAppPerSuite {
  val port = Port.randomAvailable
  val mockServer = new WireMockServer(port)

  val mockServerUrl = s"http://localhost:$port"

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    mockServer.start()
    WireMock.configureFor("localhost", port)
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    WireMock.reset()
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    mockServer.stop()
  }
}
