/*
 * Copyright 2023 HM Revenue & Customs
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
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import sdil.models.ReturnPeriod
import uk.gov.hmrc.softdrinksindustrylevy.models.LevyCalculator._

import java.time.LocalDate

class LevyCalculatorSpec extends AnyWordSpec with Matchers with ScalaCheckPropertyChecks with MockitoSugar {
  "getTaxYear" should {
//    TODO: Implement tax year using yearGen
    val yearGen = Gen.choose(2018, 2025)
    "return numeric value of year - 1 when in January" in {
      val year = 2024
      val returnPeriod = ReturnPeriod.apply(LocalDate.of(year, 1, 1))
      getTaxYear(returnPeriod) shouldBe year - 1
    }

    "return numeric value of year - 1 when in February" in {
      val year = 2024
      val returnPeriod = ReturnPeriod.apply(LocalDate.of(year, 2, 1))
      getTaxYear(returnPeriod) shouldBe year - 1
    }

    "return numeric value of year - 1 when in March" in {
      val year = 2024
      val returnPeriod = ReturnPeriod.apply(LocalDate.of(year, 3, 1))
      getTaxYear(returnPeriod) shouldBe year - 1
    }

    "return numeric value of year when in April" in {
      val year = 2024
      val returnPeriod = ReturnPeriod.apply(LocalDate.of(year, 4, 1))
      getTaxYear(returnPeriod) shouldBe year
    }

    "return numeric value of year when in May" in {
      val year = 2024
      val returnPeriod = ReturnPeriod.apply(LocalDate.of(year, 4, 1))
      getTaxYear(returnPeriod) shouldBe year
    }

    "return numeric value of year when in June" in {
      val year = 2024
      val returnPeriod = ReturnPeriod.apply(LocalDate.of(year, 4, 1))
      getTaxYear(returnPeriod) shouldBe year
    }

    "return numeric value of year when in July" in {
      val year = 2024
      val returnPeriod = ReturnPeriod.apply(LocalDate.of(year, 4, 1))
      getTaxYear(returnPeriod) shouldBe year
    }

    "return numeric value of year when in August" in {
      val year = 2024
      val returnPeriod = ReturnPeriod.apply(LocalDate.of(year, 4, 1))
      getTaxYear(returnPeriod) shouldBe year
    }

    "return numeric value of year when in September" in {
      val year = 2024
      val returnPeriod = ReturnPeriod.apply(LocalDate.of(year, 4, 1))
      getTaxYear(returnPeriod) shouldBe year
    }

    "return numeric value of year when in October" in {
      val year = 2024
      val returnPeriod = ReturnPeriod.apply(LocalDate.of(year, 4, 1))
      getTaxYear(returnPeriod) shouldBe year
    }

    "return numeric value of year when in November" in {
      val year = 2024
      val returnPeriod = ReturnPeriod.apply(LocalDate.of(year, 4, 1))
      getTaxYear(returnPeriod) shouldBe year
    }

    "return numeric value of year when in December" in {
      val year = 2024
      val returnPeriod = ReturnPeriod.apply(LocalDate.of(year, 4, 1))
      getTaxYear(returnPeriod) shouldBe year
    }
  }

  "getBandRates" should {
    (2018 to 2024).foreach(taxYear => {
      val bandRates: BandRates = getBandRates(taxYear)

      s"return 0.18 for lower band when tax year is $taxYear" in {
        bandRates.lowerBandCostPerLites shouldBe BigDecimal("0.18")
      }

      s"return 0.24 for higher band when tax year is $taxYear" in {
        bandRates.higherBandCostPerLitre shouldBe BigDecimal("0.24")
      }
    })

    "return 0.194 for lower band when tax year is 2025" in {
      val bandRates: BandRates = getBandRates(2025)
      bandRates.lowerBandCostPerLites shouldBe BigDecimal("0.194")
    }

    "return 0.259 for higher band when tax year is 2025" in {
      val bandRates: BandRates = getBandRates(2025)
      bandRates.higherBandCostPerLitre shouldBe BigDecimal("0.259")
    }
  }

  "getLevyCalculation" should {
    // TODO: IMPLEMENT GEN TESTS CORRECTLY
    val lowPosInts = Gen.choose(0, 1000)
    val highPosInts = Gen.choose(1000, 10000000)
    val janToFebInt = Gen.choose(1, 3)
    val aprToDecInt = Gen.choose(4, 12)

    (2018 to 2024).foreach(taxYear => {
      val bandRates: BandRates = getBandRates(taxYear)

      s"calculate low levy correctly with low litres totals using original rates for Apr - Dec $taxYear" in {
        true shouldBe false
      }

      s"calculate high levy correctly with low litres totals using original rates for Apr - Dec $taxYear" in {
        true shouldBe false
      }

      s"calculate total correctly using with low litres totals original rates for Apr - Dec $taxYear" in {
        true shouldBe false
      }

      s"calculate low levy correctly with high litres totals using original rates for Apr - Dec $taxYear" in {
        true shouldBe false
      }

      s"calculate high levy correctly with high litres totals using original rates for Apr - Dec $taxYear" in {
        true shouldBe false
      }

      s"calculate total correctly using with high litres totals original rates for Apr - Dec $taxYear" in {
        true shouldBe false
      }

      s"calculate low levy correctly with low litres totals using original rates for Jan - Mar ${taxYear + 1}" in {
        true shouldBe false
      }

      s"calculate high levy correctly with low litres totals using original rates for Jan - Mar ${taxYear + 1}" in {
        true shouldBe false
      }

      s"calculate total correctly using with low litres totals original rates for Jan - Mar ${taxYear + 1}" in {
        true shouldBe false
      }

      s"calculate low levy correctly with high litres totals using original rates for Jan - Mar ${taxYear + 1}" in {
        true shouldBe false
      }

      s"calculate high levy correctly with high litres totals using original rates for Jan - Mar ${taxYear + 1}" in {
        true shouldBe false
      }

      s"calculate total correctly using with high litres totals original rates for Jan - Mar ${taxYear + 1}" in {
        true shouldBe false
      }
    })

    (2025 to 2025).foreach(taxYear => {
      val bandRates: BandRates = getBandRates(taxYear)

      s"calculate low levy correctly with low litres totals using $taxYear rates for Apr - Dec $taxYear" in {
        true shouldBe false
      }

      s"calculate high levy correctly with low litres totals using $taxYear rates for Apr - Dec $taxYear" in {
        true shouldBe false
      }

      s"calculate total correctly using with low litres totals $taxYear rates for Apr - Dec $taxYear" in {
        true shouldBe false
      }

      s"calculate low levy correctly with high litres totals using $taxYear rates for Apr - Dec $taxYear" in {
        true shouldBe false
      }

      s"calculate high levy correctly with high litres totals using $taxYear rates for Apr - Dec $taxYear" in {
        true shouldBe false
      }

      s"calculate total correctly using with high litres totals $taxYear rates for Apr - Dec $taxYear" in {
        true shouldBe false
      }

      s"calculate low levy correctly with low litres totals using $taxYear rates for Jan - Mar ${taxYear + 1}" in {
        true shouldBe false
      }

      s"calculate high levy correctly with low litres totals using $taxYear rates for Jan - Mar ${taxYear + 1}" in {
        true shouldBe false
      }

      s"calculate total correctly using with low litres totals $taxYear rates for Jan - Mar ${taxYear + 1}" in {
        true shouldBe false
      }

      s"calculate low levy correctly with high litres totals using $taxYear rates for Jan - Mar ${taxYear + 1}" in {
        true shouldBe false
      }

      s"calculate high levy correctly with high litres totals using $taxYear rates for Jan - Mar ${taxYear + 1}" in {
        true shouldBe false
      }

      s"calculate total correctly using with high litres totals $taxYear rates for Jan - Mar ${taxYear + 1}" in {
        true shouldBe false
      }
    })

  }

}
