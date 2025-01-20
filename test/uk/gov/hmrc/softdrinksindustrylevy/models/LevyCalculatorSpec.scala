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
    val yearGen = Gen.choose(2018, 2026)

    forAll(yearGen) { year =>
      "return numeric value of year - 1 when in January" in {
        val returnPeriod = ReturnPeriod.apply(LocalDate.of(year, 1, 1))
        getTaxYear(returnPeriod) shouldBe year - 1
      }
    }

    forAll(yearGen) { year =>
      "return numeric value of year - 1 when in February" in {
        val returnPeriod = ReturnPeriod.apply(LocalDate.of(year, 2, 1))
        getTaxYear(returnPeriod) shouldBe year - 1
      }
    }

    forAll(yearGen) { year =>
      "return numeric value of year - 1 when in March" in {
        val returnPeriod = ReturnPeriod.apply(LocalDate.of(year, 3, 1))
        getTaxYear(returnPeriod) shouldBe year - 1
      }
    }

    forAll(yearGen) { year =>
      "return numeric value of year when in April" in {
        val returnPeriod = ReturnPeriod.apply(LocalDate.of(year, 4, 1))
        getTaxYear(returnPeriod) shouldBe year
      }
    }

    forAll(yearGen) { year =>
      "return numeric value of year when in May" in {
        val returnPeriod = ReturnPeriod.apply(LocalDate.of(year, 4, 1))
        getTaxYear(returnPeriod) shouldBe year
      }
    }

    forAll(yearGen) { year =>
      "return numeric value of year when in June" in {
        val returnPeriod = ReturnPeriod.apply(LocalDate.of(year, 4, 1))
        getTaxYear(returnPeriod) shouldBe year
      }
    }

    forAll(yearGen) { year =>
      "return numeric value of year when in July" in {
        val returnPeriod = ReturnPeriod.apply(LocalDate.of(year, 4, 1))
        getTaxYear(returnPeriod) shouldBe year
      }
    }

    forAll(yearGen) { year =>
      "return numeric value of year when in August" in {
        val returnPeriod = ReturnPeriod.apply(LocalDate.of(year, 4, 1))
        getTaxYear(returnPeriod) shouldBe year
      }
    }

    forAll(yearGen) { year =>
      "return numeric value of year when in September" in {
        val returnPeriod = ReturnPeriod.apply(LocalDate.of(year, 4, 1))
        getTaxYear(returnPeriod) shouldBe year
      }
    }

    forAll(yearGen) { year =>
      "return numeric value of year when in October" in {
        val returnPeriod = ReturnPeriod.apply(LocalDate.of(year, 4, 1))
        getTaxYear(returnPeriod) shouldBe year
      }
    }

    forAll(yearGen) { year =>
      "return numeric value of year when in November" in {
        val returnPeriod = ReturnPeriod.apply(LocalDate.of(year, 4, 1))
        getTaxYear(returnPeriod) shouldBe year
      }
    }

    forAll(yearGen) { year =>
      "return numeric value of year when in December" in {
        val returnPeriod = ReturnPeriod.apply(LocalDate.of(year, 4, 1))
        getTaxYear(returnPeriod) shouldBe year
      }
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
    val smallPosInts = Gen.choose(0, 1000)
    val largePosInts = Gen.choose(1000, 10000000)
    val janToMarInt = Gen.choose(1, 3)
    val aprToDecInt = Gen.choose(4, 12)

    (2018 to 2024).foreach(taxYear => {

      forAll(aprToDecInt) { month =>
        forAll(smallPosInts) { lowLitres =>
          forAll(smallPosInts) { highLitres =>
            s"calculate low levy correctly with small litres totals using original rates for Apr - Dec $taxYear" in {
              true shouldBe false
            }
          }
        }
      }

      forAll(aprToDecInt) { month =>
        forAll(smallPosInts) { lowLitres =>
          forAll(smallPosInts) { highLitres =>
            s"calculate high levy correctly with small litres totals using original rates for Apr - Dec $taxYear" in {
              true shouldBe false
            }
          }
        }
      }

      forAll(aprToDecInt) { month =>
        forAll(smallPosInts) { lowLitres =>
          forAll(smallPosInts) { highLitres =>
            s"calculate total correctly using with small litres totals original rates for Apr - Dec $taxYear" in {
              true shouldBe false
            }
          }
        }
      }

      forAll(aprToDecInt) { month =>
        forAll(largePosInts) { lowLitres =>
          forAll(smallPosInts) { highLitres =>
            s"calculate low levy correctly with large litres totals using original rates for Apr - Dec $taxYear" in {
              true shouldBe false
            }
          }
        }
      }

      forAll(aprToDecInt) { month =>
        forAll(largePosInts) { lowLitres =>
          forAll(smallPosInts) { highLitres =>
            s"calculate high levy correctly with large litres totals using original rates for Apr - Dec $taxYear" in {
              true shouldBe false
            }
          }
        }
      }

      forAll(aprToDecInt) { month =>
        forAll(largePosInts) { lowLitres =>
          forAll(largePosInts) { highLitres =>
            s"calculate total correctly using with large litres totals original rates for Apr - Dec $taxYear" in {
              true shouldBe false
            }
          }
        }
      }

      forAll(janToMarInt) { month =>
        forAll(smallPosInts) { lowLitres =>
          forAll(smallPosInts) { highLitres =>
            s"calculate low levy correctly with small litres totals using original rates for Jan - Mar ${taxYear + 1}" in {
              true shouldBe false
            }
          }
        }
      }

      forAll(janToMarInt) { month =>
        forAll(smallPosInts) { lowLitres =>
          forAll(smallPosInts) { highLitres =>
            s"calculate high levy correctly with small litres totals using original rates for Jan - Mar ${taxYear + 1}" in {
              true shouldBe false
            }
          }
        }
      }

      forAll(janToMarInt) { month =>
        forAll(smallPosInts) { lowLitres =>
          forAll(smallPosInts) { highLitres =>
            s"calculate total correctly using with small litres totals original rates for Jan - Mar ${taxYear + 1}" in {
              true shouldBe false
            }
          }
        }
      }

      forAll(janToMarInt) { month =>
        forAll(largePosInts) { lowLitres =>
          forAll(largePosInts) { highLitres =>
            s"calculate low levy correctly with large litres totals using original rates for Jan - Mar ${taxYear + 1}" in {
              true shouldBe false
            }
          }
        }
      }

      forAll(janToMarInt) { month =>
        forAll(largePosInts) { lowLitres =>
          forAll(largePosInts) { highLitres =>
            s"calculate high levy correctly with large litres totals using original rates for Jan - Mar ${taxYear + 1}" in {
              true shouldBe false
            }
          }
        }
      }

      forAll(janToMarInt) { month =>
        forAll(largePosInts) { lowLitres =>
          forAll(largePosInts) { highLitres =>
            s"calculate total correctly using with large litres totals original rates for Jan - Mar ${taxYear + 1}" in {
              true shouldBe false
            }
          }
        }
      }
    })

    (2025 to 2025).foreach(taxYear => {

      forAll(aprToDecInt) { month =>
        forAll(smallPosInts) { lowLitres =>
          forAll(smallPosInts) { highLitres =>
            s"calculate low levy correctly with small litres totals using $taxYear rates for Apr - Dec $taxYear" in {
              true shouldBe false
            }
          }
        }
      }

      forAll(aprToDecInt) { month =>
        forAll(smallPosInts) { lowLitres =>
          forAll(smallPosInts) { highLitres =>
            s"calculate high levy correctly with small litres totals using $taxYear rates for Apr - Dec $taxYear" in {
              true shouldBe false
            }
          }
        }
      }

      forAll(aprToDecInt) { month =>
        forAll(smallPosInts) { lowLitres =>
          forAll(smallPosInts) { highLitres =>
            s"calculate total correctly using with small litres totals $taxYear rates for Apr - Dec $taxYear" in {
              true shouldBe false
            }
          }
        }
      }

      forAll(aprToDecInt) { month =>
        forAll(largePosInts) { lowLitres =>
          forAll(largePosInts) { highLitres =>
            s"calculate low levy correctly with large litres totals using $taxYear rates for Apr - Dec $taxYear" in {
              true shouldBe false
            }
          }
        }
      }

      forAll(aprToDecInt) { month =>
        forAll(largePosInts) { lowLitres =>
          forAll(largePosInts) { highLitres =>
            s"calculate high levy correctly with large litres totals using $taxYear rates for Apr - Dec $taxYear" in {
              true shouldBe false
            }
          }
        }
      }

      forAll(aprToDecInt) { month =>
        forAll(largePosInts) { lowLitres =>
          forAll(largePosInts) { highLitres =>
            s"calculate total correctly using with large litres totals $taxYear rates for Apr - Dec $taxYear" in {
              true shouldBe false
            }
          }
        }
      }

      forAll(janToMarInt) { month =>
        forAll(smallPosInts) { lowLitres =>
          forAll(smallPosInts) { highLitres =>
            s"calculate low levy correctly with small litres totals using $taxYear rates for Jan - Mar ${taxYear + 1}" in {
              true shouldBe false
            }
          }
        }
      }

      forAll(janToMarInt) { month =>
        forAll(smallPosInts) { lowLitres =>
          forAll(smallPosInts) { highLitres =>
            s"calculate high levy correctly with small litres totals using $taxYear rates for Jan - Mar ${taxYear + 1}" in {
              true shouldBe false
            }
          }
        }
      }

      forAll(janToMarInt) { month =>
        forAll(smallPosInts) { lowLitres =>
          forAll(smallPosInts) { highLitres =>
            s"calculate total correctly using with small litres totals $taxYear rates for Jan - Mar ${taxYear + 1}" in {
              true shouldBe false
            }
          }
        }
      }

      forAll(janToMarInt) { month =>
        forAll(largePosInts) { lowLitres =>
          forAll(largePosInts) { highLitres =>
            s"calculate low levy correctly with large litres totals using $taxYear rates for Jan - Mar ${taxYear + 1}" in {
              true shouldBe false
            }
          }
        }
      }

      forAll(janToMarInt) { month =>
        forAll(largePosInts) { lowLitres =>
          forAll(largePosInts) { highLitres =>
            s"calculate high levy correctly with large litres totals using $taxYear rates for Jan - Mar ${taxYear + 1}" in {
              true shouldBe false
            }
          }
        }
      }

      forAll(janToMarInt) { month =>
        forAll(largePosInts) { lowLitres =>
          forAll(largePosInts) { highLitres =>
            s"calculate total correctly using with large litres totals $taxYear rates for Jan - Mar ${taxYear + 1}" in {
              true shouldBe false
            }
          }
        }
      }
    })

  }

}
