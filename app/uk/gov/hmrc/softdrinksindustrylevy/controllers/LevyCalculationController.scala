/*
 * Copyright 2026 HM Revenue & Customs
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

import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsError, Json}
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.{AuthConnector, AuthProviders, AuthorisationException, AuthorisedFunctions, MissingBearerToken}
import uk.gov.hmrc.softdrinksindustrylevy.models.{LevyCalculationRequest, LevyCalculationResponse, LevyCalculator}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LevyCalculationController @Inject() (
  cc: ControllerComponents,
  override val authConnector: AuthConnector
)(implicit ec: ExecutionContext)
    extends BackendController(cc) with AuthorisedFunctions {

  def calculateLevy: Action[AnyContent] = Action.async { implicit request =>
    authorised(AuthProviders(GovernmentGateway))
      .apply {
        request.body.asJson match {
          case None =>
            Future.successful(BadRequest(Json.obj("code" -> "INVALID_JSON", "message" -> "Expected JSON body")))

          case Some(json) =>
            json
              .validate[LevyCalculationRequest]
              .fold(
                errors =>
                  Future.successful(
                    BadRequest(
                      Json.obj(
                        "code"    -> "INVALID_REQUEST",
                        "message" -> "Invalid request payload",
                        "details" -> JsError.toJson(errors)
                      )
                    )
                  ),
                validRequest =>
                  try {
                    val calculationResult =
                      LevyCalculator.getLevyCalculation(
                        validRequest.lowLitres,
                        validRequest.highLitres,
                        validRequest.returnPeriod
                      )

                    val response =
                      LevyCalculationResponse(
                        calculationResult.lowLevy,
                        calculationResult.highLevy,
                        calculationResult.total,
                        calculationResult.totalRoundedDown
                      )

                    Future.successful(Ok(Json.toJson(response)))
                  } catch {
                    case e: IllegalArgumentException =>
                      Future.successful(BadRequest(Json.obj("code" -> "INVALID_REQUEST", "message" -> e.getMessage)))
                  }
              )

        }
      }
      .recover {
        case _: MissingBearerToken =>
          Unauthorized(Json.obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token not supplied"))

        case _: AuthorisationException =>
          Forbidden(Json.obj("code" -> "FORBIDDEN", "message" -> "Not authorised"))
      }
  }

}
