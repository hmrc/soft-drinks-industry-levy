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

import java.time.{Clock, LocalDateTime}

import play.api.Logger
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent}
import reactivemongo.bson.BSONObjectID
import sdil.models.{ReturnPeriod, SdilReturn}
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.retrieve.Retrievals.credentials
import uk.gov.hmrc.auth.core.{AuthConnector, AuthProviders, AuthorisedFunctions}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.softdrinksindustrylevy.config.SdilConfig
import uk.gov.hmrc.softdrinksindustrylevy.connectors.DesConnector
import uk.gov.hmrc.softdrinksindustrylevy.models._
import uk.gov.hmrc.softdrinksindustrylevy.models.json.des.returns._
import uk.gov.hmrc.softdrinksindustrylevy.services.SdilPersistence

import scala.concurrent.{ExecutionContext, Future}

class ReturnsController(
  val authConnector: AuthConnector,
  desConnector: DesConnector,
  val persistence: SdilPersistence,
  val sdilConfig: SdilConfig,
  auditing: AuditConnector
)
                       (implicit ec: ExecutionContext, clock: Clock)
  extends BaseController with AuthorisedFunctions {

  def validateSmallProducer(sdilRef: String): Action[AnyContent] = Action.async { implicit request =>
    if (sdilRef.matches("X[A-Z]SDIL000[0-9]{6}")) {
      desConnector.retrieveSubscriptionDetails("sdil", sdilRef) map {
        case Some(subscription) if subscription.isDeregistered => NotFound(Json.obj("errorCode" -> "DEREGISTERED"))
        case Some(subscription) if subscription.activity.isSmallProducer => Ok
        case Some(subscription) => NotFound(Json.obj("errorCode" -> "NOT_SMALL_PRODUCER"))
        case None => NotFound
      }
    } else {
      Future.successful(BadRequest(Json.obj("errorCode" -> "INVALID_REFERENCE")))
    }
  }

  def buildReturnAuditDetail(
    sdilReturn: SdilReturn,
    returnsRequest: ReturnsRequest,
    providerId: String,
    period: ReturnPeriod,
    subscription: Option[Subscription],
    utr: String,
    outcome: String
  ): JsValue ={
    val sdilNo: String = subscription.flatMap{_.sdilRef}.fold("unknown"){identity}
    Json.obj(
      "sdilNumber" -> sdilNo,
      "orgName" -> subscription.fold("unknown"){_.orgName},
      "utr" -> utr,
      "outcome" -> outcome,
      "authProviderType" -> "GovernmentGateway",
      "authProviderId" -> providerId,
      "return" -> Json.toJson(returnsRequest)(writesForAuditing(period, sdilReturn)).as[JsObject]
    )
  }

  def post(utr: String, year: Int, quarter: Int): Action[JsValue] =
    Action.async(parse.json) { implicit request =>
      authorised(AuthProviders(GovernmentGateway)).retrieve(credentials) { creds =>
        withJsonBody[SdilReturn] { sdilReturn =>
          Logger.info("SDIL return submission sent to DES")
          implicit val period: ReturnPeriod = ReturnPeriod(year, quarter)

          val returnsReq = ReturnsRequest(sdilReturn)
          (for {
            subscription <- desConnector.retrieveSubscriptionDetails("utr", utr)
            ref = subscription.get.sdilRef.get
            _ <- desConnector.submitReturn(ref, returnsReq)
            _ <- auditing.sendExtendedEvent(
              new SdilReturnEvent(
                request.uri,
                buildReturnAuditDetail(sdilReturn, returnsReq, creds.providerId, period, subscription, utr, "SUCCESS")
              )
            )
            _ <- persistence.returns(utr, period) = sdilReturn
          } yield {
            Ok(Json.toJson(returnsReq))
          }).recoverWith {
            case e =>
              auditing.sendExtendedEvent(
                new SdilReturnEvent(
                  request.uri,
                  buildReturnAuditDetail(sdilReturn, returnsReq, creds.providerId, period, None, utr, "ERROR")
                )
              ) map {
                throw e
              }
          }
        }
      }
    }

  def get(utr: String, year: Int, quarter: Int): Action[AnyContent] =
    Action.async { implicit request =>
      persistence.returns.get(utr, ReturnPeriod(year, quarter)).map {
        case Some(record) => Ok(Json.toJson(record))
        case None =>         NotFound
      }
    }

  def pending(utr: String): Action[AnyContent] =
    Action.async { implicit request =>

      desConnector.retrieveSubscriptionDetails("utr", utr).flatMap {
        subscription =>

        import sdilConfig.today
        val start = subscription.get.liabilityDate

        val all = {ReturnPeriod(start).count to ReturnPeriod(today).count}
          .map{ReturnPeriod.apply}
          .filter{_.end.isBefore(today)}
        persistence.returns.list(utr).map { posted => 
          Ok(Json.toJson(all.toList diff posted.keys.toList))
        }
      }
    }
  
}
