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

sealed trait TaxYear

object Pre2025 extends TaxYear
object Year2025 extends TaxYear
object Year2026 extends TaxYear

object TaxYear {
  def fromYear(year: Int): TaxYear = year match {
    case y if y < 2025 => Pre2025
    case 2025          => Year2025
    case 2026          => Year2026
    case _             => throw new IllegalArgumentException(s"Unsupported tax year: $year")
  }
}

case class BandRates(lowerBandCostPerLites: BigDecimal, higherBandCostPerLitre: BigDecimal)

case class LevyCalculation(low: BigDecimal, high: BigDecimal) {
  lazy val lowLevy = low.setScale(2, BigDecimal.RoundingMode.HALF_UP)
  lazy val highLevy = high.setScale(2, BigDecimal.RoundingMode.HALF_UP)
  lazy val total = (low + high).setScale(2, BigDecimal.RoundingMode.HALF_UP)
}

object LevyCalculator {

  // Map tax years to their corresponding band rates using the Rates object
  private val bandRatesByTaxYear: Map[TaxYear, BandRates] = Map(
    Pre2025  -> BandRates(lowerBandCostPerLitre, higherBandCostPerLitre),
    Year2025 -> BandRates(lowerBandCostPerLitrePostApril2025, higherBandCostPerLitrePostApril2025)
    // Add more years as needed
  )

  private[models] def getTaxYear(returnPeriod: ReturnPeriod): TaxYear = {
    val taxYear = returnPeriod.quarter match {
      case 0 => returnPeriod.year - 1
      case _ => returnPeriod.year
    }
    TaxYear.fromYear(taxYear)
  }

  private[models] def getBandRates(taxYear: TaxYear): BandRates =
    bandRatesByTaxYear.getOrElse(
      taxYear,
      throw new IllegalArgumentException(s"No band rates found for tax year: ${taxYear.toString}")
    )

  def getLevyCalculation(lowLitres: Long, highLitres: Long, returnPeriod: ReturnPeriod): LevyCalculation = {
    if (lowLitres < 0 || highLitres < 0) {
      throw new IllegalArgumentException("Litres cannot be negative")
    }

    val taxYear: TaxYear = getTaxYear(returnPeriod)
    val bandRates: BandRates = getBandRates(taxYear)
    val lowLevy = lowLitres * bandRates.lowerBandCostPerLites
    val highLevy = highLitres * bandRates.higherBandCostPerLitre
    LevyCalculation(lowLevy, highLevy)
  }

}
