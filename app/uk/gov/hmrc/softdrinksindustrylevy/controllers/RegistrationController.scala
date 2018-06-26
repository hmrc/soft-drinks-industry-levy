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

package uk.gov.hmrc.softdrinksindustrylevy.controllers

import play.api.Logger
import play.api.libs.json._
import play.api.mvc._
import reactivemongo.api.commands.LastError
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.retrieve.Retrievals._
import uk.gov.hmrc.auth.core.{AuthConnector, AuthProviders, AuthorisedFunctions}
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.softdrinksindustrylevy.connectors._
import uk.gov.hmrc.softdrinksindustrylevy.models._
import uk.gov.hmrc.softdrinksindustrylevy.models.json.des.create.createSubscriptionResponseFormat
import uk.gov.hmrc.softdrinksindustrylevy.models.json.internal._
import uk.gov.hmrc.softdrinksindustrylevy.services._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RegistrationController(val authConnector: AuthConnector,
                             taxEnrolmentConnector: TaxEnrolmentConnector,
                             desConnector: DesConnector,
                             buffer: MongoBufferService,
                             emailConnector: EmailConnector,
                             auditing: AuditConnector)
  extends BaseController with AuthorisedFunctions {

  def submitRegistration(idType: String, idNumber: String, safeId: String): Action[JsValue] = {
    Action.async(parse.json) { implicit request =>
      authorised(AuthProviders(GovernmentGateway)).retrieve(credentials) { creds =>
        withJsonBody[Subscription](data => {
          Logger.info("SDIL Subscription submission sent to DES")
          (for {
            res <- desConnector.createSubscription(data, idType, idNumber)
            _ <- buffer.insert(SubscriptionWrapper(safeId, data, res.formBundleNumber))
            _ <- taxEnrolmentConnector.subscribe(safeId, res.formBundleNumber)
            _ <- emailConnector.sendSubmissionReceivedEmail(data.contact.email, data.orgName)
            _ <- auditing.sendExtendedEvent(
              new SdilSubscriptionEvent(
                request.uri, buildSubscriptionAudit(data, creds.providerId, Some(res.formBundleNumber), "SUCCESS")
              )
            )
          } yield {
            Ok(Json.toJson(res))
          }) recoverWith {
            case e: LastError if e.code.contains(11000) => {
              auditing.sendExtendedEvent(
                new SdilSubscriptionEvent(request.uri, buildSubscriptionAudit(data, creds.providerId, None, "ERROR"))
              ) map {
                _ => Conflict(Json.obj("status" -> "UTR_ALREADY_SUBSCRIBED"))
              }
            }
            case e =>
              auditing.sendExtendedEvent(
                new SdilSubscriptionEvent(request.uri, buildSubscriptionAudit(data, creds.providerId, None, "ERROR"))
              ) map {
                throw e
              }
          }
        }
        )
      }
    }
  }

  def retrieveSubscriptionDetails(idType: String, idNumber: String): Action[AnyContent] = Action.async { implicit request =>
    import json.internal._

    authorised(AuthProviders(GovernmentGateway)) {
      desConnector.retrieveSubscriptionDetails(idType, idNumber).map {
        case Some(s) => Ok(Json.toJson(s))
        case None => NotFound
      }
    }
  }

  def checkEnrolmentStatus(utr: String): Action[AnyContent] = Action.async { implicit request =>
    authorised(AuthProviders(GovernmentGateway)) {
      desConnector.retrieveSubscriptionDetails("utr", utr) flatMap {
        case Some(s) if !s.isDeregistered =>
          buffer.find("subscription.utr" -> utr) flatMap {
            case Nil => Future successful Ok(Json.toJson(s))
            case l :: _ => taxEnrolmentConnector.getSubscription(l.formBundleNumber) flatMap {
              checkEnrolmentState(utr, s)
            } recover {
              case e: NotFoundException =>
                Logger.error(e.message)
                Ok(Json.toJson(s))
            }
          }
        case _ => buffer.find("subscription.utr" -> utr) map {
          case Nil => NotFound
          case l :: _ =>
            Accepted(Json.toJson(l.subscription))
        }
      }
    }
  }

  private def checkEnrolmentState(utr: String, s: Subscription): TaxEnrolmentsSubscription => Future[Result] = {
    case a if a.state == "SUCCEEDED" =>
      buffer.remove("subscription.utr" -> utr) map {
        _ => Ok(Json.toJson(s))
      }
    case a if a.state == "ERROR" =>
      Logger.error(a.errorResponse.getOrElse("unknown tax enrolment error")) // TODO also deskpro
      Future successful Ok(Json.toJson(s))
    case a if a.state == "PENDING" =>
      Future successful Accepted(Json.toJson(s))
    case _ => Future successful Ok(Json.toJson(s))
  }

  private def buildSubscriptionAudit(subscription: Subscription, providerId: String,
                                     formBundleNumber: Option[String], outcome: String)
                                    (implicit hc: HeaderCarrier): JsValue = {
    import uk.gov.hmrc.softdrinksindustrylevy.models.json.internal._
    implicit val activityMapFormat: Writes[Activity] = new Writes[Activity] {
      def writes(activity: Activity): JsValue = JsObject(
        activity match {
          case InternalActivity(a, lg) => a.map { case (t, lb) =>
            (t.toString.head.toLower +: t.toString.tail) -> litreBandsFormat.writes(lb)
          } ++ Map("isLarge" -> JsBoolean(lg))
        }
      )
    }

    implicit val subWrites: Writes[Subscription] = new Writes[Subscription] {
      def writes(s: Subscription): JsValue = Json.obj(
        "utr" -> s.utr,
        "orgName" -> s.orgName,
        "orgType" -> toName(s.orgType.getOrElse("0")),
        "address" -> s.address,
        "litreageActivity" -> activityMapFormat.writes(s.activity),
        "liabilityDate" -> s.liabilityDate,
        "productionSites" -> s.productionSites,
        "warehouseSites" -> s.warehouseSites,
        "contact" -> s.contact
      )
    }

    Json.obj(
      "subscriptionId" -> formBundleNumber,
      "outcome" -> outcome,
      "authProviderType" -> "GovernmentGateway",
      "authProviderId" -> providerId,
      "deviceId" -> hc.deviceID
    ).++(Json.toJson(subscription)(subWrites).as[JsObject])
  }

  private def toName: String => String = {
    case "1" => "Sole Trader"
    case "2" => "Limited Liability Partnership"
    case "3" => "Partnership"
    case "5" => "Unincorporated Body"
    case "7" => "Limited Company"
    case other => throw new IllegalArgumentException(s"Unexpected orgType $other")
  }

}
