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

package uk.gov.hmrc.softdrinksindustrylevy.config

import play.api.routing.Router
import com.softwaremill.macwire._
import play.api.Logger
import play.api.http.HttpErrorHandler

trait RoutesWiring {
  self: ControllerWiring with PlayWiring =>

  def errorHandler: HttpErrorHandler

  private lazy val prefix: String = ""
  private lazy val appRoutes = wire[app.Routes]
  private lazy val healthRoutes = wire[health.Routes]
  private lazy val testOnlyDoNotUseInAppConfRoutes = wire[testOnlyDoNotUseInAppConf.Routes]
  private lazy val prodRoutes = wire[prod.Routes]

  def router: Router =
    if (configuration.underlying.hasPath("play.http.router")) {
      configuration.getOptional[String]("play.http.router") match {
        case Some("testOnlyDoNotUseInAppConf.Routes") => testOnlyDoNotUseInAppConfRoutes
        case Some("prod.Routes")                      => prodRoutes
        case Some(other)                              => Logger.warn(s"Unrecognised router $other; using prod.Routes"); prodRoutes
        case _                                        => prodRoutes
      }
    } else {
      prodRoutes
    }
}
