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

import play.api.Logging
import sdil.models.ReturnPeriod
import uk.gov.hmrc.softdrinksindustrylevy.config.SdilBandRatesConfig

import java.time.LocalDate

case class BandRates(lowerBandCostPerLitre: BigDecimal, higherBandCostPerLitre: BigDecimal)

case class LevyCalculation(low: BigDecimal, high: BigDecimal) {
  lazy val lowLevy = low.setScale(2, BigDecimal.RoundingMode.HALF_UP)
  lazy val highLevy = high.setScale(2, BigDecimal.RoundingMode.HALF_UP)
  lazy val total = (low + high).setScale(2, BigDecimal.RoundingMode.HALF_UP)
  lazy val totalRoundedDown = (low + high).setScale(2, BigDecimal.RoundingMode.DOWN)
}

object LevyCalculator extends Logging {

  def getBandRates(returnPeriod: ReturnPeriod): BandRates = {
    val effectiveDate = returnPeriod.start
    val entry = SdilBandRatesConfig.rateFor(effectiveDate)
    BandRates(entry.lowerBandCostPerLitre, entry.higherBandCostPerLitre)
  }

  def getLevyCalculation(lowLitres: Long, highLitres: Long, returnPeriod: ReturnPeriod): LevyCalculation = {
    val rates = getBandRates(returnPeriod)

    val lowLevy = BigDecimal(lowLitres) * rates.lowerBandCostPerLitre
    val highLevy = BigDecimal(highLitres) * rates.higherBandCostPerLitre

    logger.info(
      s"getLevyCalculation called with returnPeriod year=${returnPeriod.year} quarter=${returnPeriod.quarter} " +
        s"effectiveDate=${returnPeriod.start} using lower=${rates.lowerBandCostPerLitre} higher=${rates.higherBandCostPerLitre}"
    )
    LevyCalculation(lowLevy, highLevy)
  }

}
