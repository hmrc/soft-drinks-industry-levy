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

import com.typesafe.config.{Config, ConfigFactory}

import java.time.LocalDate

final case class BandRateEntry(
                                startDate: LocalDate,
                                endDate: Option[LocalDate],
                                lowerBandCostPerLitre: BigDecimal,
                                higherBandCostPerLitre: BigDecimal
                              )

object SdilBandRatesConfig {

  private val path = "sdil.bandRates"
  private lazy val config: Config = ConfigFactory.load()

  lazy val bandRates: Seq[BandRateEntry] = {
    val parsed = parseBandRates(config)
    validateNoOverlaps(parsed)
    parsed
  }

  private[config] def parseBandRates(conf: Config): Seq[BandRateEntry] = {
    if (!conf.hasPath(path)) {
      throw new IllegalStateException(s"Missing config array at '$path'")
    }

    import scala.jdk.CollectionConverters._

    val parsed = conf.getConfigList(path).asScala.zipWithIndex.map { case (c, idx) =>
      val startDateStr = c.getString("startDate")
      val startDate = parseDate(startDateStr, s"$path[$idx].startDate")

      val endDate =
        if (c.hasPath("endDate")) Some(parseDate(c.getString("endDate"), s"$path[$idx].endDate"))
        else None

      val lowerRate = BigDecimal(c.getString("lowerBandCostPerLitre"))
      val higherRate = BigDecimal(c.getString("higherBandCostPerLitre"))

      if (endDate.exists(_.isBefore(startDate))) {
        throw new IllegalStateException(s"Invalid date range in '$path[$idx]': endDate is before startDate")
      }

      BandRateEntry(
        startDate = startDate,
        endDate = endDate,
        lowerBandCostPerLitre = lowerRate,
        higherBandCostPerLitre = higherRate
      )
    }.toSeq.sortBy(_.startDate)

    if (parsed.isEmpty) {
      throw new IllegalStateException(s"'$path' must contain at least one entry")
    }

    parsed
  }

  private[config] def validateNoOverlaps(rates: Seq[BandRateEntry]): Unit = {
    rates.sliding(2).zipWithIndex.foreach {
      case (Seq(current, next), idx) =>
        current.endDate match {
          case Some(currentEnd) =>
            if (!next.startDate.isAfter(currentEnd)) {
              throw new IllegalStateException(
                s"Overlapping SDIL band rates found between '$path[$idx]' and '$path[${idx + 1}]': " +
                  s"[${current.startDate}..$currentEnd] overlaps with " +
                  s"[${next.startDate}..${next.endDate.getOrElse("open")}]"
              )
            }
          case None =>
            throw new IllegalStateException(
              s"Invalid SDIL band rate configuration: '$path[$idx]' is open-ended " +
                s"but a later entry '$path[${idx + 1}]' also exists"
            )
        }

      case _ => ()
    }
  }

  def rateFor(date: LocalDate): BandRateEntry = {
    val matches = bandRates.filter { entry =>
      !date.isBefore(entry.startDate) && entry.endDate.forall(!date.isAfter(_))
    }

    matches.lastOption.getOrElse {
      val available = bandRates.map(e => s"[${e.startDate}..${e.endDate.getOrElse("open")}]").mkString(", ")
      throw new IllegalArgumentException(
        s"No SDIL band rate config found for effective date $date. Available: $available"
      )
    }
  }

  private[config] def parseDate(value: String, path: String): LocalDate =
    try LocalDate.parse(value)
    catch {
      case e: Exception =>
        throw new IllegalStateException(s"Invalid date '$value' at '$path'. Expected yyyy-MM-dd", e)
    }
}