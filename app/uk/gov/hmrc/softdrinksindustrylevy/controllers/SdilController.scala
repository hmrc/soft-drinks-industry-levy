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
import uk.gov.hmrc.play.microservice.controller.BaseController
import uk.gov.hmrc.softdrinksindustrylevy.connectors.DesConnector
import uk.gov.hmrc.softdrinksindustrylevy.models._
import uk.gov.hmrc.softdrinksindustrylevy.services.DesSubmissionService
import scala.concurrent.ExecutionContext.Implicits.global
import json.internal._
import json.des.create.createSubscriptionResponseFormat

@Singleton
class SdilController @Inject()(desSubmissionService: DesSubmissionService,
															 desConnector: DesConnector) extends BaseController {

	def submitRegistration(idType: String, idNumber: String): Action[JsValue] = Action.async(parse.json)  { implicit request =>
		withJsonBody[Subscription](data =>
			desConnector.createSubscription(data, idType, idNumber).map {
				response => Ok(Json.toJson(response))
			}
		)
	}

	def retrieveSubscripionDetails(idType: String, idNumber: String):Action[AnyContent] = Action.async { implicit request =>
		desConnector.retrieveSubscriptionDetails(idType, idNumber).map {
			response => response match {
        case r if r.status == 200 => Ok(Json.parse(""" {"known" : "you are subscribed"} """))
        case _ => Ok(Json.parse(""" {"unknown" : "you are not subscribed"} """))
      }
		}
	}
}
