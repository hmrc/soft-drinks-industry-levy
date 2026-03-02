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

import org.scalacheck.Gen
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import sdil.models.ReturnPeriod
import uk.gov.hmrc.softdrinksindustrylevy.config.{AppConfig, AppConfigHolder, BandRates, SdilBandRatesConfig}
import uk.gov.hmrc.softdrinksindustrylevy.models.LevyCalculator.*
import uk.gov.hmrc.softdrinksindustrylevy.models.TaxRateUtil.*
import uk.gov.hmrc.softdrinksindustrylevy.util.WithBandRatesConfig

import java.time.LocalDate

class LevyCalculatorSpec extends AnyWordSpec with Matchers with ScalaCheckPropertyChecks with WithBandRatesConfig {

  "getTaxYear" should {
    (2018 to 2024).foreach { year =>
      s"return $year when in April - December $year" in {
        forAll(aprToDecInt) { month =>
          val returnPeriod = ReturnPeriod(LocalDate.of(year, month, 1))
          getTaxYear(returnPeriod) shouldBe year
        }
      }

      s"return $year when in January - March ${year + 1}" in {
        forAll(janToMarInt) { month =>
          val returnPeriod = ReturnPeriod(LocalDate.of(year + 1, month, 1))
          getTaxYear(returnPeriod) shouldBe year
        }
      }
    }

    "return 2025 when in April - December 2025" in {
      forAll(aprToDecInt) { month =>
        val returnPeriod = ReturnPeriod(LocalDate.of(2025, month, 1))
        getTaxYear(returnPeriod) shouldBe 2025
      }
    }

    "return 2025 when in January - March 2026" in {
      forAll(janToMarInt) { month =>
        val returnPeriod = ReturnPeriod(LocalDate.of(2026, month, 1))
        getTaxYear(returnPeriod) shouldBe 2025
      }
    }

    "return 2026 when in April - December 2026" in {
      forAll(aprToDecInt) { month =>
        val returnPeriod = ReturnPeriod(LocalDate.of(2026, month, 1))
        getTaxYear(returnPeriod) shouldBe 2026
      }
    }

    "return 2026 when in January - March 2027" in {
      forAll(janToMarInt) { month =>
        val returnPeriod = ReturnPeriod(LocalDate.of(2027, month, 1))
        getTaxYear(returnPeriod) shouldBe 2026
      }
    }
  }

  "getBandRates" should {

    (2018 to 2024).foreach { taxYear =>
      s"return 0.18 for lower band when tax year is $taxYear" in {
        val bandRates: BandRates = getBandRates(taxYear)
        bandRates.lowerBandCostPerLitre shouldBe BigDecimal("0.18")
      }

      s"return 0.24 for higher band when tax year is $taxYear" in {
        val bandRates: BandRates = getBandRates(taxYear)
        bandRates.higherBandCostPerLitre shouldBe BigDecimal("0.24")
      }
    }

    "return 0.194 for lower band when tax year is 2025" in {
      val bandRates: BandRates = getBandRates(2025)
      bandRates.lowerBandCostPerLitre shouldBe BigDecimal("0.194")
    }

    "return 0.259 for higher band when tax year is 2025" in {
      val bandRates: BandRates = getBandRates(2025)
      bandRates.higherBandCostPerLitre shouldBe BigDecimal("0.259")
    }
  }

