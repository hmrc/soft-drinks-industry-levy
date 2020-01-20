/*
 * Copyright 2020 HM Revenue & Customs
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

import org.scalatest.Matchers
import java.time.LocalDate

import org.scalacheck.Gen
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.PropertyChecks
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.softdrinksindustrylevy.models.UkAddress

class ReturnSpec extends UnitSpec with Matchers with PropertyChecks with MockitoSugar {

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

  "SdilReturn bounds cost correct" should {
    "costLower" in {
      SdilReturn.costLower shouldBe 0.18
    }

    "costHigher" in {
      SdilReturn.costHigher shouldBe 0.24
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
        None).changedLitreages

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
        None).removedSmallProducers

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
        None).addedSmallProducers

      result.length shouldBe 1
      result.head shouldBe addedSmallPack
    }

    "total" in {
      val testSdilReturn =
        SdilReturn((1500, 1500), (1500, 1500), Nil, (0, 0), (1500, 1500), (1500, 1500), (1500, 1500), None)
      val result = testSdilReturn.total

      result shouldBe 630.00
    }
  }
}
