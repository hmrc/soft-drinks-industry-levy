/*
 * Copyright 2021 HM Revenue & Customs
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
import play.api.libs.ws.WSClient
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.mongo.MongoConnector
import uk.gov.hmrc.play.audit.http.config.AuditingConfig
import uk.gov.hmrc.play.audit.http.connector.{AuditChannel, AuditConnector, AuditCounter}
import uk.gov.hmrc.play.bootstrap.audit.{DefaultAuditChannel, DefaultAuditConnector, DefaultAuditCounter}
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.config.{AuditingConfigProvider, ServicesConfig}
import uk.gov.hmrc.softdrinksindustrylevy.connectors._
import uk.gov.hmrc.softdrinksindustrylevy.services.{SdilMongoPersistence, SdilPersistence}

trait ConnectorWiring {
  self: PlayWiring =>

  def httpClient: HttpClient
  def wsClient: WSClient

  lazy val persistence: SdilPersistence = wire[SdilMongoPersistence]
  lazy val auditConnector: AuditConnector = wire[DefaultAuditConnector]
  lazy val auditChannel: AuditChannel = wire[DefaultAuditChannel]
  lazy val auditCounter: AuditCounter = wire[DefaultAuditCounter]
  lazy val authConnector: AuthConnector = wire[DefaultAuthConnector]
  lazy val desConnector: DesConnector = wire[DesConnector]
  lazy val emailConnector: EmailConnector = wire[EmailConnector]
  lazy val fileUploadConnector: FileUploadConnector = wire[FileUploadConnector]
  lazy val gformConnector: GformConnector = wire[GformConnector]
  implicit lazy val mongoConnector: MongoConnector = MongoConnector(configuration.get[String]("mongodb.uri"))
  lazy val rosmConnector: RosmConnector = wire[RosmConnector]
  lazy val taxEnrolmentConnector: TaxEnrolmentConnector = wire[TaxEnrolmentConnector]
  lazy val testConnector: TestConnector = wire[TestConnector]
  lazy val serviceConfig: ServicesConfig = wire[ServicesConfig]

  val appName = configuration.get[String]("appName")
  lazy val auditingConfigProvider: AuditingConfigProvider = wire[AuditingConfigProvider]
  lazy val auditingConfig: AuditingConfig = auditingConfigProvider.get()
}