  "getLevyCalculation" should {

    val smallPosInts = Gen.choose(0, 1000)
    val largePosInts = Gen.choose(1000, 10000000)

    (2018 to 2024).foreach { year =>
      s"calculate low levy, high levy, and total correctly with zero litres totals using original rates for Apr - Dec $year" in {
        forAll(aprToDecInt) { month =>
          val returnPeriod = ReturnPeriod(LocalDate.of(year, month, 1))
          val levyCalculation = getLevyCalculation(0, 0, returnPeriod)
          levyCalculation.lowLevy shouldBe BigDecimal("0.00")
          levyCalculation.highLevy shouldBe BigDecimal("0.00")
          levyCalculation.total shouldBe BigDecimal("0.00")
        }
      }

      s"calculate low levy, high levy, and total correctly with small litres totals using original rates for Apr - Dec $year" in {
        forAll(smallPosInts) { lowLitres =>
          forAll(smallPosInts) { highLitres =>
            forAll(aprToDecInt) { month =>
              val returnPeriod = ReturnPeriod(LocalDate.of(year, month, 1))
              val levyCalculation = getLevyCalculation(lowLitres, highLitres, returnPeriod)
              val expectedLowLevy = lowerBandCostPerLitre * lowLitres
              val expectedHighLevy = higherBandCostPerLitre * highLitres
              levyCalculation.lowLevy shouldBe expectedLowLevy
              levyCalculation.highLevy shouldBe expectedHighLevy
              levyCalculation.total shouldBe expectedLowLevy + expectedHighLevy
            }
          }
        }
      }

      s"calculate low levy, high levy, and total correctly with large litres totals using original rates for Apr - Dec $year" in {
        forAll(largePosInts) { lowLitres =>
          forAll(largePosInts) { highLitres =>
            forAll(aprToDecInt) { month =>
              val returnPeriod = ReturnPeriod(LocalDate.of(year, month, 1))
              val levyCalculation = getLevyCalculation(lowLitres, highLitres, returnPeriod)
              val expectedLowLevy = lowerBandCostPerLitre * lowLitres
              val expectedHighLevy = higherBandCostPerLitre * highLitres
              levyCalculation.lowLevy shouldBe expectedLowLevy
              levyCalculation.highLevy shouldBe expectedHighLevy
              levyCalculation.total shouldBe expectedLowLevy + expectedHighLevy
            }
          }
        }
      }

      s"calculate low levy, high levy, and total correctly with zero litres totals using original rates for Jan - Mar ${year + 1}" in {
        forAll(janToMarInt) { month =>
          val returnPeriod = ReturnPeriod(LocalDate.of(year + 1, month, 1))
          val levyCalculation = getLevyCalculation(0, 0, returnPeriod)
          levyCalculation.lowLevy shouldBe BigDecimal("0.00")
          levyCalculation.highLevy shouldBe BigDecimal("0.00")
          levyCalculation.total shouldBe BigDecimal("0.00")
        }
      }

      s"calculate low levy, high levy, and total correctly with small litres totals using original rates for Jan - Mar ${year + 1}" in {
        forAll(smallPosInts) { lowLitres =>
          forAll(smallPosInts) { highLitres =>
            forAll(janToMarInt) { month =>
              val returnPeriod = ReturnPeriod(LocalDate.of(year + 1, month, 1))
              val levyCalculation = getLevyCalculation(lowLitres, highLitres, returnPeriod)
              val expectedLowLevy = lowerBandCostPerLitre * lowLitres
              val expectedHighLevy = higherBandCostPerLitre * highLitres
              levyCalculation.lowLevy shouldBe expectedLowLevy
              levyCalculation.highLevy shouldBe expectedHighLevy
              levyCalculation.total shouldBe expectedLowLevy + expectedHighLevy
            }
          }
        }
      }

      s"calculate low levy, high levy, and total correctly with large litres totals using original rates for Jan - Mar ${year + 1}" in {
        forAll(largePosInts) { lowLitres =>
          forAll(largePosInts) { highLitres =>
            forAll(janToMarInt) { month =>
              val returnPeriod = ReturnPeriod(LocalDate.of(year + 1, month, 1))
              val levyCalculation = getLevyCalculation(lowLitres, highLitres, returnPeriod)
              val expectedLowLevy = lowerBandCostPerLitre * lowLitres
              val expectedHighLevy = higherBandCostPerLitre * highLitres
              levyCalculation.lowLevy shouldBe expectedLowLevy
              levyCalculation.highLevy shouldBe expectedHighLevy
              levyCalculation.total shouldBe expectedLowLevy + expectedHighLevy
            }
          }
        }
      }
    }

    (2025 to 2025).foreach { year =>
      s"calculate low levy, high levy, and total correctly with zero litres totals using $year rates for Apr - Dec $year" in {
        forAll(aprToDecInt) { month =>
          val returnPeriod = ReturnPeriod(LocalDate.of(year, month, 1))
          val levyCalculation = getLevyCalculation(0, 0, returnPeriod)
          levyCalculation.lowLevy shouldBe BigDecimal("0.00")
          levyCalculation.highLevy shouldBe BigDecimal("0.00")
          levyCalculation.total shouldBe BigDecimal("0.00")
        }
      }

      s"calculate low levy, high levy, and total correctly with small litres totals using $year rates for Apr - Dec $year" in {
        forAll(aprToDecInt) { month =>
          forAll(smallPosInts) { lowLitres =>
            forAll(smallPosInts) { highLitres =>
              val returnPeriod = ReturnPeriod(LocalDate.of(year, month, 1))
              val levyCalculation = getLevyCalculation(lowLitres, highLitres, returnPeriod)
              val expectedLowLevy = lowerBandCostPerLitreMap(year) * lowLitres
              val expectedHighLevy = higherBandCostPerLitreMap(year) * highLitres
              levyCalculation.lowLevy shouldBe expectedLowLevy.setScale(2, BigDecimal.RoundingMode.HALF_UP)
              levyCalculation.highLevy shouldBe expectedHighLevy.setScale(2, BigDecimal.RoundingMode.HALF_UP)
              levyCalculation.total shouldBe (expectedLowLevy + expectedHighLevy)
                .setScale(2, BigDecimal.RoundingMode.HALF_UP)
            }
          }
        }
      }

      s"calculate low levy, high levy, and total correctly with large litres totals using $year rates for Apr - Dec $year" in {
        forAll(aprToDecInt) { month =>
          forAll(largePosInts) { lowLitres =>
            forAll(largePosInts) { highLitres =>
              val returnPeriod = ReturnPeriod(LocalDate.of(year, month, 1))
              val levyCalculation = getLevyCalculation(lowLitres, highLitres, returnPeriod)
              val expectedLowLevy = lowerBandCostPerLitreMap(year) * lowLitres
              val expectedHighLevy = higherBandCostPerLitreMap(year) * highLitres
              levyCalculation.lowLevy shouldBe expectedLowLevy.setScale(2, BigDecimal.RoundingMode.HALF_UP)
              levyCalculation.highLevy shouldBe expectedHighLevy.setScale(2, BigDecimal.RoundingMode.HALF_UP)
              levyCalculation.total shouldBe (expectedLowLevy + expectedHighLevy)
                .setScale(2, BigDecimal.RoundingMode.HALF_UP)
            }
          }
        }
      }

      s"calculate low levy, high levy, and total correctly with zero litres totals using $year rates for Jan - Mar ${year + 1}" in {
        forAll(janToMarInt) { month =>
          val returnPeriod = ReturnPeriod(LocalDate.of(year + 1, month, 1))
          val levyCalculation = getLevyCalculation(0, 0, returnPeriod)
          levyCalculation.lowLevy shouldBe BigDecimal("0.00")
          levyCalculation.highLevy shouldBe BigDecimal("0.00")
          levyCalculation.total shouldBe BigDecimal("0.00")
        }
      }

      s"calculate low levy, high levy, and total correctly with small litres totals using $year rates for Jan - Mar ${year + 1}" in {
        forAll(janToMarInt) { month =>
          forAll(smallPosInts) { lowLitres =>
            forAll(smallPosInts) { highLitres =>
              val returnPeriod = ReturnPeriod(LocalDate.of(year + 1, month, 1))
              val levyCalculation = getLevyCalculation(lowLitres, highLitres, returnPeriod)
              val expectedLowLevy = lowerBandCostPerLitreMap(year) * lowLitres
              val expectedHighLevy = higherBandCostPerLitreMap(year) * highLitres
              levyCalculation.lowLevy shouldBe expectedLowLevy.setScale(2, BigDecimal.RoundingMode.HALF_UP)
              levyCalculation.highLevy shouldBe expectedHighLevy.setScale(2, BigDecimal.RoundingMode.HALF_UP)
              levyCalculation.total shouldBe (expectedLowLevy + expectedHighLevy)
                .setScale(2, BigDecimal.RoundingMode.HALF_UP)
            }
          }
        }
      }

      s"calculate low levy, high levy, and total correctly with large litres totals using $year rates for Jan - Mar ${year + 1}" in {
        forAll(janToMarInt) { month =>
          forAll(largePosInts) { lowLitres =>
            forAll(largePosInts) { highLitres =>
              val returnPeriod = ReturnPeriod(LocalDate.of(year + 1, month, 1))
              val levyCalculation = getLevyCalculation(lowLitres, highLitres, returnPeriod)
              val expectedLowLevy = lowerBandCostPerLitreMap(year) * lowLitres
              val expectedHighLevy = higherBandCostPerLitreMap(year) * highLitres
              levyCalculation.lowLevy shouldBe expectedLowLevy.setScale(2, BigDecimal.RoundingMode.HALF_UP)
              levyCalculation.highLevy shouldBe expectedHighLevy.setScale(2, BigDecimal.RoundingMode.HALF_UP)
              levyCalculation.total shouldBe (expectedLowLevy + expectedHighLevy)
                .setScale(2, BigDecimal.RoundingMode.HALF_UP)
            }
          }
        }
      }

    }

  }

}
