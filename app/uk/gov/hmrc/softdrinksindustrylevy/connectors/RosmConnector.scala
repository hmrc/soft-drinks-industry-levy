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

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.softdrinksindustrylevy.config.WSHttp
import uk.gov.hmrc.softdrinksindustrylevy.models._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RosmConnector extends ServicesConfig {

  val desURL: String = baseUrl("des")
  val serviceURL: String = "registration/organisation"
  val http = WSHttp

  def addHeaders(implicit hc: HeaderCarrier): HeaderCarrier = {
    hc.withExtraHeaders(
      "Environment" -> getConfString("des.environment", "")
    ).copy(authorization = Some(Authorization(s"Bearer ${getConfString("des.token", "")}")))
  }

  def retrieveROSMDetails(utr: String, request: RosmRegisterRequest)
                         (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[RosmRegisterResponse]] ={
    http.POST[RosmRegisterRequest, Option[RosmRegisterResponse]](s"$desURL/$serviceURL/utr/$utr", request)(implicitly, implicitly, addHeaders, implicitly)
  }

}
