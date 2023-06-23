/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.{Logger, Mode}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.softdrinksindustrylevy.models._
import uk.gov.hmrc.http.HttpReads.Implicits._

import scala.concurrent.{ExecutionContext, Future}
import com.google.inject.{Inject, Singleton}

@Singleton
class RosmConnector @Inject()(val http: HttpClient, val mode: Mode, servicesConfig: ServicesConfig)
    extends DesHelpers(servicesConfig) {
  lazy val logger = Logger(this.getClass)
  val desURL: String = servicesConfig.baseUrl("des")
  val serviceURL: String = "registration/organisation"

  def retrieveROSMDetails(utr: String, request: RosmRegisterRequest)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext): Future[Option[RosmRegisterResponse]] =
    http
      .POST[RosmRegisterRequest, Option[RosmRegisterResponse]](
        s"$desURL/$serviceURL/utr/$utr",
        request,
        headers = desHeaders)
      .recover {
        case UpstreamErrorResponse(_, 429, _, _) =>
          logger.error("[RATE LIMITED] Received 429 from DES - converting to 503")
          throw UpstreamErrorResponse("429 received from DES - converted to 503", 503, 503)
      }
}
