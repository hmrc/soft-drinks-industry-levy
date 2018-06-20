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

import java.time.{ Clock, LocalDate }

import play.api.libs.json._
import play.api.mvc.{Action, AnyContent}
import sdil.models.{ ReturnPeriod, SdilReturn }
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.softdrinksindustrylevy.connectors.DesConnector
import uk.gov.hmrc.softdrinksindustrylevy.models._
import uk.gov.hmrc.softdrinksindustrylevy.models.json.des.returns._

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.softdrinksindustrylevy.services.SdilPersistence

class ReturnsController(
  val authConnector: AuthConnector,
  desConnector: DesConnector,
  val persistence: SdilPersistence
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

  def submitReturn(sdilRef: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[ReturnsRequest] { returnsReq =>
      desConnector.submitReturn(sdilRef, returnsReq) map {
        _ => Ok(Json.toJson(returnsReq))
      }
    }
  }

  def post(utr: String, year: Int, quarter: Int): Action[JsValue] =
    Action.async(parse.json) { implicit request =>
      withJsonBody[SdilReturn] { sdilReturn =>

        val period = ReturnPeriod(year, quarter)
        val returnsReq = ReturnsRequest(sdilReturn)
        for {
          subscription <- desConnector.retrieveSubscriptionDetails("utr", utr)
          ref          =  subscription.get.sdilRef
          _            <- desConnector.submitReturn(ref, returnsReq)
          _            <- persistence.returns(utr, period) = sdilReturn
        } yield Ok(Json.toJson(returnsReq))
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
      val today = LocalDate.now
      desConnector.retrieveSubscriptionDetails("utr", utr).flatMap {
        subscription => 

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
