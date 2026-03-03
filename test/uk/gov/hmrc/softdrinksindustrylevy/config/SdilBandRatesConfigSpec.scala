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

import com.typesafe.config.ConfigFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.LocalDate

class SdilBandRatesConfigSpec extends AnyWordSpec with Matchers {

  "parseBandRates" should {

    "parse valid config entries and sort by startDate" in {
      val conf = ConfigFactory.parseString(
        """
          |sdil.bandRates = [
          |  { startDate="2025-04-01", lowerBandCostPerLitre="0.194", higherBandCostPerLitre="0.259" },
          |  { startDate="2016-04-01", endDate="2025-03-31", lowerBandCostPerLitre="0.18", higherBandCostPerLitre="0.24" }
          |]
          |""".stripMargin
      )

      val rates = SdilBandRatesConfig.parseBandRates(conf)

      rates.size shouldBe 2
      rates.head.startDate shouldBe LocalDate.parse("2016-04-01")
      rates.last.startDate shouldBe LocalDate.parse("2025-04-01")
    }

    "throw when sdil.bandRates path is missing" in {
      val conf = ConfigFactory.parseString("""foo = "bar" """)
      val ex = intercept[IllegalStateException] {
        SdilBandRatesConfig.parseBandRates(conf)
      }
      ex.getMessage should include("Missing config array at 'sdil.bandRates'")
    }

    "throw when sdil.bandRates is empty" in {
      val conf = ConfigFactory.parseString("""sdil.bandRates = []""")
      val ex = intercept[IllegalStateException] {
        SdilBandRatesConfig.parseBandRates(conf)
      }
      ex.getMessage should include("'sdil.bandRates' must contain at least one entry")
    }

    "throw when endDate is before startDate" in {
      val conf = ConfigFactory.parseString(
        """
          |sdil.bandRates = [
          |  { startDate="2025-04-01", endDate="2025-03-31", lowerBandCostPerLitre="0.194", higherBandCostPerLitre="0.259" }
          |]
          |""".stripMargin
      )

      val ex = intercept[IllegalStateException] {
        SdilBandRatesConfig.parseBandRates(conf)
      }
      ex.getMessage should include("Invalid date range in 'sdil.bandRates[0]': endDate is before startDate")
    }

    "throw when a date is not yyyy-MM-dd" in {
      val conf = ConfigFactory.parseString(
        """
          |sdil.bandRates = [
          |  { startDate="01-04-2025", lowerBandCostPerLitre="0.194", higherBandCostPerLitre="0.259" }
          |]
          |""".stripMargin
      )

      val ex = intercept[IllegalStateException] {
        SdilBandRatesConfig.parseBandRates(conf)
      }
      ex.getMessage should include("Invalid date '01-04-2025'")
      ex.getMessage should include("Expected yyyy-MM-dd")
    }
  }

  "rateFor" should {

    "return the matching entry when date equals startDate (start inclusive)" in {
      val rates = Seq(
        BandRateEntry(
          LocalDate.parse("2016-04-01"),
          Some(LocalDate.parse("2025-03-31")),
          BigDecimal("0.18"),
          BigDecimal("0.24")
        ),
        BandRateEntry(LocalDate.parse("2025-04-01"), None, BigDecimal("0.194"), BigDecimal("0.259"))
      )

      val entry = SdilBandRatesConfig.rateFor(LocalDate.parse("2025-04-01"), rates)
      entry.lowerBandCostPerLitre shouldBe BigDecimal("0.194")
      entry.higherBandCostPerLitre shouldBe BigDecimal("0.259")
    }

    "return the matching entry when date equals endDate (end inclusive)" in {
      val rates = Seq(
        BandRateEntry(
          LocalDate.parse("2016-04-01"),
          Some(LocalDate.parse("2025-03-31")),
          BigDecimal("0.18"),
          BigDecimal("0.24")
        ),
        BandRateEntry(LocalDate.parse("2025-04-01"), None, BigDecimal("0.194"), BigDecimal("0.259"))
      )

      val entry = SdilBandRatesConfig.rateFor(LocalDate.parse("2025-03-31"), rates)
      entry.lowerBandCostPerLitre shouldBe BigDecimal("0.18")
      entry.higherBandCostPerLitre shouldBe BigDecimal("0.24")
    }

    "return the open-ended entry for dates after its startDate" in {
      val rates = Seq(
        BandRateEntry(
          LocalDate.parse("2016-04-01"),
          Some(LocalDate.parse("2025-03-31")),
          BigDecimal("0.18"),
          BigDecimal("0.24")
        ),
        BandRateEntry(LocalDate.parse("2025-04-01"), None, BigDecimal("0.194"), BigDecimal("0.259"))
      )

      val entry = SdilBandRatesConfig.rateFor(LocalDate.parse("2030-01-01"), rates)
      entry.lowerBandCostPerLitre shouldBe BigDecimal("0.194")
      entry.higherBandCostPerLitre shouldBe BigDecimal("0.259")
    }

    "throw when no entry matches the effective date" in {
      val rates = Seq(
        BandRateEntry(
          LocalDate.parse("2016-04-01"),
          Some(LocalDate.parse("2016-12-31")),
          BigDecimal("0.18"),
          BigDecimal("0.24")
        )
      )

      val ex = intercept[IllegalArgumentException] {
        SdilBandRatesConfig.rateFor(LocalDate.parse("2017-01-01"), rates)
      }

      ex.getMessage should include("No SDIL band rate config found for effective date 2017-01-01")
      ex.getMessage should include("Available:")
    }
  }
}
