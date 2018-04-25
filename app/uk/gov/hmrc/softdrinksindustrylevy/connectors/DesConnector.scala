/*
 * Copyright 2018 HM Revenue & Customs
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

import play.api.Configuration
import play.api.Mode.Mode
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.softdrinksindustrylevy.models._
import uk.gov.hmrc.softdrinksindustrylevy.models.json.des.returns._
import uk.gov.hmrc.softdrinksindustrylevy.services.JsonSchemaChecker

import scala.concurrent.{ExecutionContext, Future}

class DesConnector(val http: HttpClient,
                   val mode: Mode,
                   val runModeConfiguration: Configuration)
  extends ServicesConfig with OptionHttpReads with DesHelpers {

  val desURL: String = baseUrl("des")
  val serviceURL: String = "soft-drinks"

  // DES return 503 in the event of no subscription for the UTR, we are expected to treat as 404, hence this override
  implicit override def readOptionOf[P](implicit rds: HttpReads[P]): HttpReads[Option[P]] = new HttpReads[Option[P]] {
    def read(method: String, url: String, response: HttpResponse): Option[P] = response.status match {
      case 204 | 404 | 503 => None
      case _ => Some(rds.read(method, url, response))
    }
  }

  def createSubscription(request: Subscription, idType: String, idNumber: String)
                        (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[CreateSubscriptionResponse] = {
    import json.des.create._
    import uk.gov.hmrc.softdrinksindustrylevy.models.RosmResponseAddress._
    val formattedLines = request.address.lines.map { line => line.clean }
    val formattedAddress = request.address match {
      case a: UkAddress => a.copy(lines = formattedLines)
      case b: ForeignAddress => b.copy(lines = formattedLines)
    }
    val submission = request.copy(address = formattedAddress)

    JsonSchemaChecker[Subscription](request, "des-create-subscription")
    desPost[Subscription, CreateSubscriptionResponse](s"$desURL/$serviceURL/subscription/$idType/$idNumber", submission)
  }

  def retrieveSubscriptionDetails(idType: String, idNumber: String)
                                 (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Subscription]] = {
    import json.des.get._
    http.GET[Option[Subscription]](s"$desURL/$serviceURL/subscription/details/$idType/$idNumber")(implicitly, addHeaders, ec)
  }

  def submitReturn(sdilRef: String, returnsRequest: ReturnsRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    desPost[ReturnsRequest, HttpResponse](s"$desURL/$serviceURL/$sdilRef/return", returnsRequest)
  }

}
