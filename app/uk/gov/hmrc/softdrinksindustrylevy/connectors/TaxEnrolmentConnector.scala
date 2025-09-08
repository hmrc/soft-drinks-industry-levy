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
import play.api.{Logger, Mode}
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.softdrinksindustrylevy.models.TaxEnrolments
import uk.gov.hmrc.softdrinksindustrylevy.models.TaxEnrolments.TaxEnrolmentsSubscription

@Singleton
class TaxEnrolmentConnector @Inject() (http: HttpClientV2, val mode: Mode, servicesConfig: ServicesConfig) {

  val logger: Logger = Logger(this.getClass)
  val callbackUrl: String = servicesConfig.getConfString("tax-enrolments.callback", "")
  val serviceName: String = servicesConfig.getConfString("tax-enrolments.serviceName", "")
  lazy val taxEnrolmentsBaseUrl: String = servicesConfig.baseUrl("tax-enrolments")

  def subscribe(safeId: String, formBundleNumber: String)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[HttpResponse] =
    http
      .put(url"${subscribeUrl(formBundleNumber)}")
      .withBody(Json.toJson(requestBody(safeId, formBundleNumber)))
      .execute[HttpResponse]
      .recover {
        case e: UnauthorizedException => handleError(e, formBundleNumber)
        case e: BadRequestException   => handleError(e, formBundleNumber)
      }

  def getSubscription(
    subscriptionId: String
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[TaxEnrolmentsSubscription] = {
    val subscriptionUrl = s"$taxEnrolmentsBaseUrl/tax-enrolments/subscriptions/$subscriptionId"
    http
      .get(url"$subscriptionUrl")
      .execute[TaxEnrolmentsSubscription]
  }

  private def handleError(e: HttpException, formBundleNumber: String): HttpResponse = {
    logger.error(s"Tax enrolment returned $e for ${subscribeUrl(formBundleNumber)}")
    HttpResponse(e.responseCode, Json.toJson(e.message), headers = Map.empty[String, Seq[String]])
  }

  private def subscribeUrl(subscriptionId: String) =
    s"$taxEnrolmentsBaseUrl/tax-enrolments/subscriptions/$subscriptionId/subscriber"

  private def requestBody(safeId: String, formBundleNumber: String): JsObject =
    Json.obj(
      "serviceName" -> serviceName,
      "callback"    -> s"$callbackUrl?subscriptionId=$formBundleNumber",
      "etmpId"      -> safeId
    )

}
