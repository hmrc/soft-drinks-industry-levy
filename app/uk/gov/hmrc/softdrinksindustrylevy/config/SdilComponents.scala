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

import com.softwaremill.macwire._
import play.api.ApplicationLoader.Context
import play.api.http.{HttpErrorHandler, HttpRequestHandler}
import play.api.i18n.I18nComponents
import play.api.inject.{Injector, SimpleInjector}
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc.ControllerComponents
import play.api.{Application, BuiltInComponentsFromContext, Configuration, DefaultApplication}
import play.filters.HttpFiltersComponents
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.bootstrap.config.Base64ConfigDecoder
import uk.gov.hmrc.play.bootstrap.http._
import uk.gov.hmrc.softdrinksindustrylevy.services._

import scala.concurrent.ExecutionContext

class SdilComponents(context: Context)
    extends BuiltInComponentsFromContext(context) with I18nComponents with RoutesWiring with ControllerWiring
    with ConnectorWiring with ServicesWiring with PlayWiring with Base64ConfigDecoder with AhcWSComponents
    with FiltersWiring with ConfigWiring with HttpFiltersComponents {

  override lazy val configuration: Configuration = DefaultBase64ConfigDecoder.decodeConfig(context.initialConfiguration)
  override def errorHandler: HttpErrorHandler = wire[JsonErrorHandler]
  override implicit lazy val executionContext: ExecutionContext = actorSystem.dispatcher
  override def httpClient: HttpClient = wire[DefaultHttpClient]
  override lazy val httpRequestHandler: HttpRequestHandler = wire[RequestHandler]
  lazy val customInjector: Injector = new SimpleInjector(injector) + healthController + wsClient
  lazy val httpAuditing: HttpAuditing = wire[DefaultHttpAuditing]
  override lazy val application: Application = wire[DefaultApplication]
  val dataCorrector: DataCorrector = wire[DataCorrector]
  override val cc: ControllerComponents = controllerComponents
}
