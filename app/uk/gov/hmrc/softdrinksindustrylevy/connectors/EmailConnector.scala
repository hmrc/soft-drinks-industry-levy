/*
 * Copyright 2017 HM Revenue & Customs
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

import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.softdrinksindustrylevy.config.WSHttp

import scala.concurrent.{ExecutionContext, Future}

class EmailConnector extends ServicesConfig {

  val http: WSHttp = WSHttp

  val emailUrl: String = baseUrl("email")

  def sendConfirmationEmail(email: String, sdilNumber: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {
    val params = Json.obj(
      "to" -> Seq(email),
      "templateId" -> "sdil_registration_accepted",
      "parameters" -> Json.obj(
        "sdilNumber" -> sdilNumber
      ),
      "force" -> false
    )

    http.POST[JsValue, HttpResponse](s"$emailUrl/hmrc/email", params) map { _ => () }
  }

  // TODO find out if we need to verify...
  def sendSubmissionReceivedEmail(email: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {
    val params = Json.obj(
      "to" -> Seq(email),
      "templateId" -> "sdil_registration_received",
      "force" -> false
    )

    http.POST[JsValue, HttpResponse](s"$emailUrl/hmrc/email", params) map { _ => () }
  }

}
