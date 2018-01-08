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

import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.softdrinksindustrylevy.config.WSHttp

import scala.concurrent.{ExecutionContext, Future}

class DeskproConnector extends ServicesConfig {
  val http: WSHttp = WSHttp

  lazy val deskproUrl: String = baseUrl("hmrc-deskpro")

  def raiseTicket(subject: String, message: String, email: String, utr: String)
                 (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {
    val ticket = Json.obj(
      "name" -> "Soft Drinks Industry Levy",
      "email" -> email,
      "subject" -> subject,
      "message" -> message,
      "referrer" -> "N/A",
      "javascriptEnabled" -> "false",
      "userAgent" -> "soft-drinks-industry-levy",
      "authId" -> "N/A",
      "areaOfTax" -> "sdil",
      "sessionId" -> "N/A",
      "userTaxIdentifiers" -> Json.obj(
        "utr" -> utr
      )
    )

    http.POST[JsValue, HttpResponse](s"$deskproUrl/deskpro/ticket", ticket) map { _ => () }
  }
}
