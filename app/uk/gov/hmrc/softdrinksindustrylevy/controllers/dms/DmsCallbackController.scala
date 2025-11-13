/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.softdrinksindustrylevy.controllers.dms

import com.google.inject.Inject
import play.api.Logging
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.softdrinksindustrylevy.models.dms.DmsSubmissionResult

import scala.concurrent.Future

class DmsCallbackController @Inject() (cc: ControllerComponents) extends BackendController(cc) with Logging {
  def callback: Action[DmsSubmissionResult] = Action.async(parse.json[DmsSubmissionResult]) { implicit request =>
    val dmsSubmissionResult = request.body
    dmsSubmissionResult.failureReason match {
      case Some(failureReason) =>
        logger.warn(
          s"Dms submission failed due to $failureReason ${dmsSubmissionResult.failureType
              .getOrElse("")}, id: ${dmsSubmissionResult.id}"
        )
      case None =>
        logger.info(s"Dms submission processed successfully, id: ${dmsSubmissionResult.id}")
    }

    Future.successful(Ok)
  }
}
