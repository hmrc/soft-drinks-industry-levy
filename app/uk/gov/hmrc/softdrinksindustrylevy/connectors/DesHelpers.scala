/*
 * Copyright 2020 HM Revenue & Customs
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

import play.api.libs.json.Writes
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

abstract class DesHelpers(servicesConfig: ServicesConfig) {

  val http: HttpClient

  def desPost[I, O](
    url: String,
    body: I)(implicit wts: Writes[I], rds: HttpReads[O], hc: HeaderCarrier, ec: ExecutionContext): Future[O] =
    http.POST[I, O](url, body)(wts, rds, addHeaders, ec)

  def addHeaders(implicit hc: HeaderCarrier): HeaderCarrier =
    hc.withExtraHeaders(
        "Environment" -> servicesConfig.getConfString("des.environment", "")
      )
      .copy(authorization = Some(Authorization(s"Bearer ${servicesConfig.getConfString("des.token", "")}")))
}
