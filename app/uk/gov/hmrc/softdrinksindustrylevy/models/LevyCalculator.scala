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

import sdil.models.ReturnPeriod
import uk.gov.hmrc.softdrinksindustrylevy.config.Rates._

case class BandRates(lowerBandCostPerLites: BigDecimal, higherBandCostPerLitre: BigDecimal)

case class LevyCalculation(low: BigDecimal, high: BigDecimal) {
  lazy val lowLevy = low.setScale(2)
  lazy val highLevy = high.setScale(2)
  lazy val total = (low + high).setScale(2)
}

object LevyCalculator {

  private[models] def getTaxYear(returnPeriod: ReturnPeriod): Int = {
    returnPeriod.quarter match {
      case 0 => returnPeriod.year - 1
      case _ => returnPeriod.year
    }
  }

  private[models] def getBandRates(taxYear: Int): BandRates = {
    taxYear match {
      case year if year < 2025 => BandRates(lowerBandCostPerLitre, higherBandCostPerLitre)
      case 2025 => BandRates(lowerBandCostPerLitrePostApril2025, higherBandCostPerLitrePostApril2025)
//      case 2026 => BandRates(lowerBandCostPerLitrePostApril2026, higherBandCostPerLitrePostApril2026)
    }
  }

  def getLevyCalculation(lowLitres: Long, highLitres: Long, returnPeriod: ReturnPeriod): LevyCalculation = {
    val taxYear: Int = getTaxYear(returnPeriod)
    val bandRates: BandRates = getBandRates(taxYear)
    val lowLevy = lowLitres * bandRates.lowerBandCostPerLites
    val highLevy = highLitres * bandRates.higherBandCostPerLitre
    LevyCalculation(lowLevy, highLevy)
  }

}
