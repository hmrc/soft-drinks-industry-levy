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

import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.JsValue
import play.api.mvc.{Action, ControllerComponents}
import sdil.models.ReturnVariationData
import uk.gov.hmrc.play.bootstrap.controller.BackendController
import uk.gov.hmrc.softdrinksindustrylevy.connectors.GformConnector
import uk.gov.hmrc.softdrinksindustrylevy.models.{ReturnsVariationRequest, VariationsRequest, formatReturnVariationData}
import uk.gov.hmrc.softdrinksindustrylevy.services.{ReturnsAdjustmentSubmissionService, ReturnsVariationSubmissionService, VariationSubmissionService}

import scala.concurrent.ExecutionContext.Implicits.global

class VariationsController(
  override val messagesApi: MessagesApi,
  gforms: GformConnector,
  variationSubmissions: VariationSubmissionService,
  returnSubmission: ReturnsVariationSubmissionService,
  returnsAdjustmentSubmissionService: ReturnsAdjustmentSubmissionService,
  val cc: ControllerComponents
) extends BackendController(cc) with I18nSupport {

  def generateVariations(sdilNumber: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[VariationsRequest] { data =>
      val page = views.html.variations_pdf(data, sdilNumber).toString
      for {
        _ <- gforms.submitToDms(page, sdilNumber)
        _ <- variationSubmissions.save(data, sdilNumber)
      } yield NoContent
    }
  }

  def returnsVariation(sdilNumber: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[ReturnsVariationRequest] { data =>
      val page = views.html.returns_variation_pdf(data, sdilNumber).toString
      for {
        _ <- gforms.submitToDms(page, sdilNumber)
        _ <- returnSubmission.save(data, sdilNumber)
      } yield NoContent
    }
  }

  def varyReturn(sdilRef: String): Action[JsValue] =
    Action.async(parse.json) { implicit request =>
      withJsonBody[ReturnVariationData] { data =>
        Logger.info("SDIL return variation sent to DMS queue")
        val page = views.html.return_variation_pdf(data, sdilRef).toString

        for {
          _ <- gforms.submitToDms(page, sdilRef)
          _ <- returnsAdjustmentSubmissionService.save(data, sdilRef)
        } yield NoContent

      }
    }
}
