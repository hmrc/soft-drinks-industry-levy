/*
 * Copyright 2018 HM Revenue & Customs
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

import org.scalatestplus.play.PlaySpec
import ActivityType._
import org.scalatest.AppendedClues

class ActivitySpec extends PlaySpec with AppendedClues {

  "Internal Activity" should {
    "not include litres copacked for small producers in the total copacked values" in {
      val activity = internalActivity(copackedAll = (100, 200), copackedSmall = (25, 75))

      activity.liableCopacked.value mustBe ((75, 125))
    }

    "include the volume copacked by others in the total produced values" in {
      val activity = internalActivity(produced = (200, 300), copackedByOthers = (300, 400))

      activity.totalProduced.value mustBe ((500, 700))
    }

    "sum the liable litres correctly" in {
      val activity = internalActivity(
        produced = (1, 2),
        copackedAll = (3, 4),
        copackedSmall = (2, 1),
        imported = (5, 6),
        copackedByOthers = (7, 8)
      )

      activity.totalLiableLitres mustBe ((14, 19))
    }

    "calculate the correct tax estimate" in {
      val activity = internalActivity(
        produced = (2, 3),
        copackedAll = (4, 5),
        copackedSmall = (3, 2),
        imported = (6, 7),
        copackedByOthers = (8, 9)
      )

      activity.taxEstimation mustBe BigDecimal("8.34")
    }

    "calculate the tax estimate as £99,999,999,999.99 if the tax estimate is greater than £99,999,999,999.99" in {
      val activity = internalActivity(
        produced = (2000000000L, 30000000000000L),
        copackedAll = (40000000, 50000000000L),
        copackedSmall = (3, 2),
        imported = (600000000, 7000000000L),
        copackedByOthers = (8, 9)
      )

      activity.taxEstimation mustBe BigDecimal("99999999999.99")
    }

    "flag the user as a small producer if the total copacked by others or produced is less than 1,000,000" in {
      val activity = internalActivity(
        produced = (499999, 500000),
        copackedAll = (999999, 999999)
      )

      activity.isLarge mustBe false withClue "Incorrectly marked as large producer"
    }

    "flag the user as a large producer if the total copacked by others and produced is greater than or equal to 1,000,000" in {
      val activity = internalActivity(
        produced = (999999, 999999),
        copackedAll = (499999, 500000)
      )

      activity.isLarge mustBe true withClue "Incorrectly marked as small producer"
    }
  }

  def internalActivity(produced: LitreBands = zero,
                       copackedAll: LitreBands = zero,
                       copackedSmall: LitreBands = zero,
                       imported: LitreBands = zero,
                       copackedByOthers: LitreBands = zero) = {
    InternalActivity(
      Map(
        ProducedOwnBrand -> produced,
        CopackerAll -> copackedAll,
        CopackerSmall -> copackedSmall,
        Imported -> imported,
        Copackee -> copackedByOthers
      )
    )
  }

  lazy val zero: LitreBands = (0, 0)
}
