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

package sdil.models

import java.time.LocalDate
import org.scalacheck.Gen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import uk.gov.hmrc.softdrinksindustrylevy.models.TaxRateUtil._
import uk.gov.hmrc.softdrinksindustrylevy.models.UkAddress

class ReturnSpec extends AnyWordSpec with Matchers with ScalaCheckPropertyChecks with MockitoSugar {

  "A ReturnPeriod" should {
    val lowPosInts = Gen.choose(0, 1000)

    "be indexed correctly" in {
      forAll(lowPosInts) { i =>
        ReturnPeriod(i).count should be(i)
      }
    }

    "contain its start date" in {
      forAll(lowPosInts) { i =>
        val period = ReturnPeriod(i)
        ReturnPeriod(period.start) should be(period)
      }
    }

    "contain its end date" in {
      forAll(lowPosInts) { i =>
        val period = ReturnPeriod(i)
        ReturnPeriod(period.end) should be(period)
      }
    }

    "give the correct quarter for predefined dates" in {
      ReturnPeriod(LocalDate.of(2018, 4, 15)).quarter should be(1)
      ReturnPeriod(LocalDate.of(2018, 8, 15)).quarter should be(2)
      ReturnPeriod(LocalDate.of(2018, 12, 15)).quarter should be(3)
      ReturnPeriod(LocalDate.of(2019, 2, 15)).quarter should be(0)
    }

    "start on the 5th April 2018 if it is the first" in {
      ReturnPeriod(0).start should be(LocalDate.of(2018, 4, 5))
      ReturnPeriod(LocalDate.of(2018, 4, 8)) should be(ReturnPeriod(0))
    }

    "increment correctly" in {
      ReturnPeriod(0).next should be(ReturnPeriod(1))
      ReturnPeriod(0).end.plusDays(1) should be(ReturnPeriod(1).start)
    }

    "decrement correctly" in {
      ReturnPeriod(1).previous should be(ReturnPeriod(0))
    }

    "give correct pretty output" in {
      val testYear = 2018
      new ReturnPeriod(testYear, 0).pretty shouldBe "January to March 2018 (18C1)"
      new ReturnPeriod(testYear, 1).pretty shouldBe "April to June 2018 (18C2)"
      new ReturnPeriod(testYear, 2).pretty shouldBe "July to September 2018 (18C3)"
      new ReturnPeriod(testYear, 3).pretty shouldBe "October to December 2018 (18C4)"
    }
  }

  "SdilReturn - leviedLitres, total" should {
    (2018 to 2024).foreach { year =>
      s"calculate levied litres, total for SdilReturn with ownBrand correctly - using original rates for Apr - Dec $year" in {
        forAll(aprToDecInt) { month =>
          implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year, month, 1))
          val sdilReturn: SdilReturn = getSdilReturn(ownBrand = true)
          sdilReturn.leviedLitres shouldBe sdilReturn.ownBrand
          sdilReturn.total shouldBe sdilReturn.ownBrand._1 * lowerBandCostPerLitre + sdilReturn.ownBrand._2 * higherBandCostPerLitre
        }
      }

