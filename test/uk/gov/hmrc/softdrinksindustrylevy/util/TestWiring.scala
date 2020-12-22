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

package uk.gov.hmrc.softdrinksindustrylevy.util

import java.io.File
import java.time.Clock

import akka.stream.Materializer
import com.softwaremill.macwire._
import org.scalatestplus.mockito.MockitoSugar
import play.api.Mode.Mode
import play.api.inject.DefaultApplicationLifecycle
import play.api.{Configuration, Environment, Play}
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.audit.DefaultAuditConnector
import uk.gov.hmrc.play.bootstrap.config.{AuditingConfigProvider, ServicesConfig}
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpAuditing

trait TestWiring extends MockitoSugar {
  val appName = configuration.getString("appName").get
  lazy val auditingConfigProvider: AuditingConfigProvider = wire[AuditingConfigProvider]
  val auditingconfig = auditingConfigProvider.get()
  lazy val applicatonLifecycle = wire[DefaultApplicationLifecycle]
  lazy val materializer: Materializer = mock[Materializer]
  lazy val auditConnector: AuditConnector = wire[DefaultAuditConnector]
  lazy val httpAuditing: HttpAuditing = wire[DefaultHttpAuditing]
  lazy val configuration: Configuration = Configuration.load(environment, Map("auditing.enabled" -> "false"))
  lazy val environment: Environment = Environment.simple(new File("."))
  lazy val mode: Mode = environment.mode
  implicit def clock: Clock = Clock.systemDefaultZone()
  lazy val actorSystem = Play.current.actorSystem
  val servicesConfig = wire[ServicesConfig]
}
