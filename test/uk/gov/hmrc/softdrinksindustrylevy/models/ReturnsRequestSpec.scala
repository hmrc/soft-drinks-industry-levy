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

import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import sdil.models.{ReturnPeriod, SdilReturn, SmallProducer}
import uk.gov.hmrc.softdrinksindustrylevy.models.TaxRateUtil._
import uk.gov.hmrc.softdrinksindustrylevy.util.FakeApplicationSpec

import java.time.LocalDate

class ReturnsRequestSpec extends FakeApplicationSpec with MockitoSugar with ScalaCheckPropertyChecks {
  "ReturnsPackaging" should {
    "totalSmallProdVolumes" in {
      val testSmallProdLitre1 = (109L, 110L)
      val testSmallProdLitre2 = (111L, 112L)
      val testSmallProd1 = SmallProducerVolume("", testSmallProdLitre1)
      val testSmallProd2 = SmallProducerVolume("", testSmallProdLitre2)

      val result = ReturnsPackaging(Seq(testSmallProd1, testSmallProd2), (0, 0)).totalSmallProdVolumes

      result._1 mustBe testSmallProdLitre1._1 + testSmallProdLitre2._1
      result._2 mustBe testSmallProdLitre1._2 + testSmallProdLitre2._2
    }
  }

  "ReturnsRequest" should {
    "apply converts values correctly" in {
      val testSdilRef = "someSdilRef"
      val testPackSmallLitre = (109L, 110L)
      val testPackSmall = SmallProducer(Some("Small producer co"), testSdilRef, testPackSmallLitre)
      val testImportSmall = (111L, 112L)
      val testImportLarge = (113L, 114L)
      val testPackLarge = (115L, 116L)
      val testOwnBrand = (117L, 118L)
      val testExport = (119L, 120L)
      val testWastage = (121L, 122L)

      val testSdilReturn = SdilReturn(
        importSmall = testImportSmall,
        importLarge = testImportLarge,
        packLarge = testPackLarge,
        ownBrand = testOwnBrand,
        `export` = testExport,
        wastage = testWastage,
        packSmall = List(testPackSmall),
        submittedOn = None
      )

      val result = ReturnsRequest(testSdilReturn)

      val resultSmallProducer = result.packaged.get.smallProducerVolumes.head
      resultSmallProducer.producerRef mustBe testSdilRef
      resultSmallProducer.volumes._1 mustBe testPackSmallLitre._1
      resultSmallProducer.volumes._2 mustBe testPackSmallLitre._2
      result.packaged.get.largeProducerVolumes._1 mustBe testPackLarge._1 + testOwnBrand._1
      result.packaged.get.largeProducerVolumes._2 mustBe testPackLarge._2 + testOwnBrand._2

      result.imported.get.smallProducerVolumes._1 mustBe testImportSmall._1
      result.imported.get.smallProducerVolumes._2 mustBe testImportSmall._2
      result.imported.get.largeProducerVolumes._1 mustBe testImportLarge._1
      result.imported.get.largeProducerVolumes._2 mustBe testImportLarge._2

      result.exported.get._1 mustBe testExport._1
      result.exported.get._2 mustBe testExport._2

      result.wastage.get._1 mustBe testWastage._1
      result.wastage.get._2 mustBe testWastage._2
    }
  }

