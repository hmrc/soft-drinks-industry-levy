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
import uk.gov.hmrc.softdrinksindustrylevy.services._

trait ServicesWiring {
  self: ConnectorWiring with PlayWiring =>

  lazy val mongoBufferService: MongoBufferService = wire[MongoBufferService]
  lazy val variationSubmissionService: VariationSubmissionService = wire[VariationSubmissionService]
  lazy val returnsVariationSubmissionService: ReturnsVariationSubmissionService =
    wire[ReturnsVariationSubmissionService]
  lazy val returnsAdjustmentSubmissionService: ReturnsAdjustmentSubmissionService =
    wire[ReturnsAdjustmentSubmissionService]
}
