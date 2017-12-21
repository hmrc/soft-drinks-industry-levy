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

import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.microservice.controller.BaseController
import uk.gov.hmrc.softdrinksindustrylevy.services.MongoStorageService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class TaxEnrolmentCallbackController @Inject()(mongo: MongoStorageService)
  extends BaseController {

  def callback(formBundleNumber: String): Action[AnyContent] = Action.async { implicit request =>
    // todo go and get the subscription? see https://confluence.tools.tax.service.gov.uk/display/SOS/tax-enrolment+subscription+API
    // delete stuff from mongo
    // mongo.removeById(formBundleNumber)
    // email user
    // return something maybe?
    Future.successful(NotFound) // TODO change this
  }

}