  "liableVolumes" should {
    "include packaged large producer volumes" in {
      val returnsRequest = getReturnsRequest(packagedLargeProducer = true)
      returnsRequest.packaged.map(_.largeProducerVolumes._1) mustBe Some(returnsRequest.liableVolumes._1)
      returnsRequest.packaged.map(_.largeProducerVolumes._2) mustBe Some(returnsRequest.liableVolumes._2)
    }

    "include imported large producer volumes" in {
      val returnsRequest = getReturnsRequest(importedLargeProducer = true)
      returnsRequest.imported.map(_.largeProducerVolumes._1) mustBe Some(returnsRequest.liableVolumes._1)
      returnsRequest.imported.map(_.largeProducerVolumes._2) mustBe Some(returnsRequest.liableVolumes._2)
    }

    "ignore packaged small producer volumes" in {
      val returnsRequest = getReturnsRequest(packagedNumberOfSmallProducers = 5)
      returnsRequest.liableVolumes._1 mustEqual 0L
      returnsRequest.liableVolumes._2 mustEqual 0L
    }

    "ignore imported small producer volumes" in {
      val returnsRequest = getReturnsRequest(importedSmallProducer = true)
      returnsRequest.liableVolumes._1 mustEqual 0L
      returnsRequest.liableVolumes._2 mustEqual 0L
    }

    "ignore exported volumes" in {
      val returnsRequest = getReturnsRequest(exported = true)
      returnsRequest.liableVolumes._1 mustEqual 0L
      returnsRequest.liableVolumes._2 mustEqual 0L
    }

    "ignore wastage volumes" in {
      val returnsRequest = getReturnsRequest(wastage = true)
      returnsRequest.liableVolumes._1 mustEqual 0L
      returnsRequest.liableVolumes._2 mustEqual 0L
    }

    "be 'sum' of packaged large producer volumes and imported large producer volumes" in {
      val returnsRequest = getFullReturnsRequest
      val expectedLiableLitres = for {
        packagedLargeProducerVolumes <- returnsRequest.packaged.map(_.largeProducerVolumes)
        importedLargeProducerVolumes <- returnsRequest.imported.map(_.largeProducerVolumes)
      } yield (
        packagedLargeProducerVolumes._1 + importedLargeProducerVolumes._1,
        packagedLargeProducerVolumes._2 + importedLargeProducerVolumes._2
      )
      expectedLiableLitres.map(_._1) mustBe Some(returnsRequest.liableVolumes._1)
      expectedLiableLitres.map(_._2) mustBe Some(returnsRequest.liableVolumes._2)
    }
  }

  "nonLiableVolumes" should {
    "include exported volumes" in {
      val returnsRequest = getReturnsRequest(exported = true)
      returnsRequest.exported.map(_._1) mustBe Some(returnsRequest.nonLiableVolumes._1)
      returnsRequest.exported.map(_._2) mustBe Some(returnsRequest.nonLiableVolumes._2)
    }

    "include wastage volumes" in {
      val returnsRequest = getReturnsRequest(wastage = true)
      returnsRequest.wastage.map(_._1) mustBe Some(returnsRequest.nonLiableVolumes._1)
      returnsRequest.wastage.map(_._2) mustBe Some(returnsRequest.nonLiableVolumes._2)
    }

    "ignore packaged large producer volumes" in {
      val returnsRequest = getReturnsRequest(packagedLargeProducer = true)
      returnsRequest.nonLiableVolumes._1 mustEqual 0L
      returnsRequest.nonLiableVolumes._2 mustEqual 0L
    }

    "ignore imported large producer volumes" in {
      val returnsRequest = getReturnsRequest(importedLargeProducer = true)
      returnsRequest.nonLiableVolumes._1 mustEqual 0L
      returnsRequest.nonLiableVolumes._2 mustEqual 0L
    }

    "ignore packaged small producer volumes" in {
      val returnsRequest = getReturnsRequest(packagedNumberOfSmallProducers = 5)
      returnsRequest.nonLiableVolumes._1 mustEqual 0L
      returnsRequest.nonLiableVolumes._2 mustEqual 0L
    }

    "ignore imported small producer volumes" in {
      val returnsRequest = getReturnsRequest(importedSmallProducer = true)
      returnsRequest.nonLiableVolumes._1 mustEqual 0L
      returnsRequest.nonLiableVolumes._2 mustEqual 0L
    }

    "be 'sum' of exported volumes and wastage volumes" in {
      val returnsRequest = getFullReturnsRequest
      val expectedNonLiableLitres = for {
        exportedVolumes <- returnsRequest.exported
        wastageVolumes  <- returnsRequest.wastage
      } yield (exportedVolumes._1 + wastageVolumes._1, exportedVolumes._2 + wastageVolumes._2)
      expectedNonLiableLitres.map(_._1) mustBe Some(returnsRequest.nonLiableVolumes._1)
      expectedNonLiableLitres.map(_._2) mustBe Some(returnsRequest.nonLiableVolumes._2)
    }
  }

  "totalLevy" should {
    (2018 to 2024).foreach { year =>
      s"calculate low levy, high levy, and total correctly with zero litres totals using original rates for Apr - Dec $year" in {
        forAll(aprToDecInt) { month =>
          implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year, month, 1))
          val returnsRequest = ReturnsRequest(packaged = None, imported = None, exported = None, wastage = None)
          returnsRequest.totalLevy mustBe BigDecimal("0.00")
        }
      }

