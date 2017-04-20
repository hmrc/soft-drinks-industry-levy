package uk.gov.hmrc.softdrinksindustrylevy.tests

import play.api.test.WsTestClient
import uk.gov.hmrc.softdrinksindustrylevy.support.{SDILActions, IntegrationSpec}

import scala.concurrent.Await
import scala.concurrent.duration._


class MainSpec extends IntegrationSpec {


  "Visiting Google address" should "return a 200 OK response" in {
    WsTestClient.withClient { client =>
      val result = Await.result(
        new SDILActions(client).call(), 50000.milliseconds)
      result.statusText shouldBe "OK"
      result.status shouldBe 200
    }
  }

//  "BARS" should "return a 200 OK response with a valid sort code and account number" in {
//    WsTestClient.withClient { client =>
//      val result = Await.result(
//        new SDILActions(client).getBank("123456", "12345678"), 10.seconds)
//      result.statusText shouldBe "OK"
//      result.status shouldBe 200
//    }
//  }

}

