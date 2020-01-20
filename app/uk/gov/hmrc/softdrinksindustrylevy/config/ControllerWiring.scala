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

import com.kenshoo.play.metrics.{Metrics, MetricsController, MetricsImpl}
import com.softwaremill.macwire._
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.health.HealthController
import uk.gov.hmrc.softdrinksindustrylevy.controllers._
import uk.gov.hmrc.softdrinksindustrylevy.controllers.test.TestingController

trait ControllerWiring {
  self: ConnectorWiring with PlayWiring with ServicesWiring =>

  def authConnector: AuthConnector

  lazy val metrics: Metrics = wire[MetricsImpl]

  lazy val healthController: HealthController = wire[HealthController]
  lazy val metricsController: MetricsController = wire[MetricsController]
  lazy val rosmController: RosmController = wire[RosmController]
  lazy val sdilController: RegistrationController = wire[RegistrationController]
  lazy val taxEnrolmentCallbackController: TaxEnrolmentCallbackController = wire[TaxEnrolmentCallbackController]
  lazy val testingController: TestingController = wire[TestingController]
  lazy val variationsController: VariationsController = wire[VariationsController]
  lazy val returnsController: ReturnsController = wire[ReturnsController]
  lazy val balanceController: BalanceController = wire[BalanceController]
  val cc: ControllerComponents
}
