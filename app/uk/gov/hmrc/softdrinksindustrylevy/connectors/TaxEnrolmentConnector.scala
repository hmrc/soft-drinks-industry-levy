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

import javax.inject.Singleton

import play.api.libs.json.{JsObject, JsString, Json}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.softdrinksindustrylevy.config.WSHttp

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaxEnrolmentConnector extends ServicesConfig {

  val callbackUrl: String = getConfString("tax-enrolments.callback", "")
  val serviceName: String = getConfString("tax-enrolments.serviceName", "")

  val http: WSHttp.type = WSHttp

  def subscribe(safeId: String, formBundleNumber: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[JsObject]] = {
    http.PUT[JsObject, Option[JsObject]](subscribeUrl(formBundleNumber), requestBody(safeId))
  }

  private def subscribeUrl(subscriptionId: String) =
    s"${baseUrl("tax-enrolments")}/tax-enrolments/subscriptions/$subscriptionId/subscriber"

  private def requestBody(safeId: String): JsObject = {
    Json.obj(
      "serviceName" → JsString(serviceName),
      "callback" → JsString(callbackUrl),
      "etmpId" → JsString(safeId)
    )
  }

  private def addHeaders(implicit hc: HeaderCarrier): HeaderCarrier = {
    hc.withExtraHeaders(
      "Environment" -> getConfString("des.environment", "")
    ).copy(authorization = Some(Authorization(s"Bearer ${getConfString("des.token", "")}"))) // TODO check where these tokens are
  }

}
