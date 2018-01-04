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

package uk.gov.hmrc.softdrinksindustrylevy.controllers

import javax.inject.{Inject, Singleton}

import play.api.Logger
import play.api.libs.json._
import play.api.mvc._
import reactivemongo.api.commands.LastError
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.{AuthConnector, AuthProviders, AuthorisedFunctions}
import uk.gov.hmrc.play.microservice.controller.BaseController
import uk.gov.hmrc.softdrinksindustrylevy.connectors.{DesConnector, EmailConnector, TaxEnrolmentConnector, TaxEnrolmentsSubscription}
import uk.gov.hmrc.softdrinksindustrylevy.models._
import uk.gov.hmrc.softdrinksindustrylevy.models.json.des.create.createSubscriptionResponseFormat
import uk.gov.hmrc.softdrinksindustrylevy.models.json.internal._
import uk.gov.hmrc.softdrinksindustrylevy.services._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class SdilController @Inject()(val authConnector: AuthConnector,
                               desSubmissionService: DesSubmissionService,
                               taxEnrolmentConnector: TaxEnrolmentConnector,
                               desConnector: DesConnector,
                               buffer: MongoBufferService,
                               emailConnector: EmailConnector)
  extends BaseController with AuthorisedFunctions {

  def submitRegistration(idType: String, idNumber: String, safeId: String): Action[JsValue] = {
    Action.async(parse.json) { implicit request =>
      authorised(AuthProviders(GovernmentGateway)) {
        withJsonBody[Subscription](data =>
          (for {
            res <- desConnector.createSubscription(data, idType, idNumber)
            _ <- buffer.insert(SubscriptionWrapper(safeId, data, res.formBundleNumber))
            _ <- taxEnrolmentConnector.subscribe(safeId, res.formBundleNumber)
            _ <- emailConnector.sendSubmissionReceivedEmail(data.contact.email, data.orgName)
          } yield {
            Ok(Json.toJson(res))
          }) recover {
            case e: LastError if e.code.contains(11000) => Conflict(Json.obj("status" -> "UTR_ALREADY_SUBSCRIBED"))
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
    desConnector.retrieveSubscriptionDetails("utr", utr) flatMap {
      case Some(s) =>
        buffer.find("subscription.utr" -> utr) flatMap {
          case Nil => Future successful Ok(Json.toJson(s))
          case l :: _ => taxEnrolmentConnector.getSubscription(l.formBundleNumber) flatMap {
            checkEnrolmentState(utr, s)
          } recover {
            case e: NotFoundException =>
              Logger.error(e.message) // TODO log to deskpro pending decision
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

  private def checkEnrolmentState(utr: String, s: Subscription): TaxEnrolmentsSubscription => Future[Result] = {
    case a if  a.state == "SUCCEEDED" =>
      buffer.remove("subscription.utr" -> utr) map {
        _ => Ok(Json.toJson(s))
      }
    case a if a.state == "ERROR" =>
      Logger.error(a.errorResponse.getOrElse("unknown tax enrolment error")) // TODO also deskpro
      Future successful Ok(Json.toJson(s))
    case _ => Future successful Ok(Json.toJson(s))
  }

}
