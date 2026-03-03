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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration

import java.time.LocalDate

class SdilBandRatesConfigSpec extends AnyWordSpec with Matchers {

  "SdilBandRatesConfig" should {

    "reject config with more than one open-ended period" in {
      val conf = Configuration.from(
        Map(
          "sdil.bandRates" -> Seq(
            Map(
              "startDate"              -> "2018-04-01",
              "lowerBandCostPerLitre"  -> "0.18",
              "higherBandCostPerLitre" -> "0.24"
            ),
            Map(
              "startDate"              -> "2025-04-01",
              "lowerBandCostPerLitre"  -> "0.194",
              "higherBandCostPerLitre" -> "0.259"
            )
          )
        )
      )

      val ex = intercept[IllegalArgumentException] {
        new SdilBandRatesConfig(conf)
      }
      ex.getMessage should include("open-ended periods")
    }

    "reject overlapping periods" in {
      val conf = Configuration.from(
        Map(
          "sdil.bandRates" -> Seq(
            Map(
              "startDate"              -> "2018-04-01",
              "endDate"                -> "2025-03-31",
              "lowerBandCostPerLitre"  -> "0.18",
              "higherBandCostPerLitre" -> "0.24"
            ),
            Map(
              // overlaps because it starts on the same day the previous ends
              "startDate"              -> "2025-03-31",
              "endDate"                -> "2026-03-31",
              "lowerBandCostPerLitre"  -> "0.194",
              "higherBandCostPerLitre" -> "0.259"
            )
          )
        )
      )

      val ex = intercept[IllegalArgumentException] {
        new SdilBandRatesConfig(conf)
      }
      ex.getMessage should include("overlapping periods")
    }

    "select correct rates for valid config" in {
      val conf = Configuration.from(
        Map(
          "sdil.bandRates" -> Seq(
            Map(
              "startDate"              -> "2018-04-01",
              "endDate"                -> "2025-03-31",
              "lowerBandCostPerLitre"  -> "0.18",
              "higherBandCostPerLitre" -> "0.24"
            ),
            Map(
              "startDate"              -> "2025-04-01",
              "lowerBandCostPerLitre"  -> "0.194",
              "higherBandCostPerLitre" -> "0.259"
            )
          )
        )
      )

      val cfg = new SdilBandRatesConfig(conf)

      cfg.bandRatesFor(LocalDate.parse("2024-06-01")).lowerBandCostPerLitre shouldBe BigDecimal("0.18")
      cfg.bandRatesFor(LocalDate.parse("2025-04-01")).lowerBandCostPerLitre shouldBe BigDecimal("0.194")
      cfg.bandRatesFor(LocalDate.parse("2026-01-01")).higherBandCostPerLitre shouldBe BigDecimal("0.259")
    }
  }
}
