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

import play.api.Configuration
import play.api.Mode.Mode
import play.api.libs.json._
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.{AuthConnector, AuthProviders, AuthorisedFunctions}
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.softdrinksindustrylevy.connectors.{RosmConnector, TaxEnrolmentConnector}
import uk.gov.hmrc.softdrinksindustrylevy.models._
import uk.gov.hmrc.softdrinksindustrylevy.services.JsonSchemaChecker

import scala.concurrent.ExecutionContext.Implicits.global

class RosmController(val authConnector: AuthConnector,
                     rosmConnector: RosmConnector,
                     taxEnrolmentConnector: TaxEnrolmentConnector,
                     val mode: Mode,
                     val runModeConfiguration: Configuration)
  extends BaseController with ServicesConfig with AuthorisedFunctions {

  def lookupRegistration(utr: String): Action[AnyContent] = Action.async { implicit request =>
    authorised(AuthProviders(GovernmentGateway)) {
      rosmConnector.retrieveROSMDetails(utr, RosmRegisterRequest(regime = getString("etmp.sdil.regime"))).map {
        case r if r.exists {
          JsonSchemaChecker[RosmRegisterResponse](r.get, "rosm-response")
          res => res.organisation.isDefined || res.individual.isDefined
        } => Ok(Json.toJson(r))
        case _ => NotFound
      }
    }
  }

}
