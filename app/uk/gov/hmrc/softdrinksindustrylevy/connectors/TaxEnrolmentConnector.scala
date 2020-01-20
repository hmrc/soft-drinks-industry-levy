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

import play.api.Logger
import play.api.Mode
import play.api.libs.json.{Format, JsObject, Json}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

class TaxEnrolmentConnector(http: HttpClient, val mode: Mode, servicesConfig: ServicesConfig) {

  val callbackUrl: String = servicesConfig.getConfString("tax-enrolments.callback", "")
  val serviceName: String = servicesConfig.getConfString("tax-enrolments.serviceName", "")
  lazy val taxEnrolmentsUrl: String = servicesConfig.baseUrl("tax-enrolments")

  def subscribe(safeId: String, formBundleNumber: String)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext): Future[HttpResponse] =
    http.PUT[JsObject, HttpResponse](subscribeUrl(formBundleNumber), requestBody(safeId, formBundleNumber)) map {
      Result =>
        Result
    } recover {
      case e: UnauthorizedException => handleError(e, formBundleNumber)
      case e: BadRequestException   => handleError(e, formBundleNumber)
    }

  def getSubscription(
    subscriptionId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[TaxEnrolmentsSubscription] =
    http.GET[TaxEnrolmentsSubscription](s"$taxEnrolmentsUrl/tax-enrolments/subscriptions/$subscriptionId")

  private def handleError(e: HttpException, formBundleNumber: String): HttpResponse = {
    Logger.error(s"Tax enrolment returned $e for ${subscribeUrl(formBundleNumber)}")
    HttpResponse(e.responseCode, Some(Json.toJson(e.message)))
  }

  private def subscribeUrl(subscriptionId: String) =
    s"$taxEnrolmentsUrl/tax-enrolments/subscriptions/$subscriptionId/subscriber"

  private def requestBody(safeId: String, formBundleNumber: String): JsObject =
    Json.obj(
      "serviceName" -> serviceName,
      "callback"    -> s"$callbackUrl?subscriptionId=$formBundleNumber",
      "etmpId"      -> safeId
    )

}

case class TaxEnrolmentsSubscription(
  identifiers: Option[Seq[Identifier]],
  etmpId: String,
  state: String,
  errorResponse: Option[String])

object TaxEnrolmentsSubscription {
  implicit val format: Format[TaxEnrolmentsSubscription] = Json.format[TaxEnrolmentsSubscription]
}

case class Identifier(key: String, value: String)

object Identifier {
  implicit val format: Format[Identifier] = Json.format[Identifier]
}
