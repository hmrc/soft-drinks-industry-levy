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

import org.scalatestplus.play.PlaySpec
import ActivityType._
import org.scalacheck.Gen
import org.scalatest.AppendedClues
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{JsNumber, JsObject, JsValue, Json}
import sdil.models.ReturnPeriod

import java.time.LocalDate

class ActivitySpec extends PlaySpec with AppendedClues with ScalaCheckPropertyChecks {

  "Internal Activity" should {
    "include the volume copacked by others in the total produced values" in {
      val activity = internalActivity(produced = (200, 300), copackedByOthers = (300, 400))

      activity.totalProduced.value mustBe ((500, 700))
    }

    "sum the liable litres correctly" in {
      val activity = internalActivity(
        produced = (1, 2),
        copackedAll = (3, 4),
        imported = (5, 6)
      )

      activity.totalLiableLitres mustBe ((9, 12))
    }

    // TODO: Test taxEstimation in models/Activity.scala is used to form "estimatedTaxAmount" in des.create.scala
    // This is used by Json.toJson(submission) in createSubscription in DesConnector
    //  RELATING TO REGISTRATION - DO AFTER RETURNS
    "taxEstimationWithExplicitReturnPeriod" should {
      val janToMarInt = Gen.choose(1, 3)
      val aprToDecInt = Gen.choose(4, 12)

      (2018 to 2024).foreach { year =>
        val lowerBandCostPerLitre = BigDecimal("0.18")
        val higherBandCostPerLitre = BigDecimal("0.24")

        s"calculate zero taxEstimation correctly - using original rates for Apr - Dec $year" in {
          forAll(aprToDecInt) { month =>
            val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year, month, 1))
            val internalActivity = getInternalActivity()
            val taxEstimation = internalActivity.taxEstimationWithExplicitReturnPeriod(returnPeriod)
            taxEstimation mustBe BigDecimal("0.00")
          }
        }

        s"calculate non-zero taxEstimation for ProducedOwnBrand correctly - using original rates for Apr - Dec $year" in {
          forAll(aprToDecInt) { month =>
            val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year, month, 1))
            val internalActivity = getInternalActivity(hasProduced = true)
            val taxEstimation = internalActivity.taxEstimationWithExplicitReturnPeriod(returnPeriod)
            val producedOwnBrand = internalActivity.activity(ProducedOwnBrand)
            taxEstimation mustBe producedOwnBrand._1 * lowerBandCostPerLitre + producedOwnBrand._2 * higherBandCostPerLitre
          }
        }

        s"calculate non-zero taxEstimation for CopackerAll correctly - using original rates for Apr - Dec $year" in {
          forAll(aprToDecInt) { month =>
            val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year, month, 1))
            val internalActivity = getInternalActivity(hasCopackedAll = true)
            val taxEstimation = internalActivity.taxEstimationWithExplicitReturnPeriod(returnPeriod)
            val copackerAll = internalActivity.activity(CopackerAll)
            taxEstimation mustBe copackerAll._1 * lowerBandCostPerLitre + copackerAll._2 * higherBandCostPerLitre
          }
        }

        s"calculate non-zero taxEstimation for Imported correctly - using original rates for Apr - Dec $year" in {
          forAll(aprToDecInt) { month =>
            val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year, month, 1))
            val internalActivity = getInternalActivity(hasImported = true)
            val taxEstimation = internalActivity.taxEstimationWithExplicitReturnPeriod(returnPeriod)
            val imported = internalActivity.activity(Imported)
            taxEstimation mustBe imported._1 * lowerBandCostPerLitre + imported._2 * higherBandCostPerLitre
          }
        }

        s"calculate zero taxEstimation for Copackee correctly - using original rates for Apr - Dec $year" in {
          forAll(aprToDecInt) { month =>
            val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year, month, 1))
            val internalActivity = getInternalActivity(hasCopackedByOthers = true)
            val taxEstimation = internalActivity.taxEstimationWithExplicitReturnPeriod(returnPeriod)
            taxEstimation mustBe BigDecimal("0.00")
          }
        }

        s"calculate non-zero taxEstimation for all correctly - using original rates for Apr - Dec $year" in {
          forAll(aprToDecInt) { month =>
            val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year, month, 1))
            val internalActivity = getFullInternalActivity
            val taxEstimation = internalActivity.taxEstimationWithExplicitReturnPeriod(returnPeriod)
            taxEstimation mustBe internalActivity.totalLiableLitres._1 * lowerBandCostPerLitre + internalActivity.totalLiableLitres._2 * higherBandCostPerLitre
          }
        }

        s"calculate zero taxEstimation correctly - using original rates for Jan - Mar ${year + 1}" in {
          forAll(janToMarInt) { month =>
            val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year + 1, month, 1))
            val internalActivity = getInternalActivity()
            val taxEstimation = internalActivity.taxEstimationWithExplicitReturnPeriod(returnPeriod)
            taxEstimation mustBe BigDecimal("0.00")
          }
        }

        s"calculate non-zero taxEstimation for ProducedOwnBrand correctly - using original rates for Jan - Mar ${year + 1}" in {
          forAll(janToMarInt) { month =>
            val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year + 1, month, 1))
            val internalActivity = getInternalActivity(hasProduced = true)
            val taxEstimation = internalActivity.taxEstimationWithExplicitReturnPeriod(returnPeriod)
            val producedOwnBrand = internalActivity.activity(ProducedOwnBrand)
            taxEstimation mustBe producedOwnBrand._1 * lowerBandCostPerLitre + producedOwnBrand._2 * higherBandCostPerLitre
          }
        }

        s"calculate non-zero taxEstimation for CopackerAll correctly - using original rates for Jan - Mar ${year + 1}" in {
          forAll(janToMarInt) { month =>
            val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year + 1, month, 1))
            val internalActivity = getInternalActivity(hasCopackedAll = true)
            val taxEstimation = internalActivity.taxEstimationWithExplicitReturnPeriod(returnPeriod)
            val copackerAll = internalActivity.activity(CopackerAll)
            taxEstimation mustBe copackerAll._1 * lowerBandCostPerLitre + copackerAll._2 * higherBandCostPerLitre
          }
        }

        s"calculate non-zero taxEstimation for Imported correctly - using original rates for Jan - Mar ${year + 1}" in {
          forAll(janToMarInt) { month =>
            val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year + 1, month, 1))
            val internalActivity = getInternalActivity(hasImported = true)
            val taxEstimation = internalActivity.taxEstimationWithExplicitReturnPeriod(returnPeriod)
            val imported = internalActivity.activity(Imported)
            taxEstimation mustBe imported._1 * lowerBandCostPerLitre + imported._2 * higherBandCostPerLitre
          }
        }

        s"calculate zero taxEstimation for Copackee correctly - using original rates for Jan - Mar ${year + 1}" in {
          forAll(janToMarInt) { month =>
            val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year + 1, month, 1))
            val internalActivity = getInternalActivity(hasCopackedByOthers = true)
            val taxEstimation = internalActivity.taxEstimationWithExplicitReturnPeriod(returnPeriod)
            taxEstimation mustBe BigDecimal("0.00")
          }
        }

        s"calculate non-zero taxEstimation for all correctly - using original rates for Jan - Mar ${year + 1}" in {
          forAll(janToMarInt) { month =>
            val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year + 1, month, 1))
            val internalActivity = getFullInternalActivity
            val taxEstimation = internalActivity.taxEstimationWithExplicitReturnPeriod(returnPeriod)
            taxEstimation mustBe internalActivity.totalLiableLitres._1 * lowerBandCostPerLitre + internalActivity.totalLiableLitres._2 * higherBandCostPerLitre
          }
        }

      }

      (2025 to 2025).foreach { year =>
        val lowerBandCostPerLitreMap: Map[Int, BigDecimal] = Map(2025 -> BigDecimal("0.194"))
        val higherBandCostPerLitreMap: Map[Int, BigDecimal] = Map(2025 -> BigDecimal("0.259"))

        s"write lowVolume, highVolume, and levySubtotal - using original rates for Apr - Dec $year" in {
//          forAll(aprToDecInt) { month =>
//            implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year, month, 1))
//            forAll { r: ReturnsRequest =>
//              val json = Json.toJson(r)
//              r.imported match {
//                case Some(returnsImporting) =>
//                  val lowVolumeLevy = returnsImporting.largeProducerVolumes._1 * lowerBandCostPerLitreMap(year)
//                  val highVolumeLevy = returnsImporting.largeProducerVolumes._2 * higherBandCostPerLitreMap(year)
//                  val levySubtotal = lowVolumeLevy + highVolumeLevy
//                  val monetaryFields: Seq[(String, JsValue)] = Seq(
//                    ("lowVolume", JsNumber(lowVolumeLevy)),
//                    ("highVolume", JsNumber(highVolumeLevy)),
//                    ("levySubtotal", JsNumber(levySubtotal))
//                  )
//                  assert((json \ "importing" \ "monetaryValues").as[JsObject] == JsObject(monetaryFields))
//                //              TODO: Need to add correct assertion here
//                case None => assert(true)
//              }
//            }
//          }
        }

      }
    }

    "calculate the correct tax estimate" in {
      val returnPeriod = ReturnPeriod(LocalDate.of(2025, 4, 9))
      val activity = internalActivity(
        produced = (2, 3),
        copackedAll = (4, 5),
        imported = (6, 7)
      )

      activity.taxEstimationWithExplicitReturnPeriod(returnPeriod) mustBe BigDecimal("5.76")
    }

    "calculate the tax estimate as £99,999,999,999.99 if the tax estimate is greater than £99,999,999,999.99" in {
      val returnPeriod = ReturnPeriod(LocalDate.of(2025, 4, 9))
      val activity = internalActivity(
        produced = (2000000000L, 30000000000000L),
        copackedAll = (40000000, 50000000000L),
        imported = (600000000, 7000000000L),
        copackedByOthers = (8, 9)
      )

      activity.taxEstimationWithExplicitReturnPeriod(returnPeriod) mustBe BigDecimal("99999999999.99")
    }
  }

  def internalActivity(
    produced: LitreBands = zero,
    copackedAll: LitreBands = zero,
    imported: LitreBands = zero,
    copackedByOthers: LitreBands = zero
  ) =
    InternalActivity(
      Map(
        ProducedOwnBrand -> produced,
        CopackerAll      -> copackedAll,
        Imported         -> imported,
        Copackee         -> copackedByOthers
      ),
      false
    )

  lazy val zero: LitreBands = (0, 0)

  private def getRandomLitres: Long = Math.floor(Math.random() * 1000000).toLong
  private def getRandomLitreage: (Long, Long) = (getRandomLitres, getRandomLitres)

  private def getInternalActivity(hasProduced: Boolean = false, hasCopackedAll: Boolean = false, hasImported: Boolean = false, hasCopackedByOthers: Boolean = false) =
    InternalActivity(
      Map(
        ProducedOwnBrand -> (if (hasProduced) getRandomLitreage else zero),
        CopackerAll      -> (if (hasCopackedAll) getRandomLitreage else zero),
        Imported         -> (if (hasImported) getRandomLitreage else zero),
        Copackee         -> (if (hasCopackedByOthers) getRandomLitreage else zero)
      ),
      false
    )

  private def getFullInternalActivity = getInternalActivity(true, true, true, true)
}
