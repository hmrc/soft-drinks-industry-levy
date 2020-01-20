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

package uk.gov.hmrc.softdrinksindustrylevy.controllers.test

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.controller.BackendController
import uk.gov.hmrc.softdrinksindustrylevy.connectors.{FileUploadConnector, TestConnector}
import uk.gov.hmrc.softdrinksindustrylevy.services.{MongoBufferService, SdilPersistence, VariationSubmissionService}

import scala.concurrent.ExecutionContext.Implicits.global

class TestingController(
  override val messagesApi: MessagesApi,
  testConnector: TestConnector,
  buffer: MongoBufferService,
  fileUpload: FileUploadConnector,
  variationSubmissions: VariationSubmissionService,
  cc: ControllerComponents,
  persistence: SdilPersistence)
    extends BackendController(cc) with I18nSupport {

  def reset(url: String): Action[AnyContent] = Action.async { implicit request =>
    testConnector.reset(url) map (r => Status(r.status))
  }

  def resetDb: Action[AnyContent] = Action.async { implicit request =>
    buffer.drop.map(_ => Ok)
  }

  def getFile(envelopeId: String, fileName: String) = Action.async { implicit request =>
    fileUpload.getFile(envelopeId, fileName) map { file =>
      Ok(file)
    }
  }

  def getVariationHtml(sdilNumber: String): Action[AnyContent] = Action.async { implicit request =>
    variationSubmissions.get(sdilNumber) map {
      case Some(v) => Ok(views.html.variations_pdf(v, sdilNumber))
      case None    => NotFound
    }
  }

  def getSdilReturnsMongoDrop: Action[AnyContent] = Action.async { implicit request =>
    persistence.returns.dropCollection.map {
      case true  => Ok
      case false => NoContent
    }
  }
}
