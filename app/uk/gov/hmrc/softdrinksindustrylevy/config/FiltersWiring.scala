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

import com.kenshoo.play.metrics.{Metrics, MetricsFilter, MetricsFilterImpl}
import uk.gov.hmrc.play.bootstrap.filters._
import uk.gov.hmrc.play.bootstrap.backend.filters._
import com.softwaremill.macwire._
import play.api.BuiltInComponentsFromContext
import uk.gov.hmrc.play.bootstrap.config.{DefaultHttpAuditEvent, HttpAuditEvent}

trait FiltersWiring {
  self: PlayWiring with ConfigWiring with ConnectorWiring with BuiltInComponentsFromContext =>

  def metrics: Metrics

  lazy val mdcFilter: MDCFilter = wire[MDCFilter]
  lazy val auditFilter: AuditFilter = wire[DefaultBackendAuditFilter]
  lazy val cacheControlFilter: CacheControlFilter = wire[CacheControlFilter]
  lazy val loggingFilter: LoggingFilter = wire[DefaultLoggingFilter]
  lazy val filters: BackendFilters = wire[BackendFilters]
  lazy val metricsFilter: MetricsFilter = wire[MetricsFilterImpl]
  lazy val httpAuditEvent: HttpAuditEvent = wire[DefaultHttpAuditEvent]
}
