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

import play.api.Configuration

import java.time.LocalDate
import java.time.format.DateTimeParseException
import javax.inject.{Inject, Singleton}

@Singleton
final class SdilBandRatesConfig @Inject() (configuration: Configuration) {

  private final case class RatePeriod(
    startDate: LocalDate,
    endDate: Option[LocalDate],
    rates: BandRates
  )

  // Reads the list once, validates, then sorts newest-first
  private val periods: List[RatePeriod] = {
    val raw: Seq[Configuration] = configuration.get[Seq[Configuration]]("sdil.bandRates")

    val parsed: List[RatePeriod] =
      raw.toList.map { c =>
        val start = readLocalDate(c, "startDate")
        val end = readOptionalLocalDate(c, "endDate")

        val lower = BigDecimal(c.get[String]("lowerBandCostPerLitre"))
        val higher = BigDecimal(c.get[String]("higherBandCostPerLitre"))

        RatePeriod(
          startDate = start,
          endDate = end,
          rates = BandRates(lower, higher)
        )
      }

    validate(parsed)

    parsed.sortBy(_.startDate)(using Ordering[LocalDate].reverse)
  }

  /** Find rates for a given date: startDate <= date AND (endDate absent OR date <= endDate) */
  def bandRatesFor(date: LocalDate): BandRates =
    periods
      .find(p => !date.isBefore(p.startDate) && p.endDate.forall(e => !date.isAfter(e)))
      .map(_.rates)
      .getOrElse {
        throw new IllegalArgumentException(
          s"No SDIL band rates configured for date $date. Check sdil.bandRates start/end dates."
        )
      }

  private def validate(ps: List[RatePeriod]): Unit = {
    if (ps.isEmpty)
      throw new IllegalArgumentException("sdil.bandRates must contain at least one rate period")

    val openEndedCount = ps.count(_.endDate.isEmpty)
    if (openEndedCount > 1)
      throw new IllegalArgumentException(
        s"Invalid sdil.bandRates: found $openEndedCount open-ended periods (endDate missing). Only one is allowed."
      )

    val asc = ps.sortBy(_.startDate)
    asc.zip(asc.drop(1)).foreach { case (a, b) =>
      a.endDate match {
        case None =>
          throw new IllegalArgumentException(
            s"Invalid sdil.bandRates: open-ended period starting ${a.startDate} overlaps period starting ${b.startDate}"
          )
        case Some(aEnd) =>
          if (!b.startDate.isAfter(aEnd)) {
            throw new IllegalArgumentException(
              s"Invalid sdil.bandRates: overlapping periods. " +
                s"Period ${a.startDate}..$aEnd overlaps ${b.startDate}..${b.endDate.map(_.toString).getOrElse("open-ended")}"
            )
          }
      }
    }
  }

  private def readLocalDate(c: Configuration, key: String): LocalDate = {
    val raw = c.get[String](key)
    try LocalDate.parse(raw)
    catch {
      case _: DateTimeParseException =>
        throw new IllegalArgumentException(s"Invalid date for sdil.bandRates.$key: '$raw' (expected yyyy-MM-dd)")
    }
  }

  private def readOptionalLocalDate(c: Configuration, key: String): Option[LocalDate] =
    c.getOptional[String](key).map { raw =>
      try LocalDate.parse(raw)
      catch {
        case _: DateTimeParseException =>
          throw new IllegalArgumentException(s"Invalid date for sdil.bandRates.$key: '$raw' (expected yyyy-MM-dd)")
      }
    }
}
