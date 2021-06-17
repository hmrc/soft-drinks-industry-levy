/*
 * Copyright 2021 HM Revenue & Customs
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
import uk.gov.hmrc.http.Authorization
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

abstract class DesHelpers(servicesConfig: ServicesConfig) {

  val http: HttpClient

  val serviceKey: String = s"Bearer ${servicesConfig.getConfString("des.token", "")}"
  val serviceEnvironment: String = servicesConfig.getConfString("des.environment", "")

  private def desHeaders = Seq("Environment" -> serviceEnvironment, "Authorization" -> serviceKey)

  def desPost[I, O](
    url: String,
    body: I)(implicit wts: Writes[I], rds: HttpReads[O], hc: HeaderCarrier, ec: ExecutionContext): Future[O] =
    http.POST[I, O](url, body, headers = desHeaders)(wts, rds, addHeaders, ec)

  def addHeaders(implicit hc: HeaderCarrier): HeaderCarrier =
    hc.withExtraHeaders()
}
