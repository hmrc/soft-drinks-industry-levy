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

package uk.gov.hmrc.softdrinksindustrylevy.models

import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

case class LitreOps(litreBands: LitreBands)(implicit servicesConfig: ServicesConfig) {
  lazy val lowerBandCostPerLitre: BigDecimal = BigDecimal(servicesConfig.getString("lowerBandCostPerLitre"))
  lazy val higherBandCostPerLitre: BigDecimal = BigDecimal(servicesConfig.getString("higherBandCostPerLitre"))
  lazy val lowLevy: BigDecimal = litreBands._1 * BigDecimal("0.18")
  lazy val highLevy: BigDecimal = litreBands._2 * BigDecimal("0.24")
  lazy val dueLevy: BigDecimal = lowLevy + highLevy
}
