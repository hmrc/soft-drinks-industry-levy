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

package uk.gov.hmrc.softdrinksindustrylevy.config

import sdil.models.ReturnPeriod

case class BandRates(lowerBandCostPerLites: BigDecimal, higherBandCostPerLitre: BigDecimal)

case class LevyCalculation(lowLevy: BigDecimal, highLevy: BigDecimal) {
  lazy val total = lowLevy + highLevy
}

object Rates {

  val lowerBandCostPerLitreString: String = "0.18"
  val higherBandCostPerLitreString: String = "0.24"

  val lowerBandCostPerLitrePostApril2025String: String = "0.194"
  val higherBandCostPerLitrePostApril2025String: String = "0.259"

  val lowerBandCostPerLitre: BigDecimal = BigDecimal(lowerBandCostPerLitreString)
  val higherBandCostPerLitre: BigDecimal = BigDecimal(higherBandCostPerLitreString)

  val lowerBandCostPerLitrePostApril2025: BigDecimal = BigDecimal(lowerBandCostPerLitrePostApril2025String)
  val higherBandCostPerLitrePostApril2025: BigDecimal = BigDecimal(higherBandCostPerLitrePostApril2025String)

  def getTaxYear(returnPeriod: ReturnPeriod): Int = {
    returnPeriod.quarter match {
      case 1 => returnPeriod.year - 1
      case _ => returnPeriod.year
    }
  }

  def getBandRates(returnPeriod: ReturnPeriod): BandRates = {
    getTaxYear(returnPeriod) match {
//      case 2026 => BandRates(lowerBandCostPerLitrePostApril2026, higherBandCostPerLitrePostApril2026)
      case 2025 => BandRates(lowerBandCostPerLitrePostApril2025, higherBandCostPerLitrePostApril2025)
      case _ => BandRates(lowerBandCostPerLitre, higherBandCostPerLitre)
    }
  }

  def getLevyCalculation(lowLitres: Long, highLitres: Long, returnPeriod: ReturnPeriod): LevyCalculation = {
    val bandRates: BandRates = getBandRates(returnPeriod)
    val lowLevy = lowLitres * bandRates.lowerBandCostPerLites
    val highLevy = highLitres * bandRates.higherBandCostPerLitre
    LevyCalculation(lowLevy, highLevy)
  }

}
