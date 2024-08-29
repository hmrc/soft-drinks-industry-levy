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

import com.google.inject.{Inject, Singleton}
import play.api.Mode
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailConnector @Inject() (http: HttpClientV2, val mode: Mode, servicesConfig: ServicesConfig) {

  val emailBaseUrl: String = servicesConfig.baseUrl("email")

  def sendConfirmationEmail(orgName: String, email: String, sdilNumber: String)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Unit] = {
    val params = Json.obj(
      "to"         -> Seq(email),
      "templateId" -> "sdil_registration_accepted",
      "parameters" -> Json.obj(
        "sdilNumber"  -> sdilNumber,
        "companyName" -> orgName
      ),
      "force" -> false
    )

    val emailUrl = s"$emailBaseUrl/hmrc/email"

    http
      .post(url"$emailUrl")
      .withBody(params)
      .execute[HttpResponse]
      .map(_ => ())
  }

  // TODO find out if we need to verify...
  def sendSubmissionReceivedEmail(email: String, orgName: String)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Unit] = {
    val params = Json.obj(
      "to"         -> Seq(email),
      "templateId" -> "sdil_registration_received",
      "parameters" -> Json.obj(
        "companyName" -> orgName
      ),
      "force" -> false
    )
    val emailUrl = s"$emailBaseUrl/hmrc/email"

    http
      .post(url"$emailUrl")
      .withBody(params)
      .execute[HttpResponse]
      .map(_ => ())
  }
}
