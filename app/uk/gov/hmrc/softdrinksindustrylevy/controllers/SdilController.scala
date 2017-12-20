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

import play.api.libs.json._
import play.api.mvc._
import reactivemongo.api.commands.LastError
import uk.gov.hmrc.play.microservice.controller.BaseController
import uk.gov.hmrc.softdrinksindustrylevy.connectors.{DesConnector, TaxEnrolmentConnector}
import uk.gov.hmrc.softdrinksindustrylevy.models._
import uk.gov.hmrc.softdrinksindustrylevy.models.json.des.create.createSubscriptionResponseFormat
import uk.gov.hmrc.softdrinksindustrylevy.models.json.internal._
import uk.gov.hmrc.softdrinksindustrylevy.services.{DesSubmissionService, MongoStorageService}

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class SdilController @Inject()(desSubmissionService: DesSubmissionService,
															 taxEnrolmentConnector: TaxEnrolmentConnector,
															 desConnector: DesConnector) extends BaseController {

	def submitRegistration(idType: String, idNumber: String, safeId: String): Action[JsValue] = Action.async(parse.json)  { implicit request =>
		withJsonBody[Subscription](data =>
			desConnector.createSubscription(data, idType, idNumber).map {
				response =>
					// TODO store in mongo
					taxEnrolmentConnector.subscribe(safeId, response.formBundleNumber)
					Ok(Json.toJson(response))
			}
		)
	}

	def retrieveSubscriptionDetails(idType: String, idNumber: String):Action[AnyContent] = Action.async { implicit request =>
		desConnector.retrieveSubscriptionDetails(idType, idNumber).map {
			response => {
        response match {
          case r if r.status == 200 => Ok(Json.obj("status" -> "SUBSCRIBED"))
          case _ => NotFound(Json.obj("status" -> "NOT_SUBSCRIBED"))
        }
      }
    }
  }

  def checkPendingSubscription(utr: String): Action[AnyContent] = Action.async { implicit request =>
    mongo.findById(utr) map {
      case Some(_) => Ok(Json.obj("status" -> "SUBSCRIPTION_PENDING"))
      case _ => NotFound(Json.obj("status" -> "SUBSCRIPTION_NOT_FOUND"))
    }
  }
}
