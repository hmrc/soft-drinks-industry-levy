package uk.gov.hmrc.softdrinksindustrylevy.support

import javax.inject.Inject

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.Future

class SDILActions @Inject()(ws: WSClient) {

  def call(): Future[WSResponse] = {
    ws.url("http://www.google.com").get().map { response =>
      val statusText: String = response.statusText
      val statusCode: Int = response.status
      println(s"Got a response $statusText")
      println(s"Got a response $statusCode")
      response
    }
  }

  def getBank(sortcode: String, accountnumber: String): Future[WSResponse] = {
    ws.url(s"http://localhost:9854/direct-debit/bank?sortCode=$sortcode&accountNumber=$accountnumber").get().map { response =>
      val statusText: String = response.statusText
      val statusCode: Int = response.status
      println(s"Got a response $statusText")
      println(s"Got a response $statusCode")
      response
    }
  }
}
