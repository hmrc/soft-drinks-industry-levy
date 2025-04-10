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
import uk.gov.hmrc.softdrinksindustrylevy.models.{ReturnsImporting, ReturnsPackaging, ReturnsRequest, SmallProducerVolume, UkAddress}

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

//  TODO: TEST sumLitres/total in models/Return.scala is used in varyReturn in VariationsController in views.html.return_variation_pdf which is submitted to dms
//  RELATING TO VARIATION - DO AFTER RETURNS

  private def getRandomLitres: Long = Math.floor(Math.random() * 1000000).toLong
  private def getRandomLitreage: (Long, Long) = (getRandomLitres, getRandomLitres)
  private def getRandomSdilRef(index: Int): String = s"${Math.floor(Math.random() * 1000).toLong}SdilRef$index"

  private def getSdilReturn(
                             ownBrand: Boolean = false,
                             packLarge: Boolean = false,
                             numberOfPackSmall: Int = 0,
                             importSmall: Boolean = false,
                             importLarge: Boolean = false,
                             export: Boolean = false,
                             wastage: Boolean = false
                           ): SdilReturn = {
    val smallProducers: Seq[SmallProducer] = (0 to numberOfPackSmall)
      .map(index => SmallProducer(None, getRandomSdilRef(index), getRandomLitreage))
    SdilReturn(
      ownBrand = if (ownBrand) getRandomLitreage else (0L, 0L),
      packLarge = if (packLarge) getRandomLitreage else (0L, 0L),
      packSmall = smallProducers.toList,
      importSmall = if (importSmall) getRandomLitreage else (0L, 0L),
      importLarge = if (importLarge) getRandomLitreage else (0L, 0L),
      export = if (export) getRandomLitreage else (0L, 0L),
      wastage = if (wastage) getRandomLitreage else (0L, 0L),
      submittedOn = None
    )
  }

  private def getFullSdilReturn: SdilReturn = getSdilReturn(
    ownBrand = true, packLarge = true, numberOfPackSmall = 5,
    importSmall = true, importLarge = true, export = true, wastage = true)

//  TODO: Is there a cleverer way to -1 * tuple
  
  "SdilReturn - leviedLitres, total" should {
    val janToMarInt = Gen.choose(1, 3)
    val aprToDecInt = Gen.choose(4, 12)

    (2018 to 2024).foreach { year =>
      val lowerBandCostPerLitre = BigDecimal("0.18")
      val higherBandCostPerLitre = BigDecimal("0.24")

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
          sdilReturn.leviedLitres shouldBe (0L, 0L)
          sdilReturn.total shouldBe BigDecimal("0.00")
        }
      }

      s"calculate levied litres, total for SdilReturn with importSmall correctly - using original rates for Apr - Dec $year" in {
        forAll(aprToDecInt) { month =>
          implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year, month, 1))
          val sdilReturn: SdilReturn = getSdilReturn(importSmall = true)
          sdilReturn.leviedLitres shouldBe (0L, 0L)
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
          sdilReturn.leviedLitres shouldBe (-1 * sdilReturn.export._1, -1 * sdilReturn.export._2)
          sdilReturn.total shouldBe -1 * (sdilReturn.export._1 * lowerBandCostPerLitre + sdilReturn.export._2 * higherBandCostPerLitre)
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
          val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year, month, 1))
        }
      }

    }

    (2025 to 2025).foreach { year =>
      val lowerBandCostPerLitreMap: Map[Int, BigDecimal] = Map(2025 -> BigDecimal("0.194"))
      val higherBandCostPerLitreMap: Map[Int, BigDecimal] = Map(2025 -> BigDecimal("0.259"))

      s"calculate levied litres, total for SdilReturn correctly - using $year rates for Apr - Dec $year" in {
        forAll(aprToDecInt) { month =>
          val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year, month, 1))
//          val internalActivity = getInternalActivity()
//          val taxEstimation = internalActivity.taxEstimationWithExplicitReturnPeriod(returnPeriod)
//          taxEstimation mustBe BigDecimal("0.00")
        }
      }
    }
  }

  "ReturnVariationData - revisedTotalDifference" should {}

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
      implicit val returnPeriod = ReturnPeriod(LocalDate.now())
      val result = testSdilReturn.total

      result shouldBe 630.00
    }
  }
}