      s"calculate levied litres, total for SdilReturn with packLarge correctly - using original rates for Apr - Dec $year" in {
        forAll(aprToDecInt) { month =>
          implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year, month, 1))
          val sdilReturn: SdilReturn = getSdilReturn(packLarge = true)
          sdilReturn.leviedLitres shouldBe sdilReturn.packLarge
          sdilReturn.total shouldBe sdilReturn.packLarge._1 * lowerBandCostPerLitre + sdilReturn.packLarge._2 * higherBandCostPerLitre
        }
      }

      s"calculate levied litres, total for SdilReturn with packSmall correctly - using original rates for Apr - Dec $year" in {
        forAll(aprToDecInt) { month =>
          implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year, month, 1))
          val sdilReturn: SdilReturn = getSdilReturn(numberOfPackSmall = 5)
          sdilReturn.leviedLitres shouldBe zero
          sdilReturn.total shouldBe BigDecimal("0.00")
        }
      }

      s"calculate levied litres, total for SdilReturn with importSmall correctly - using original rates for Apr - Dec $year" in {
        forAll(aprToDecInt) { month =>
          implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year, month, 1))
          val sdilReturn: SdilReturn = getSdilReturn(importSmall = true)
          sdilReturn.leviedLitres shouldBe zero
          sdilReturn.total shouldBe BigDecimal("0.00")
        }
      }

      s"calculate levied litres, total for SdilReturn with importLarge correctly - using original rates for Apr - Dec $year" in {
        forAll(aprToDecInt) { month =>
          implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year, month, 1))
          val sdilReturn: SdilReturn = getSdilReturn(importLarge = true)
          sdilReturn.leviedLitres shouldBe sdilReturn.importLarge
          sdilReturn.total shouldBe sdilReturn.importLarge._1 * lowerBandCostPerLitre + sdilReturn.importLarge._2 * higherBandCostPerLitre
        }
      }

      s"calculate levied litres, total for SdilReturn with export correctly - using original rates for Apr - Dec $year" in {
        forAll(aprToDecInt) { month =>
          implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year, month, 1))
          val sdilReturn: SdilReturn = getSdilReturn(export = true)
          sdilReturn.leviedLitres shouldBe (-1 * sdilReturn.`export`._1, -1 * sdilReturn.`export`._2)
          sdilReturn.total shouldBe -1 * (sdilReturn.`export`._1 * lowerBandCostPerLitre + sdilReturn.`export`._2 * higherBandCostPerLitre)
        }
      }

      s"calculate levied litres, total for SdilReturn with wastage correctly - using original rates for Apr - Dec $year" in {
        forAll(aprToDecInt) { month =>
          implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year, month, 1))
          val sdilReturn: SdilReturn = getSdilReturn(wastage = true)
          sdilReturn.leviedLitres shouldBe (-1 * sdilReturn.wastage._1, -1 * sdilReturn.wastage._2)
          sdilReturn.total shouldBe -1 * (sdilReturn.wastage._1 * lowerBandCostPerLitre + sdilReturn.wastage._2 * higherBandCostPerLitre)
        }
      }

      s"calculate levied litres, total for SdilReturn with all fields correctly - using original rates for Apr - Dec $year" in {
        forAll(aprToDecInt) { month =>
          implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year, month, 1))
          val sdilReturn: SdilReturn = getFullSdilReturn
          val expectedLeviedLowLitres =
            sdilReturn.ownBrand._1 + sdilReturn.packLarge._1 + sdilReturn.importLarge._1 - sdilReturn.`export`._1 - sdilReturn.wastage._1
          val expectedLeviedHighLitres =
            sdilReturn.ownBrand._2 + sdilReturn.packLarge._2 + sdilReturn.importLarge._2 - sdilReturn.`export`._2 - sdilReturn.wastage._2
          sdilReturn.leviedLitres shouldBe (expectedLeviedLowLitres, expectedLeviedHighLitres)
          sdilReturn.total shouldBe expectedLeviedLowLitres * lowerBandCostPerLitre + expectedLeviedHighLitres * higherBandCostPerLitre
        }
      }

      s"calculate levied litres, total for SdilReturn with ownBrand correctly - using original rates for Jan - Mar ${year + 1}" in {
        forAll(janToMarInt) { month =>
          implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year + 1, month, 1))
          val sdilReturn: SdilReturn = getSdilReturn(ownBrand = true)
          sdilReturn.leviedLitres shouldBe sdilReturn.ownBrand
          sdilReturn.total shouldBe sdilReturn.ownBrand._1 * lowerBandCostPerLitre + sdilReturn.ownBrand._2 * higherBandCostPerLitre
        }
      }

      s"calculate levied litres, total for SdilReturn with packLarge correctly - using original rates for Jan - Mar ${year + 1}" in {
        forAll(janToMarInt) { month =>
          implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year + 1, month, 1))
          val sdilReturn: SdilReturn = getSdilReturn(packLarge = true)
          sdilReturn.leviedLitres shouldBe sdilReturn.packLarge
          sdilReturn.total shouldBe sdilReturn.packLarge._1 * lowerBandCostPerLitre + sdilReturn.packLarge._2 * higherBandCostPerLitre
        }
      }

      s"calculate levied litres, total for SdilReturn with packSmall correctly - using original rates for Jan - Mar ${year + 1}" in {
        forAll(janToMarInt) { month =>
          implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year + 1, month, 1))
          val sdilReturn: SdilReturn = getSdilReturn(numberOfPackSmall = 5)
          sdilReturn.leviedLitres shouldBe zero
          sdilReturn.total shouldBe BigDecimal("0.00")
        }
      }

      s"calculate levied litres, total for SdilReturn with importSmall correctly - using original rates for Jan - Mar ${year + 1}" in {
        forAll(janToMarInt) { month =>
          implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year + 1, month, 1))
          val sdilReturn: SdilReturn = getSdilReturn(importSmall = true)
          sdilReturn.leviedLitres shouldBe zero
          sdilReturn.total shouldBe BigDecimal("0.00")
        }
      }

      s"calculate levied litres, total for SdilReturn with importLarge correctly - using original rates for Jan - Mar ${year + 1}" in {
        forAll(janToMarInt) { month =>
          implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year + 1, month, 1))
          val sdilReturn: SdilReturn = getSdilReturn(importLarge = true)
          sdilReturn.leviedLitres shouldBe sdilReturn.importLarge
          sdilReturn.total shouldBe sdilReturn.importLarge._1 * lowerBandCostPerLitre + sdilReturn.importLarge._2 * higherBandCostPerLitre
        }
      }

      s"calculate levied litres, total for SdilReturn with export correctly - using original rates for Jan - Mar ${year + 1}" in {
        forAll(janToMarInt) { month =>
          implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year + 1, month, 1))
          val sdilReturn: SdilReturn = getSdilReturn(export = true)
          sdilReturn.leviedLitres shouldBe (-1 * sdilReturn.`export`._1, -1 * sdilReturn.`export`._2)
          sdilReturn.total shouldBe -1 * (sdilReturn.`export`._1 * lowerBandCostPerLitre + sdilReturn.`export`._2 * higherBandCostPerLitre)
        }
      }

      s"calculate levied litres, total for SdilReturn with wastage correctly - using original rates for Jan - Mar ${year + 1}" in {
        forAll(janToMarInt) { month =>
          implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year + 1, month, 1))
          val sdilReturn: SdilReturn = getSdilReturn(wastage = true)
          sdilReturn.leviedLitres shouldBe (-1 * sdilReturn.wastage._1, -1 * sdilReturn.wastage._2)
          sdilReturn.total shouldBe -1 * (sdilReturn.wastage._1 * lowerBandCostPerLitre + sdilReturn.wastage._2 * higherBandCostPerLitre)
        }
      }

      s"calculate levied litres, total for SdilReturn with all fields correctly - using original rates for Jan - Mar ${year + 1}" in {
        forAll(janToMarInt) { month =>
          implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year + 1, month, 1))
          val sdilReturn: SdilReturn = getFullSdilReturn
          val expectedLeviedLowLitres =
            sdilReturn.ownBrand._1 + sdilReturn.packLarge._1 + sdilReturn.importLarge._1 - sdilReturn.`export`._1 - sdilReturn.wastage._1
          val expectedLeviedHighLitres =
            sdilReturn.ownBrand._2 + sdilReturn.packLarge._2 + sdilReturn.importLarge._2 - sdilReturn.`export`._2 - sdilReturn.wastage._2
          sdilReturn.leviedLitres shouldBe (expectedLeviedLowLitres, expectedLeviedHighLitres)
          sdilReturn.total shouldBe expectedLeviedLowLitres * lowerBandCostPerLitre + expectedLeviedHighLitres * higherBandCostPerLitre
        }
      }

    }

    (2025 to 2025).foreach { year =>
      s"calculate levied litres, total for SdilReturn with ownBrand correctly - using $year rates for Apr - Dec $year" in {
        forAll(aprToDecInt) { month =>
          implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year, month, 1))
          val sdilReturn: SdilReturn = getSdilReturn(ownBrand = true)
          sdilReturn.leviedLitres shouldBe sdilReturn.ownBrand
          val expectedTotal = (sdilReturn.ownBrand._1 * lowerBandCostPerLitreMap(
            year
          ) + sdilReturn.ownBrand._2 * higherBandCostPerLitreMap(year))
            .setScale(2, BigDecimal.RoundingMode.HALF_UP)
          sdilReturn.total shouldBe expectedTotal
        }
      }

      s"calculate levied litres, total for SdilReturn with packLarge correctly - using $year rates for Apr - Dec $year" in {
        forAll(aprToDecInt) { month =>
          implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year, month, 1))
          val sdilReturn: SdilReturn = getSdilReturn(packLarge = true)
          sdilReturn.leviedLitres shouldBe sdilReturn.packLarge
          val expectedTotal = (sdilReturn.packLarge._1 * lowerBandCostPerLitreMap(
            year
          ) + sdilReturn.packLarge._2 * higherBandCostPerLitreMap(year))
            .setScale(2, BigDecimal.RoundingMode.HALF_UP)
          sdilReturn.total shouldBe expectedTotal
        }
      }

      s"calculate levied litres, total for SdilReturn with packSmall correctly - using $year rates for Apr - Dec $year" in {
        forAll(aprToDecInt) { month =>
          implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year, month, 1))
          val sdilReturn: SdilReturn = getSdilReturn(numberOfPackSmall = 5)
          sdilReturn.leviedLitres shouldBe zero
          sdilReturn.total shouldBe BigDecimal("0.00")
        }
      }

      s"calculate levied litres, total for SdilReturn with importSmall correctly - using $year rates for Apr - Dec $year" in {
        forAll(aprToDecInt) { month =>
          implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year, month, 1))
          val sdilReturn: SdilReturn = getSdilReturn(importSmall = true)
          sdilReturn.leviedLitres shouldBe zero
          sdilReturn.total shouldBe BigDecimal("0.00")
        }
      }

      s"calculate levied litres, total for SdilReturn with importLarge correctly - using $year rates for Apr - Dec $year" in {
        forAll(aprToDecInt) { month =>
          implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year, month, 1))
          val sdilReturn: SdilReturn = getSdilReturn(importLarge = true)
          sdilReturn.leviedLitres shouldBe sdilReturn.importLarge
          val expectedTotal = (sdilReturn.importLarge._1 * lowerBandCostPerLitreMap(
            year
          ) + sdilReturn.importLarge._2 * higherBandCostPerLitreMap(year))
            .setScale(2, BigDecimal.RoundingMode.HALF_UP)
          sdilReturn.total shouldBe expectedTotal
        }
      }

      s"calculate levied litres, total for SdilReturn with export correctly - using $year rates for Apr - Dec $year" in {
        forAll(aprToDecInt) { month =>
          implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year, month, 1))
          val sdilReturn: SdilReturn = getSdilReturn(export = true)
          sdilReturn.leviedLitres shouldBe (-1 * sdilReturn.`export`._1, -1 * sdilReturn.`export`._2)
          val expectedTotal = -1 * (sdilReturn.`export`._1 * lowerBandCostPerLitreMap(
            year
          ) + sdilReturn.`export`._2 * higherBandCostPerLitreMap(year))
            .setScale(2, BigDecimal.RoundingMode.HALF_UP)
          sdilReturn.total shouldBe expectedTotal
        }
      }

      s"calculate levied litres, total for SdilReturn with wastage correctly - using $year rates for Apr - Dec $year" in {
        forAll(aprToDecInt) { month =>
          implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year, month, 1))
          val sdilReturn: SdilReturn = getSdilReturn(wastage = true)
          sdilReturn.leviedLitres shouldBe (-1 * sdilReturn.wastage._1, -1 * sdilReturn.wastage._2)
          val expectedTotal = -1 * (sdilReturn.wastage._1 * lowerBandCostPerLitreMap(
            year
          ) + sdilReturn.wastage._2 * higherBandCostPerLitreMap(year))
            .setScale(2, BigDecimal.RoundingMode.HALF_UP)
          sdilReturn.total shouldBe expectedTotal
        }
      }

      s"calculate levied litres, total for SdilReturn with all fields correctly - using $year rates for Apr - Dec $year" in {
        forAll(aprToDecInt) { month =>
          implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year, month, 1))
          val sdilReturn: SdilReturn = getFullSdilReturn
          val expectedLeviedLowLitres =
            sdilReturn.ownBrand._1 + sdilReturn.packLarge._1 + sdilReturn.importLarge._1 - sdilReturn.`export`._1 - sdilReturn.wastage._1
          val expectedLeviedHighLitres =
            sdilReturn.ownBrand._2 + sdilReturn.packLarge._2 + sdilReturn.importLarge._2 - sdilReturn.`export`._2 - sdilReturn.wastage._2
          sdilReturn.leviedLitres shouldBe (expectedLeviedLowLitres, expectedLeviedHighLitres)
          val expectedTotal = (expectedLeviedLowLitres * lowerBandCostPerLitreMap(
            year
          ) + expectedLeviedHighLitres * higherBandCostPerLitreMap(year))
            .setScale(2, BigDecimal.RoundingMode.HALF_UP)
          sdilReturn.total shouldBe expectedTotal
        }
      }

      s"calculate levied litres, total for SdilReturn with ownBrand correctly - using $year rates for Jan - Mar ${year + 1}" in {
        forAll(janToMarInt) { month =>
          implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year + 1, month, 1))
          val sdilReturn: SdilReturn = getSdilReturn(ownBrand = true)
          sdilReturn.leviedLitres shouldBe sdilReturn.ownBrand
          val expectedTotal = (sdilReturn.ownBrand._1 * lowerBandCostPerLitreMap(
            year
          ) + sdilReturn.ownBrand._2 * higherBandCostPerLitreMap(year))
            .setScale(2, BigDecimal.RoundingMode.HALF_UP)
          sdilReturn.total shouldBe expectedTotal
        }
      }

      s"calculate levied litres, total for SdilReturn with packLarge correctly - using $year rates for Jan - Mar ${year + 1}" in {
        forAll(janToMarInt) { month =>
          implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year + 1, month, 1))
          val sdilReturn: SdilReturn = getSdilReturn(packLarge = true)
          sdilReturn.leviedLitres shouldBe sdilReturn.packLarge
          val expectedTotal = (sdilReturn.packLarge._1 * lowerBandCostPerLitreMap(
            year
          ) + sdilReturn.packLarge._2 * higherBandCostPerLitreMap(year))
            .setScale(2, BigDecimal.RoundingMode.HALF_UP)
          sdilReturn.total shouldBe expectedTotal
        }
      }

      s"calculate levied litres, total for SdilReturn with packSmall correctly - using $year rates for Jan - Mar ${year + 1}" in {
        forAll(janToMarInt) { month =>
          implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year + 1, month, 1))
          val sdilReturn: SdilReturn = getSdilReturn(numberOfPackSmall = 5)
          sdilReturn.leviedLitres shouldBe zero
          sdilReturn.total shouldBe BigDecimal("0.00")
        }
      }

      s"calculate levied litres, total for SdilReturn with importSmall correctly - using $year rates for Jan - Mar ${year + 1}" in {
        forAll(janToMarInt) { month =>
          implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year + 1, month, 1))
          val sdilReturn: SdilReturn = getSdilReturn(importSmall = true)
          sdilReturn.leviedLitres shouldBe zero
          sdilReturn.total shouldBe BigDecimal("0.00")
        }
      }

      s"calculate levied litres, total for SdilReturn with importLarge correctly - using $year rates for Jan - Mar ${year + 1}" in {
        forAll(janToMarInt) { month =>
          implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year + 1, month, 1))
          val sdilReturn: SdilReturn = getSdilReturn(importLarge = true)
          sdilReturn.leviedLitres shouldBe sdilReturn.importLarge
          val expectedTotal = (sdilReturn.importLarge._1 * lowerBandCostPerLitreMap(
            year
          ) + sdilReturn.importLarge._2 * higherBandCostPerLitreMap(year))
            .setScale(2, BigDecimal.RoundingMode.HALF_UP)
          sdilReturn.total shouldBe expectedTotal
        }
      }

      s"calculate levied litres, total for SdilReturn with export correctly - using $year rates for Jan - Mar ${year + 1}" in {
        forAll(janToMarInt) { month =>
          implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year + 1, month, 1))
          val sdilReturn: SdilReturn = getSdilReturn(export = true)
          sdilReturn.leviedLitres shouldBe (-1 * sdilReturn.`export`._1, -1 * sdilReturn.`export`._2)
          val expectedTotal = -1 * (sdilReturn.`export`._1 * lowerBandCostPerLitreMap(
            year
          ) + sdilReturn.`export`._2 * higherBandCostPerLitreMap(year))
            .setScale(2, BigDecimal.RoundingMode.HALF_UP)
          sdilReturn.total shouldBe expectedTotal
        }
      }

      s"calculate levied litres, total for SdilReturn with wastage correctly - using $year rates for Jan - Mar ${year + 1}" in {
        forAll(janToMarInt) { month =>
          implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year + 1, month, 1))
          val sdilReturn: SdilReturn = getSdilReturn(wastage = true)
          sdilReturn.leviedLitres shouldBe (-1 * sdilReturn.wastage._1, -1 * sdilReturn.wastage._2)
          val expectedTotal = -1 * (sdilReturn.wastage._1 * lowerBandCostPerLitreMap(
            year
          ) + sdilReturn.wastage._2 * higherBandCostPerLitreMap(year))
            .setScale(2, BigDecimal.RoundingMode.HALF_UP)
          sdilReturn.total shouldBe expectedTotal
        }
      }

      s"calculate levied litres, total for SdilReturn with all fields correctly - using $year rates for Jan - Mar ${year + 1}" in {
        forAll(janToMarInt) { month =>
          implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year + 1, month, 1))
          val sdilReturn: SdilReturn = getFullSdilReturn
          val expectedLeviedLowLitres =
            sdilReturn.ownBrand._1 + sdilReturn.packLarge._1 + sdilReturn.importLarge._1 - sdilReturn.`export`._1 - sdilReturn.wastage._1
          val expectedLeviedHighLitres =
            sdilReturn.ownBrand._2 + sdilReturn.packLarge._2 + sdilReturn.importLarge._2 - sdilReturn.`export`._2 - sdilReturn.wastage._2
          sdilReturn.leviedLitres shouldBe (expectedLeviedLowLitres, expectedLeviedHighLitres)
          val expectedTotal = (expectedLeviedLowLitres * lowerBandCostPerLitreMap(
            year
          ) + expectedLeviedHighLitres * higherBandCostPerLitreMap(year))
            .setScale(2, BigDecimal.RoundingMode.HALF_UP)
          sdilReturn.total shouldBe expectedTotal
        }
      }
    }
  }

  "ReturnVariationData - revisedTotalDifference" should {
    val rvdAddress = UkAddress(List("My House", "My Lane"), "AA111A")

    (2018 to 2024).foreach { year =>
      s"calculate revisedTotalDifference for ReturnVariationData for two SdilReturns with all fields correctly - using original rates for Apr - Dec $year" in {
        forAll(aprToDecInt) { month =>
          implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year, month, 1))
          val originalSdilReturn: SdilReturn = getFullSdilReturn
          val revisedSdilReturn: SdilReturn = getFullSdilReturn
          val expectedOriginalLeviedLowLitres =
            originalSdilReturn.ownBrand._1 + originalSdilReturn.packLarge._1 + originalSdilReturn.importLarge._1 - originalSdilReturn.`export`._1 - originalSdilReturn.wastage._1
          val expectedOriginalLeviedHighLitres =
            originalSdilReturn.ownBrand._2 + originalSdilReturn.packLarge._2 + originalSdilReturn.importLarge._2 - originalSdilReturn.`export`._2 - originalSdilReturn.wastage._2
          val expectedRevisedlLeviedLowLitres =
            revisedSdilReturn.ownBrand._1 + revisedSdilReturn.packLarge._1 + revisedSdilReturn.importLarge._1 - revisedSdilReturn.`export`._1 - revisedSdilReturn.wastage._1
          val expectedRevisedLeviedHighLitres =
            revisedSdilReturn.ownBrand._2 + revisedSdilReturn.packLarge._2 + revisedSdilReturn.importLarge._2 - revisedSdilReturn.`export`._2 - revisedSdilReturn.wastage._2
          val changeInLeviedLitres = (
            expectedRevisedlLeviedLowLitres - expectedOriginalLeviedLowLitres,
            expectedRevisedLeviedHighLitres - expectedOriginalLeviedHighLitres
          )
          val returnVariationData =
            ReturnVariationData(originalSdilReturn, revisedSdilReturn, returnPeriod, "testOrg", rvdAddress, "", None)
          returnVariationData.revisedTotalDifference shouldBe changeInLeviedLitres._1 * lowerBandCostPerLitre + changeInLeviedLitres._2 * higherBandCostPerLitre
        }
      }

      s"calculate revisedTotalDifference for ReturnVariationData for two SdilReturns with all fields correctly - using original rates for Jan - Mar ${year + 1}" in {
        forAll(janToMarInt) { month =>
          implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year + 1, month, 1))
          val originalSdilReturn: SdilReturn = getFullSdilReturn
          val revisedSdilReturn: SdilReturn = getFullSdilReturn
          val expectedOriginalLeviedLowLitres =
            originalSdilReturn.ownBrand._1 + originalSdilReturn.packLarge._1 + originalSdilReturn.importLarge._1 - originalSdilReturn.`export`._1 - originalSdilReturn.wastage._1
          val expectedOriginalLeviedHighLitres =
            originalSdilReturn.ownBrand._2 + originalSdilReturn.packLarge._2 + originalSdilReturn.importLarge._2 - originalSdilReturn.`export`._2 - originalSdilReturn.wastage._2
          val expectedRevisedlLeviedLowLitres =
            revisedSdilReturn.ownBrand._1 + revisedSdilReturn.packLarge._1 + revisedSdilReturn.importLarge._1 - revisedSdilReturn.`export`._1 - revisedSdilReturn.wastage._1
          val expectedRevisedLeviedHighLitres =
            revisedSdilReturn.ownBrand._2 + revisedSdilReturn.packLarge._2 + revisedSdilReturn.importLarge._2 - revisedSdilReturn.`export`._2 - revisedSdilReturn.wastage._2
          val changeInLeviedLitres = (
            expectedRevisedlLeviedLowLitres - expectedOriginalLeviedLowLitres,
            expectedRevisedLeviedHighLitres - expectedOriginalLeviedHighLitres
          )
          val returnVariationData =
            ReturnVariationData(originalSdilReturn, revisedSdilReturn, returnPeriod, "testOrg", rvdAddress, "", None)
          returnVariationData.revisedTotalDifference shouldBe changeInLeviedLitres._1 * lowerBandCostPerLitre + changeInLeviedLitres._2 * higherBandCostPerLitre
        }
      }
    }

    (2025 to 2025).foreach { year =>
      s"calculate revisedTotalDifference for ReturnVariationData for two SdilReturns with all fields correctly - using $year rates for Apr - Dec $year" in {
        forAll(aprToDecInt) { month =>
          implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year, month, 1))
          val originalSdilReturn: SdilReturn = getFullSdilReturn
          val revisedSdilReturn: SdilReturn = getFullSdilReturn
          val expectedOriginalLeviedLowLitres =
            originalSdilReturn.ownBrand._1 + originalSdilReturn.packLarge._1 + originalSdilReturn.importLarge._1 - originalSdilReturn.`export`._1 - originalSdilReturn.wastage._1
          val expectedOriginalLeviedHighLitres =
            originalSdilReturn.ownBrand._2 + originalSdilReturn.packLarge._2 + originalSdilReturn.importLarge._2 - originalSdilReturn.`export`._2 - originalSdilReturn.wastage._2
          val expectedRevisedlLeviedLowLitres =
            revisedSdilReturn.ownBrand._1 + revisedSdilReturn.packLarge._1 + revisedSdilReturn.importLarge._1 - revisedSdilReturn.`export`._1 - revisedSdilReturn.wastage._1
          val expectedRevisedLeviedHighLitres =
            revisedSdilReturn.ownBrand._2 + revisedSdilReturn.packLarge._2 + revisedSdilReturn.importLarge._2 - revisedSdilReturn.`export`._2 - revisedSdilReturn.wastage._2
          val changeInLeviedLitres = (
            expectedRevisedlLeviedLowLitres - expectedOriginalLeviedLowLitres,
            expectedRevisedLeviedHighLitres - expectedOriginalLeviedHighLitres
          )
          val returnVariationData =
            ReturnVariationData(originalSdilReturn, revisedSdilReturn, returnPeriod, "testOrg", rvdAddress, "", None)
          val expectedRTD = (changeInLeviedLitres._1 * lowerBandCostPerLitreMap(
            year
          ) + changeInLeviedLitres._2 * higherBandCostPerLitreMap(year))
            .setScale(2, BigDecimal.RoundingMode.HALF_UP)
          returnVariationData.revisedTotalDifference shouldBe expectedRTD
        }
      }

      s"calculate revisedTotalDifference for ReturnVariationData for two SdilReturns with all fields correctly - using $year rates for Jan - Mar ${year + 1}" in {
        forAll(janToMarInt) { month =>
          implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year + 1, month, 1))
          val originalSdilReturn: SdilReturn = getFullSdilReturn
          val revisedSdilReturn: SdilReturn = getFullSdilReturn
          val expectedOriginalLeviedLowLitres =
            originalSdilReturn.ownBrand._1 + originalSdilReturn.packLarge._1 + originalSdilReturn.importLarge._1 - originalSdilReturn.`export`._1 - originalSdilReturn.wastage._1
          val expectedOriginalLeviedHighLitres =
            originalSdilReturn.ownBrand._2 + originalSdilReturn.packLarge._2 + originalSdilReturn.importLarge._2 - originalSdilReturn.`export`._2 - originalSdilReturn.wastage._2
          val expectedRevisedlLeviedLowLitres =
            revisedSdilReturn.ownBrand._1 + revisedSdilReturn.packLarge._1 + revisedSdilReturn.importLarge._1 - revisedSdilReturn.`export`._1 - revisedSdilReturn.wastage._1
          val expectedRevisedLeviedHighLitres =
            revisedSdilReturn.ownBrand._2 + revisedSdilReturn.packLarge._2 + revisedSdilReturn.importLarge._2 - revisedSdilReturn.`export`._2 - revisedSdilReturn.wastage._2
          val changeInLeviedLitres = (
            expectedRevisedlLeviedLowLitres - expectedOriginalLeviedLowLitres,
            expectedRevisedLeviedHighLitres - expectedOriginalLeviedHighLitres
          )
          val returnVariationData =
            ReturnVariationData(originalSdilReturn, revisedSdilReturn, returnPeriod, "testOrg", rvdAddress, "", None)
          val expectedRTD = (changeInLeviedLitres._1 * lowerBandCostPerLitreMap(
            year
          ) + changeInLeviedLitres._2 * higherBandCostPerLitreMap(year))
            .setScale(2, BigDecimal.RoundingMode.HALF_UP)
          returnVariationData.revisedTotalDifference shouldBe expectedRTD
        }
      }
    }
  }

  "ReturnVariationData" should {
    val commonSmallPack = SmallProducer(Some("common"), "1", (100, 100))
    val removedSmallPack = SmallProducer(Some("removed"), "2", (100, 100))
    val addedSmallPack = SmallProducer(Some("added"), "3", (100, 100))
    val testOriginalPackers = SdilReturn(packSmall = List(commonSmallPack, removedSmallPack), submittedOn = None)
    val testRevisedPackers = SdilReturn(packSmall = List(commonSmallPack, addedSmallPack), submittedOn = None)

    "changedLitreages" in {
      val testOriginal = SdilReturn(submittedOn = None)
      val testRevised = SdilReturn((3, 3), (3, 3), Nil, (3, 3), (3, 3), (3, 3), (3, 3), None)
      val result = ReturnVariationData(
        testOriginal,
        testRevised,
        ReturnPeriod(2018, 1),
        "testOrg",
        UkAddress(Nil, ""),
        "",
        None
      ).changedLitreages

      for ((_, (x, y)) <- result) {
        x shouldBe 3
        y shouldBe 3
      }
    }

    "removedSmallProducers" in {
      val result = ReturnVariationData(
        testOriginalPackers,
        testRevisedPackers,
        ReturnPeriod(2018, 1),
        "testOrg",
        UkAddress(Nil, ""),
        "",
        None
      ).removedSmallProducers

      result.length shouldBe 1
      result.head shouldBe removedSmallPack
    }

    "addedSmallProducers" in {
      val result = ReturnVariationData(
        testOriginalPackers,
        testRevisedPackers,
        ReturnPeriod(2018, 1),
        "testOrg",
        UkAddress(Nil, ""),
        "",
        None
      ).addedSmallProducers

      result.length shouldBe 1
      result.head shouldBe addedSmallPack
    }

    "total" in {
      val testSdilReturn =
        SdilReturn((1500, 1500), (1500, 1500), Nil, (0, 0), (1500, 1500), (1500, 1500), (1500, 1500), None)
      implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(2025, 3, 31))
      val result = testSdilReturn.total

      result shouldBe 630.00
    }
  }
}
