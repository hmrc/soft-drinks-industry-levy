/*
 * Copyright 2026 HM Revenue & Customs
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

import com.typesafe.config.{Config, ConfigObject}
import javax.inject.{Inject, Singleton}

import java.time.LocalDate
import scala.jdk.CollectionConverters.*

@Singleton
final class SdilBandRatesConfig @Inject() (config: Config) {

  private final case class RatePeriod(
    startDate: LocalDate,
    endDate: Option[LocalDate],
    rates: BandRates
  ) {
    def contains(date: LocalDate): Boolean =
      !date.isBefore(startDate) && endDate.forall(end => !date.isAfter(end))
  }

  private val periods: List[RatePeriod] = {

    val raw: List[ConfigObject] =
      config.getObjectList("sdil.bandRates").asScala.toList

    val parsed: List[RatePeriod] =
      raw.map { obj =>
        val c = obj.toConfig

        val start = LocalDate.parse(c.getString("startDate"))
        val end =
          if (c.hasPath("endDate")) Some(LocalDate.parse(c.getString("endDate")))
          else None

        val lower = BigDecimal(c.getString("lowerBandCostPerLitre"))
        val higher = BigDecimal(c.getString("higherBandCostPerLitre"))

        RatePeriod(
          startDate = start,
          endDate = end,
          rates = BandRates(lower, higher)
        )
      }

    parsed.sortBy(_.startDate)
  }

  def bandRatesFor(date: LocalDate): BandRates =
    periods.find(_.contains(date)).map(_.rates).getOrElse {
      throw new IllegalArgumentException(
        s"No SDIL band rates configured for date $date. Check sdil.bandRates start/end dates."
      )
    }
}
