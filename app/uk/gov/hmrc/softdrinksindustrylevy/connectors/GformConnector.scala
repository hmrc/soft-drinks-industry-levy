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

import java.util.Base64

import play.api.Mode
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

class GformConnector(http: HttpClient, val mode: Mode, servicesConfig: ServicesConfig) {

  val gformUrl: String = servicesConfig.baseUrl("gform")

  def submitToDms(html: String, sdilNumber: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {
    val payload = DmsHtmlSubmission(encode(html), DmsMetadata("SDIL-VAR-1", sdilNumber, "BT-NRU-SDIL", "BT"))

    http.POST[DmsHtmlSubmission, HttpResponse](s"$gformUrl/gform/dms/submit", payload) map { _ =>
      ()
    }
  }

  private val encode = (s: String) => new String(Base64.getEncoder.encode(s.getBytes))
}

case class DmsHtmlSubmission(html: String, metadata: DmsMetadata)

object DmsHtmlSubmission {
  implicit val format: Format[DmsHtmlSubmission] = Json.format[DmsHtmlSubmission]
}

case class DmsMetadata(dmsFormId: String, customerId: String, classificationType: String, businessArea: String)

object DmsMetadata {
  implicit val format: Format[DmsMetadata] = Json.format[DmsMetadata]
}
