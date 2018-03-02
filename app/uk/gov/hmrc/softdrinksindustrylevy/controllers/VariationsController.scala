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

import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Action
import uk.gov.hmrc.play.microservice.controller.BaseController
import uk.gov.hmrc.softdrinksindustrylevy.connectors.GformConnector
import uk.gov.hmrc.softdrinksindustrylevy.models.VariationsRequest

import scala.concurrent.ExecutionContext.Implicits.global

class VariationsController @Inject()(val messagesApi: MessagesApi,
                                     gforms: GformConnector) extends BaseController with I18nSupport {

  def generateVariations(sdilNumber: String) = Action.async(parse.json) { implicit request =>
    withJsonBody[VariationsRequest] { data =>
      val page = views.html.variations_pdf(data).toString
      gforms.submitToDms(page, sdilNumber) map { _ => NoContent }
    }
  }
}
