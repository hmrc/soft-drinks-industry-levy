package uk.gov.hmrc.softdrinksindustrylevy.controllers

import org.mockito.ArgumentMatchers.any
import com.softwaremill.macwire.wire
import org.mockito.Mockito.{reset, times, verify, when}
import play.api.test.Helpers._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.softdrinksindustrylevy.config.SdilComponents
import uk.gov.hmrc.softdrinksindustrylevy.connectors.DesConnector
import uk.gov.hmrc.softdrinksindustrylevy.models.DisplayDirectDebitResponse
import uk.gov.hmrc.softdrinksindustrylevy.util.FakeApplicationSpec
import uk.gov.hmrc.http.NotFoundException

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DirectDebitControllerSpec extends FakeApplicationSpec with MockitoSugar with BeforeAndAfterEach with ScalaFutures{

  val mockDesConnector: DesConnector = mock[DesConnector]
  lazy val cc = new SdilComponents(context).cc
  val testDirectDebitController = wire[DirectDebitController]


  implicit val hc: HeaderCarrier = new HeaderCarrier


  override def beforeEach() {
    reset(mockDesConnector)
  }

  "DirectDebitController" should {
    "return an OK with a DirectDebitResponse" in {
      when(mockDesConnector.displayDirectDebit(any())(any())).thenReturn(Future.successful(DisplayDirectDebitResponse(true)))

      val response = testDirectDebitController.checkDirectDebitStatus("XMSDIL000000001")(FakeRequest())

      status(response) mustBe OK
      verify(mockDesConnector, times(1)).displayDirectDebit(any())(any())
      contentAsJson(response) mustBe Json.parse(
        """{
          |   "directDebitMandateFound" : true
          |}""".stripMargin)
    }

    "return an 404 when des returns a NotFoundException " in {

      val mockedException = Future.failed(new NotFoundException(""))

      when(mockDesConnector.displayDirectDebit(any())(any())).thenReturn(mockedException)

      val exception = the[Exception] thrownBy testDirectDebitController.checkDirectDebitStatus("XMSDIL000000001")(FakeRequest()).futureValue

      exception mustBe a[NotFoundException]
      verify(mockDesConnector, times(1)).displayDirectDebit(any())(any())
    }

  }

}
