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

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Action
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.softdrinksindustrylevy.connectors.DesConnector
import uk.gov.hmrc.softdrinksindustrylevy.models.ReturnsRequest
import uk.gov.hmrc.softdrinksindustrylevy.models.json.des.returns._

import scala.concurrent.ExecutionContext

class ReturnsController(val authConnector: AuthConnector,
                        desConnector: DesConnector)(implicit ec: ExecutionContext)
  extends BaseController with AuthorisedFunctions {


  def submitReturn(sdilRef: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    //    authorised(AuthProviders(GovernmentGateway)) {
    withJsonBody[ReturnsRequest] { returnsReq =>
      desConnector.submitReturn(sdilRef, returnsReq) map {
        _ => Ok(Json.obj("VALID" -> returnsReq.toString))
      }
    }
    //    }
  }

}