      s"calculate low levy, high levy, and total correctly with non-zero litres totals using original rates for Apr - Dec $year" in {
        forAll(aprToDecInt) { month =>
          implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year, month, 1))
          val returnsRequest = getFullReturnsRequest
          val leviedLitres = for {
            plp <- returnsRequest.packaged.map(_.largeProducerVolumes)
            ilp <- returnsRequest.imported.map(_.largeProducerVolumes)
            ex  <- returnsRequest.exported
            wa  <- returnsRequest.wastage
          } yield (plp._1 + ilp._1 - ex._1 - wa._1, plp._2 + ilp._2 - ex._2 - wa._2)
          val totalLevy = leviedLitres.map(calculateLevy(_, year))
          returnsRequest.totalLevy mustEqual totalLevy.get
        }
      }

      s"calculate low levy, high levy, and total correctly with zero litres totals using original rates for Jan - Mar ${year + 1}" in {
        forAll(janToMarInt) { month =>
          implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year + 1, month, 1))
          val returnsRequest = ReturnsRequest(packaged = None, imported = None, exported = None, wastage = None)
          returnsRequest.totalLevy mustBe BigDecimal("0.00")
        }
      }

      s"calculate low levy, high levy, and total correctly with non-zero litres totals using original rates for Jan - Mar ${year + 1}" in {
        forAll(janToMarInt) { month =>
          implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year + 1, month, 1))
          val returnsRequest = getFullReturnsRequest
          val leviedLitres = for {
            plp <- returnsRequest.packaged.map(_.largeProducerVolumes)
            ilp <- returnsRequest.imported.map(_.largeProducerVolumes)
            ex  <- returnsRequest.exported
            wa  <- returnsRequest.wastage
          } yield (plp._1 + ilp._1 - ex._1 - wa._1, plp._2 + ilp._2 - ex._2 - wa._2)
          val totalLevy = leviedLitres.map(calculateLevy(_, year))
          returnsRequest.totalLevy mustEqual totalLevy.get
        }
      }
    }

    (2025 to 2025).foreach { year =>
      s"calculate low levy, high levy, and total correctly with zero litres totals using $year rates for Apr - Dec $year" in {
        forAll(aprToDecInt) { month =>
          implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year, month, 1))
          val returnsRequest = ReturnsRequest(packaged = None, imported = None, exported = None, wastage = None)
          returnsRequest.totalLevy mustBe BigDecimal("0.00")
        }
      }

      s"calculate low levy, high levy, and total correctly with non-zero litres totals using $year rates for Apr - Dec $year" in {
        forAll(aprToDecInt) { month =>
          implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year, month, 1))
          val returnsRequest = getFullReturnsRequest
          val leviedLitres = for {
            plp <- returnsRequest.packaged.map(_.largeProducerVolumes)
            ilp <- returnsRequest.imported.map(_.largeProducerVolumes)
            ex  <- returnsRequest.exported
            wa  <- returnsRequest.wastage
          } yield (plp._1 + ilp._1 - ex._1 - wa._1, plp._2 + ilp._2 - ex._2 - wa._2)
          val totalLevy = leviedLitres.map(calculateLevy(_, year))
          returnsRequest.totalLevy mustEqual totalLevy.get.setScale(2, BigDecimal.RoundingMode.DOWN)
        }
      }

      s"calculate low levy, high levy, and total correctly with zero litres totals using $year rates for Jan - Mar ${year + 1}" in {
        forAll(janToMarInt) { month =>
          implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year + 1, month, 1))
          val returnsRequest = ReturnsRequest(packaged = None, imported = None, exported = None, wastage = None)
          returnsRequest.totalLevy mustBe BigDecimal("0.00")
        }
      }

      s"calculate low levy, high levy, and total correctly with non-zero litres totals using $year rates for Jan - Mar ${year + 1}" in {
        forAll(janToMarInt) { month =>
          implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year + 1, month, 1))
          val returnsRequest = getFullReturnsRequest
          val leviedLitres = for {
            plp <- returnsRequest.packaged.map(_.largeProducerVolumes)
            ilp <- returnsRequest.imported.map(_.largeProducerVolumes)
            ex  <- returnsRequest.exported
            wa  <- returnsRequest.wastage
          } yield (plp._1 + ilp._1 - ex._1 - wa._1, plp._2 + ilp._2 - ex._2 - wa._2)
          val totalLevy = leviedLitres.map(calculateLevy(_, year))
          returnsRequest.totalLevy mustEqual totalLevy.get.setScale(2, BigDecimal.RoundingMode.DOWN)
        }
      }

    }

  }
}
