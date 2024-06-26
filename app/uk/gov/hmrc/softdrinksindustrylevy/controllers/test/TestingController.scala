/*
 * Copyright 2023 HM Revenue & Customs
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
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.softdrinksindustrylevy.connectors.{FileUploadConnector, TestConnector}
import uk.gov.hmrc.softdrinksindustrylevy.services.{MongoBufferService, ReturnsPersistence, SdilMongoPersistence, VariationSubmissionService}
import com.google.inject.{Inject, Singleton}

import scala.concurrent.ExecutionContext
@Singleton
class TestingController @Inject() (
  override val messagesApi: MessagesApi,
  testConnector: TestConnector,
  pending: MongoBufferService,
  subscriptions: SdilMongoPersistence,
  fileUpload: FileUploadConnector,
  variationSubmissions: VariationSubmissionService,
  cc: ControllerComponents,
  returns: ReturnsPersistence
)(implicit ec: ExecutionContext)
    extends BackendController(cc) with I18nSupport {

  def reset(url: String): Action[AnyContent] = Action.async { implicit request =>
    testConnector.reset(url) map (r => Status(r.status))
  }

  def resetPendingDb: Action[AnyContent] = Action.async {
    pending.collection.drop().toFuture().map(_ => Ok)
  }

  def resetSubscriptionsDb: Action[AnyContent] = Action.async {
    subscriptions.collection.drop().toFuture().map(_ => Ok)
  }

  def getFile(envelopeId: String, fileName: String) = Action.async {
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

  def getSdilReturnsMongoDrop: Action[AnyContent] = Action.async {
    returns.dropCollection.map(_ => Ok)
  }
}
