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

  private val pre2025Entry = BandRateEntry(
    startDate = LocalDate.parse("2016-04-01"),
    endDate = Some(LocalDate.parse("2025-03-31")),
    lowerBandCostPerLitre = BigDecimal("0.18"),
    higherBandCostPerLitre = BigDecimal("0.24")
  )

  private val post2025Entry = BandRateEntry(
    startDate = LocalDate.parse("2025-04-01"),
    endDate = None,
    lowerBandCostPerLitre = BigDecimal("0.194"),
    higherBandCostPerLitre = BigDecimal("0.259")
  )

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

      rates shouldBe Seq(pre2025Entry, post2025Entry)
    }

    "throw when sdil.bandRates path is missing" in {
      val conf = ConfigFactory.parseString("""foo = "bar" """)

      val ex = intercept[IllegalStateException] {
        SdilBandRatesConfig.parseBandRates(conf)
      }
      ex.getMessage shouldBe "Missing config array at 'sdil.bandRates'"
    }

    "throw when sdil.bandRates is empty" in {
      val conf = ConfigFactory.parseString("""sdil.bandRates = []""")

      val ex = intercept[IllegalStateException] {
        SdilBandRatesConfig.parseBandRates(conf)
      }

      ex.getMessage shouldBe "'sdil.bandRates' must contain at least one entry"
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

      ex.getMessage shouldBe "Invalid date range in 'sdil.bandRates[0]': endDate is before startDate"
    }

    "throw when startDate is not yyyy-MM-dd" in {
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
      ex.getMessage should include("'sdil.bandRates[0].startDate'")
      ex.getMessage should include("Expected yyyy-MM-dd")
    }

    "throw when endDate is not yyyy-MM-dd" in {
      val conf = ConfigFactory.parseString(
        """
          |sdil.bandRates = [
          |  { startDate="2025-04-01", endDate="31-03-2026", lowerBandCostPerLitre="0.194", higherBandCostPerLitre="0.259" }
          |]
          |""".stripMargin
      )

      val ex = intercept[IllegalStateException] {
        SdilBandRatesConfig.parseBandRates(conf)
      }

      ex.getMessage should include("Invalid date '31-03-2026'")
      ex.getMessage should include("'sdil.bandRates[0].endDate'")
      ex.getMessage should include("Expected yyyy-MM-dd")
    }
  }

  "validateNoOverlaps" should {

    "allow adjacent non-overlapping periods" in {
      val rates = Seq(
        BandRateEntry(
          startDate = LocalDate.parse("2016-04-01"),
          endDate = Some(LocalDate.parse("2025-03-31")),
          lowerBandCostPerLitre = BigDecimal("0.18"),
          higherBandCostPerLitre = BigDecimal("0.24")
        ),
        BandRateEntry(
          startDate = LocalDate.parse("2025-04-01"),
          endDate = Some(LocalDate.parse("2026-03-31")),
          lowerBandCostPerLitre = BigDecimal("0.194"),
          higherBandCostPerLitre = BigDecimal("0.259")
        )
      )

      noException shouldBe thrownBy {
        SdilBandRatesConfig.validateNoOverlaps(rates)
      }
    }

    "throw when two configured periods overlap" in {
      val rates = Seq(
        BandRateEntry(
          startDate = LocalDate.parse("2016-04-01"),
          endDate = Some(LocalDate.parse("2025-12-31")),
          lowerBandCostPerLitre = BigDecimal("0.18"),
          higherBandCostPerLitre = BigDecimal("0.24")
        ),
        BandRateEntry(
          startDate = LocalDate.parse("2025-04-01"),
          endDate = None,
          lowerBandCostPerLitre = BigDecimal("0.194"),
          higherBandCostPerLitre = BigDecimal("0.259")
        )
      )

      val ex = intercept[IllegalStateException] {
        SdilBandRatesConfig.validateNoOverlaps(rates)
      }

      ex.getMessage should include("Overlapping SDIL band rates found")
      ex.getMessage should include("[2016-04-01..2025-12-31]")
      ex.getMessage should include("[2025-04-01..open]")
    }

    "throw when an open-ended period is followed by another later period" in {
      val rates = Seq(
        BandRateEntry(
          startDate = LocalDate.parse("2025-04-01"),
          endDate = None,
          lowerBandCostPerLitre = BigDecimal("0.194"),
          higherBandCostPerLitre = BigDecimal("0.259")
        ),
        BandRateEntry(
          startDate = LocalDate.parse("2026-04-01"),
          endDate = None,
          lowerBandCostPerLitre = BigDecimal("0.200"),
          higherBandCostPerLitre = BigDecimal("0.300")
        )
      )

      val ex = intercept[IllegalStateException] {
        SdilBandRatesConfig.validateNoOverlaps(rates)
      }

      ex.getMessage should include("is open-ended")
      ex.getMessage should include("but a later entry")
    }

    "allow a single open-ended final period" in {
      val rates = Seq(pre2025Entry, post2025Entry)

      noException shouldBe thrownBy {
        SdilBandRatesConfig.validateNoOverlaps(rates)
      }
    }
  }

  "rateFor" should {

    "return the matching entry when date equals startDate (start inclusive)" in {
      val entry = SdilBandRatesConfig.rateFor(LocalDate.parse("2025-04-01"))

      entry shouldBe post2025Entry
    }

    "return the matching entry when date equals endDate (end inclusive)" in {
      val entry = SdilBandRatesConfig.rateFor(LocalDate.parse("2025-03-31"))

      entry shouldBe pre2025Entry
    }

    "return the earlier entry for a date within the bounded range" in {
      val entry = SdilBandRatesConfig.rateFor(LocalDate.parse("2024-07-01"))

      entry shouldBe pre2025Entry
    }

    "return the open-ended entry for dates after its startDate" in {
      val entry = SdilBandRatesConfig.rateFor(LocalDate.parse("2030-01-01"))

      entry shouldBe post2025Entry
    }

    "throw when no entry matches the effective date" in {

      val ex = intercept[IllegalArgumentException] {
        SdilBandRatesConfig.rateFor(LocalDate.parse("2015-01-01"))
      }

      ex.getMessage should include("No SDIL band rate config found for effective date 2015-01-01")
      ex.getMessage should include("Available:")
    }
  }
}
