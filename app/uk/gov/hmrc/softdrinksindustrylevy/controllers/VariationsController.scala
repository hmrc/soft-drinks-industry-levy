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

import java.io.PrintWriter

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.JsValue
import play.api.mvc.Action
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.softdrinksindustrylevy.connectors.GformConnector
import uk.gov.hmrc.softdrinksindustrylevy.models.{ReturnsVariationRequest, VariationsRequest}
import uk.gov.hmrc.softdrinksindustrylevy.services.{ReturnsVariationSubmissionService, VariationSubmissionService}
import uk.gov.hmrc.softdrinksindustrylevy.models.json.des.create.addressFormat

import scala.concurrent.ExecutionContext.Implicits.global
import sys.process._


class VariationsController(
  val messagesApi: MessagesApi,
  gforms: GformConnector,
  variationSubmissions: VariationSubmissionService,
  returnSubmission: ReturnsVariationSubmissionService
) extends BaseController with I18nSupport {

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
//      val temp = java.io.File.createTempFile("variations", ".html")
//
//      println(s"Writing to $temp")
//
//      new PrintWriter(temp) { write(page); close }
//
//      s"epiphany $temp".!
//
//      concurrent.Future.successful(NoContent)
      for {
        _ <- gforms.submitToDms(page, sdilNumber)
        _ <- returnSubmission.save(data, sdilNumber)
      } yield NoContent

    }
  }
}
