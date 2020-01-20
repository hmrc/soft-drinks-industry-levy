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

package uk.gov.hmrc.softdrinksindustrylevy.controllers

import play.api.Mode
import play.api.libs.json._
import play.api.mvc.{Action, ControllerComponents}
import play.api.{Configuration, Logger}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.{RunMode, ServicesConfig}
import uk.gov.hmrc.play.bootstrap.controller.BackendController
import uk.gov.hmrc.softdrinksindustrylevy.connectors.{EmailConnector, Identifier, TaxEnrolmentConnector, TaxEnrolmentsSubscription}
import uk.gov.hmrc.softdrinksindustrylevy.models.TaxEnrolmentEvent
import uk.gov.hmrc.softdrinksindustrylevy.services.MongoBufferService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TaxEnrolmentCallbackController(
  buffer: MongoBufferService,
  emailConnector: EmailConnector,
  taxEnrolments: TaxEnrolmentConnector,
  val mode: Mode,
  val cc: ControllerComponents,
  val runModeConfiguration: Configuration,
  val runMode: RunMode,
  auditing: AuditConnector)
    extends BackendController(cc) {

  val serviceConfig = new ServicesConfig(runModeConfiguration, runMode)

  def callback(formBundleNumber: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[CallbackNotification] { body =>
      if (body.state == "SUCCEEDED") {
        for {
          teSub      <- taxEnrolments.getSubscription(formBundleNumber)
          pendingSub <- buffer.findById(teSub.etmpId)
          _          <- buffer.removeById(teSub.etmpId)
          _ <- sendNotificationEmail(
                pendingSub.map(_.subscription.orgName),
                pendingSub.map(_.subscription.contact.email),
                getSdilNumber(teSub),
                formBundleNumber)
          _ <- auditing.sendExtendedEvent(buildAuditEvent(body, request.uri, formBundleNumber))
        } yield {
          Logger.info("Tax-enrolments callback successful")
          NoContent
        }
      } else {
        Logger.error(
          s"Got error from tax-enrolments callback for $formBundleNumber: [${body.errorResponse.getOrElse("")}]")
        auditing.sendExtendedEvent(buildAuditEvent(body, request.uri, formBundleNumber)) map { _ =>
          NoContent
        }
      }
    }
  }

  private def sendNotificationEmail(
    orgName: Option[String],
    email: Option[String],
    sdilNumber: Option[String],
    formBundleNumber: String)(implicit hc: HeaderCarrier): Future[Unit] =
    (orgName, email) match {
      case (Some(o), Some(e)) =>
        sdilNumber match {
          case Some(s) => emailConnector.sendConfirmationEmail(o, e, s)
          case None =>
            Future.successful(
              Logger.error(
                s"Unable to send email for form bundle $formBundleNumber as enrolment is missing SDIL Number"))
        }
      case _ =>
        Future.successful(
          Logger.error(s"Received callback for form bundle number $formBundleNumber, but no pending record exists"))
    }

  private def getSdilNumber(taxEnrolmentsSubscription: TaxEnrolmentsSubscription): Option[String] =
    taxEnrolmentsSubscription.identifiers.getOrElse(Nil).collectFirst {
      case Identifier(_, value) if value.slice(2, 4) == "SD" => value
    }

  private def buildAuditEvent(callback: CallbackNotification, path: String, subscriptionId: String)(
    implicit hc: HeaderCarrier) = {
    implicit val callbackFormat: OWrites[CallbackNotification] = Json.writes[CallbackNotification]
    val detailJson = Json.obj(
      "subscriptionId" -> subscriptionId,
      "url"            -> s"${serviceConfig.baseUrl("tax-enrolments")}/tax-enrolments/subscriptions/$subscriptionId",
      "outcome" -> (callback.state match {
        case "SUCCEEDED" => "SUCCESS"
        case _           => "ERROR"
      }),
      "errorResponse" -> callback.errorResponse
    )
    new TaxEnrolmentEvent(callback.state, path, detailJson)
  }

}

case class CallbackNotification(state: String, errorResponse: Option[String])

object CallbackNotification {
  implicit val format: Format[CallbackNotification] = Json.format[CallbackNotification]
}
