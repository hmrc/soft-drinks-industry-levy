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

import javax.inject.Singleton

import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.softdrinksindustrylevy.config.WSHttp
import uk.gov.hmrc.softdrinksindustrylevy.models._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DesConnector extends ServicesConfig {

  val desURL: String = baseUrl("des")
  val serviceURL: String = "soft-drinks"
  val http = WSHttp

  def addHeaders(implicit hc: HeaderCarrier): HeaderCarrier = {
    hc.withExtraHeaders(
      "Environment" -> getConfString("des.environment", "")
    ).copy(authorization = Some(Authorization(s"Bearer ${getConfString("des.token", "")}")))
  }

  def createSubscription(request: Subscription, idType: String, idNumber: String)
                        (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[CreateSubscriptionResponse] = {
    import json.des.create._

    http.POST[Subscription, CreateSubscriptionResponse](s"$desURL/$serviceURL/subscription/$idType/$idNumber", request)(implicitly, implicitly, addHeaders, implicitly)
  }

  def retrieveSubscriptionDetails(idType: String, idNumber: String)
                                 (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Subscription]] = {
    import json.des.get._

    http.GET[Option[Subscription]](s"$desURL/$serviceURL/subscription/details/$idType/$idNumber")(implicitly, addHeaders, ec)
  }
}
