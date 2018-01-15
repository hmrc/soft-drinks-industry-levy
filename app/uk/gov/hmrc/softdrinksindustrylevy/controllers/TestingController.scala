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

import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.microservice.controller.BaseController
import uk.gov.hmrc.softdrinksindustrylevy.connectors.TestConnector
import uk.gov.hmrc.softdrinksindustrylevy.services.MongoBufferService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}


class TestingController @Inject()(testConnector: TestConnector,
                                  buffer: MongoBufferService)
  extends BaseController {

  def resetStore: Action[AnyContent] = Action.async {
    implicit request =>
      testConnector.sendReset map {
        r => Status(r.status)
      }
  }

  def resetDb: Action[AnyContent] = Action.async {
    implicit request =>
      buffer.drop.flatMap(_ => Future(Status(OK)))
  }

}
