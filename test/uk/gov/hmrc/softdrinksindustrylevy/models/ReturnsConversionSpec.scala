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

import java.time.{Clock, LocalDate, LocalDateTime, ZoneId}
import com.github.fge.jackson.JsonLoader
import com.github.fge.jsonschema.main.JsonSchemaFactory
import org.scalacheck.Gen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{JsNumber, JsObject, JsString, JsValue, Json}
import uk.gov.hmrc.softdrinksindustrylevy.models.connectors.arbReturnReq
import uk.gov.hmrc.softdrinksindustrylevy.models.json.des.returns._
import sdil.models._

class ReturnsConversionSpec extends AnyWordSpec with ScalaCheckPropertyChecks with Matchers {

//  implicit val clock: Clock = Clock.systemDefaultZone()
  private val zone = ZoneId.systemDefault()

  implicit def period(implicit cl: Clock): ReturnPeriod = ReturnPeriod(LocalDate.now(cl))

  "ReturnsConversion" should {
    "parse Returns as expected" in {
      val validator = JsonSchemaFactory.byDefault.getValidator

      val stream = getClass.getResourceAsStream("/test/des-return.schema.json")
      val schemaText = scala.io.Source.fromInputStream(stream).getLines().mkString
      stream.close
      val schema = JsonLoader.fromString(schemaText)
      implicit val clock: Clock = Clock.systemDefaultZone()
      forAll { r: ReturnsRequest =>
        val json = JsonLoader.fromString(Json.prettyPrint(Json.toJson(r)))
        val report = validator.validate(schema, json)
        assert(report.isSuccess, report)
      }
    }

    "Period key is first quarter when date is before 1st April" in {
      val date = LocalDateTime.of(2018, 1, 1, 12, 0).atZone(zone).toInstant
      implicit val clock: Clock = Clock.fixed(date, zone)
      forAll { r: ReturnsRequest =>
        val json = Json.toJson(r)
        assert((json \ "periodKey").as[String] == "18C1")
      }
    }

    "Period key is second quarter when date is equal to or after 1st April and before 1st July" in {
      val date = LocalDateTime.of(2018, 6, 30, 12, 0).atZone(zone).toInstant
      implicit val clock: Clock = Clock.fixed(date, zone)
      forAll { r: ReturnsRequest =>
        val json = Json.toJson(r)
        assert((json \ "periodKey").as[String] == "18C2")
      }
    }

    "Period key is third quarter when date is equal to or after 1st July and before 1st October" in {
      val date = LocalDateTime.of(2018, 7, 1, 12, 0).atZone(zone).toInstant
      implicit val clock: Clock = Clock.fixed(date, zone)
      forAll { r: ReturnsRequest =>
        val json = Json.toJson(r)
        assert((json \ "periodKey").as[String] == "18C3")
      }
    }

    "Period key is fourth quarter when date is equal to or after 1st October and equal to or before 31st December" in {
      val date = LocalDateTime.of(2018, 12, 31, 12, 0).atZone(zone).toInstant
      implicit val clock: Clock = Clock.fixed(date, zone)
      forAll { r: ReturnsRequest =>
        val json = Json.toJson(r)
        assert((json \ "periodKey").as[String] == "18C4")
      }
    }

    // TODO: Test lowLevy and highLevy are used in monetaryWrites of models/returns.scala (as is dueLevy) to form lowVolume and highVolume within the returnsRequestFormat/writesForAuditing.
    //
    // This is then used in Line 96 of ReturnsController within the buildReturnAuditDetail method and also (more importantly) in Line 134 Json.toJson(returnsRequest) of the method submitReturn within DesConnector.
    //
    // dueLevy is also used in this way to form levySubtotal
    // monetaryWrites
    // in addition, can test netLevyDueTotal/totalLevy
    //  RELATING TO RETURNS - DO FIRST
    "packaged" should {
      "volumeSmall" in {
//        TODO: THIS IS NOT EASY
        implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(2024, 1, 1))
        forAll { r: ReturnsRequest =>
          val json = Json.toJson(r)
          assert((json \ "periodKey").as[String] == "18C4")
        }
      }

      "volumeLarge" in {
        implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(2024, 1, 1))
        forAll { r: ReturnsRequest =>
          val json = Json.toJson(r)
          r.packaged match {
            case Some(returnsPackaging) =>
              val volumeLargeFields: Seq[(String, JsValue)] = Seq(
                ("lowVolume", JsString(returnsPackaging.largeProducerVolumes._1.toString)),
                ("highVolume", JsString(returnsPackaging.largeProducerVolumes._2.toString))
              )
              assert((json \ "packaging" \ "volumeLarge").as[JsObject] == JsObject(volumeLargeFields))
//              TODO: Need to add correct assertion here
            case None => assert(true)
          }
        }
      }

      "monetaryWrites" should {
        val janToMarInt = Gen.choose(1, 3)
        val aprToDecInt = Gen.choose(4, 12)

        (2018 to 2024).foreach { year =>
          val lowerBandCostPerLitre = BigDecimal("0.18")
          val higherBandCostPerLitre = BigDecimal("0.24")

          s"write lowVolume, highVolume, and levySubtotal - using original rates for Apr - Dec $year" in {
            forAll(aprToDecInt) { month =>
              implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year, month, 1))
              forAll { r: ReturnsRequest =>
                val json = Json.toJson(r)
                r.packaged match {
                  case Some(returnsPackaging) =>
                    val lowVolumeLevy = returnsPackaging.largeProducerVolumes._1 * lowerBandCostPerLitre
                    val highVolumeLevy = returnsPackaging.largeProducerVolumes._2 * higherBandCostPerLitre
                    val levySubtotal = lowVolumeLevy + highVolumeLevy
                    val monetaryFields: Seq[(String, JsValue)] = Seq(
                      ("lowVolume", JsNumber(lowVolumeLevy)),
                      ("highVolume", JsNumber(highVolumeLevy)),
                      ("levySubtotal", JsNumber(levySubtotal))
                    )
                    assert((json \ "packaging" \ "monetaryValues").as[JsObject] == JsObject(monetaryFields))
                  //              TODO: Need to add correct assertion here
                  case None => assert(true)
                }
              }
            }
          }

          s"write lowVolume, highVolume, and levySubtotal - using original rates for Jan - Mar ${year + 1}" in {
            forAll(janToMarInt) { month =>
              implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year + 1, month, 1))
              forAll { r: ReturnsRequest =>
                val json = Json.toJson(r)
                r.packaged match {
                  case Some(returnsPackaging) =>
                    val lowVolumeLevy = returnsPackaging.largeProducerVolumes._1 * lowerBandCostPerLitre
                    val highVolumeLevy = returnsPackaging.largeProducerVolumes._2 * higherBandCostPerLitre
                    val levySubtotal = lowVolumeLevy + highVolumeLevy
                    val monetaryFields: Seq[(String, JsValue)] = Seq(
                      ("lowVolume", JsNumber(lowVolumeLevy)),
                      ("highVolume", JsNumber(highVolumeLevy)),
                      ("levySubtotal", JsNumber(levySubtotal))
                    )
                    assert((json \ "packaging" \ "monetaryValues").as[JsObject] == JsObject(monetaryFields))
                  //              TODO: Need to add correct assertion here
                  case None => assert(true)
                }
              }
            }
          }
        }

        (2025 to 2025).foreach { year =>
          val lowerBandCostPerLitreMap: Map[Int, BigDecimal] = Map(2025 -> BigDecimal("0.194"))
          val higherBandCostPerLitreMap: Map[Int, BigDecimal] = Map(2025 -> BigDecimal("0.259"))

          s"write lowVolume, highVolume, and levySubtotal - using original rates for Apr - Dec $year" in {
            forAll(aprToDecInt) { month =>
              implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year, month, 1))
              forAll { r: ReturnsRequest =>
                val json = Json.toJson(r)
                r.packaged match {
                  case Some(returnsPackaging) =>
                    val lowVolumeLevy = returnsPackaging.largeProducerVolumes._1 * lowerBandCostPerLitreMap(year)
                    val highVolumeLevy = returnsPackaging.largeProducerVolumes._2 * higherBandCostPerLitreMap(year)
                    val levySubtotal = lowVolumeLevy + highVolumeLevy
                    val monetaryFields: Seq[(String, JsValue)] = Seq(
                      ("lowVolume", JsNumber(lowVolumeLevy)),
                      ("highVolume", JsNumber(highVolumeLevy)),
                      ("levySubtotal", JsNumber(levySubtotal))
                    )
                    assert((json \ "packaging" \ "monetaryValues").as[JsObject] == JsObject(monetaryFields))
                  //              TODO: Need to add correct assertion here
                  case None => assert(true)
                }
              }
            }
          }

          s"write lowVolume, highVolume, and levySubtotal - using original rates for Jan - Mar ${year + 1}" in {
            forAll(janToMarInt) { month =>
              implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year + 1, month, 1))
              forAll { r: ReturnsRequest =>
                val json = Json.toJson(r)
                r.packaged match {
                  case Some(returnsPackaging) =>
                    val lowVolumeLevy = returnsPackaging.largeProducerVolumes._1 * lowerBandCostPerLitreMap(year)
                    val highVolumeLevy = returnsPackaging.largeProducerVolumes._2 * higherBandCostPerLitreMap(year)
                    val levySubtotal = lowVolumeLevy + highVolumeLevy
                    val monetaryFields: Seq[(String, JsValue)] = Seq(
                      ("lowVolume", JsNumber(lowVolumeLevy)),
                      ("highVolume", JsNumber(highVolumeLevy)),
                      ("levySubtotal", JsNumber(levySubtotal))
                    )
                    assert((json \ "packaging" \ "monetaryValues").as[JsObject] == JsObject(monetaryFields))
                  //              TODO: Need to add correct assertion here
                  case None => assert(true)
                }
              }
            }
          }
        }
      }
    }

    "imported" should {
      "volumeSmall" in {
        implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(2024, 1, 1))
        forAll { r: ReturnsRequest =>
          val json = Json.toJson(r)
          r.imported match {
            case Some(returnsImporting) =>
              val volumeSmallFields: Seq[(String, JsValue)] = Seq(
                ("lowVolume", JsString(returnsImporting.smallProducerVolumes._1.toString)),
                ("highVolume", JsString(returnsImporting.smallProducerVolumes._2.toString))
              )
              assert((json \ "importing" \ "volumeSmall").as[JsObject] == JsObject(volumeSmallFields))
            //              TODO: Need to add correct assertion here
            case None => assert(true)
          }
        }
      }

      "volumeLarge" in {
        implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(2024, 1, 1))
        forAll { r: ReturnsRequest =>
          val json = Json.toJson(r)
          r.imported match {
            case Some(returnsImporting) =>
              val volumeLargeFields: Seq[(String, JsValue)] = Seq(
                ("lowVolume", JsString(returnsImporting.largeProducerVolumes._1.toString)),
                ("highVolume", JsString(returnsImporting.largeProducerVolumes._2.toString))
              )
              assert((json \ "importing" \ "volumeLarge").as[JsObject] == JsObject(volumeLargeFields))
            //              TODO: Need to add correct assertion here
            case None => assert(true)
          }
        }
      }

      "monetaryWrites" should {
        val janToMarInt = Gen.choose(1, 3)
        val aprToDecInt = Gen.choose(4, 12)

        (2018 to 2024).foreach { year =>
          val lowerBandCostPerLitre = BigDecimal("0.18")
          val higherBandCostPerLitre = BigDecimal("0.24")

          s"write lowVolume, highVolume, and levySubtotal - using original rates for Apr - Dec $year" in {
            forAll(aprToDecInt) { month =>
              implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year, month, 1))
              forAll { r: ReturnsRequest =>
                val json = Json.toJson(r)
                r.imported match {
                  case Some(returnsImporting) =>
                    val lowVolumeLevy = returnsImporting.largeProducerVolumes._1 * lowerBandCostPerLitre
                    val highVolumeLevy = returnsImporting.largeProducerVolumes._2 * higherBandCostPerLitre
                    val levySubtotal = lowVolumeLevy + highVolumeLevy
                    val monetaryFields: Seq[(String, JsValue)] = Seq(
                      ("lowVolume", JsNumber(lowVolumeLevy)),
                      ("highVolume", JsNumber(highVolumeLevy)),
                      ("levySubtotal", JsNumber(levySubtotal))
                    )
                    assert((json \ "importing" \ "monetaryValues").as[JsObject] == JsObject(monetaryFields))
                  //              TODO: Need to add correct assertion here
                  case None => assert(true)
                }
              }
            }
          }

          s"write lowVolume, highVolume, and levySubtotal - using original rates for Jan - Mar ${year + 1}" in {
            forAll(janToMarInt) { month =>
              implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year + 1, month, 1))
              forAll { r: ReturnsRequest =>
                val json = Json.toJson(r)
                r.imported match {
                  case Some(returnsImporting) =>
                    val lowVolumeLevy = returnsImporting.largeProducerVolumes._1 * lowerBandCostPerLitre
                    val highVolumeLevy = returnsImporting.largeProducerVolumes._2 * higherBandCostPerLitre
                    val levySubtotal = lowVolumeLevy + highVolumeLevy
                    val monetaryFields: Seq[(String, JsValue)] = Seq(
                      ("lowVolume", JsNumber(lowVolumeLevy)),
                      ("highVolume", JsNumber(highVolumeLevy)),
                      ("levySubtotal", JsNumber(levySubtotal))
                    )
                    assert((json \ "importing" \ "monetaryValues").as[JsObject] == JsObject(monetaryFields))
                  //              TODO: Need to add correct assertion here
                  case None => assert(true)
                }
              }
            }
          }
        }

        (2025 to 2025).foreach { year =>
          val lowerBandCostPerLitreMap: Map[Int, BigDecimal] = Map(2025 -> BigDecimal("0.194"))
          val higherBandCostPerLitreMap: Map[Int, BigDecimal] = Map(2025 -> BigDecimal("0.259"))

          s"write lowVolume, highVolume, and levySubtotal - using original rates for Apr - Dec $year" in {
            forAll(aprToDecInt) { month =>
              implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year, month, 1))
              forAll { r: ReturnsRequest =>
                val json = Json.toJson(r)
                r.imported match {
                  case Some(returnsImporting) =>
                    val lowVolumeLevy = returnsImporting.largeProducerVolumes._1 * lowerBandCostPerLitreMap(year)
                    val highVolumeLevy = returnsImporting.largeProducerVolumes._2 * higherBandCostPerLitreMap(year)
                    val levySubtotal = lowVolumeLevy + highVolumeLevy
                    val monetaryFields: Seq[(String, JsValue)] = Seq(
                      ("lowVolume", JsNumber(lowVolumeLevy)),
                      ("highVolume", JsNumber(highVolumeLevy)),
                      ("levySubtotal", JsNumber(levySubtotal))
                    )
                    assert((json \ "importing" \ "monetaryValues").as[JsObject] == JsObject(monetaryFields))
                  //              TODO: Need to add correct assertion here
                  case None => assert(true)
                }
              }
            }
          }

          s"write lowVolume, highVolume, and levySubtotal - using original rates for Jan - Mar ${year + 1}" in {
            forAll(janToMarInt) { month =>
              implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year + 1, month, 1))
              forAll { r: ReturnsRequest =>
                val json = Json.toJson(r)
                r.imported match {
                  case Some(returnsImporting) =>
                    val lowVolumeLevy = returnsImporting.largeProducerVolumes._1 * lowerBandCostPerLitreMap(year)
                    val highVolumeLevy = returnsImporting.largeProducerVolumes._2 * higherBandCostPerLitreMap(year)
                    val levySubtotal = lowVolumeLevy + highVolumeLevy
                    val monetaryFields: Seq[(String, JsValue)] = Seq(
                      ("lowVolume", JsNumber(lowVolumeLevy)),
                      ("highVolume", JsNumber(highVolumeLevy)),
                      ("levySubtotal", JsNumber(levySubtotal))
                    )
                    assert((json \ "importing" \ "monetaryValues").as[JsObject] == JsObject(monetaryFields))
                  //              TODO: Need to add correct assertion here
                  case None => assert(true)
                }
              }
            }
          }
        }
      }
    }

    "exported" should {
      "volumes" in {
        implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(2024, 1, 1))
        forAll { r: ReturnsRequest =>
          val json = Json.toJson(r)
          r.exported match {
            case Some(returnsExported) =>
              val volumesFields: Seq[(String, JsValue)] = Seq(
                ("lowVolume", JsString(returnsExported._1.toString)),
                ("highVolume", JsString(returnsExported._2.toString))
              )
              assert((json \ "exporting" \ "volumes").as[JsObject] == JsObject(volumesFields))
            //              TODO: Need to add correct assertion here
            case None => assert(true)
          }
        }
      }

      "monetaryWrites" should {
        val janToMarInt = Gen.choose(1, 3)
        val aprToDecInt = Gen.choose(4, 12)

        (2018 to 2024).foreach { year =>
          val lowerBandCostPerLitre = BigDecimal("0.18")
          val higherBandCostPerLitre = BigDecimal("0.24")

          s"write lowVolume, highVolume, and levySubtotal - using original rates for Apr - Dec $year" in {
            forAll(aprToDecInt) { month =>
              implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year, month, 1))
              forAll { r: ReturnsRequest =>
                val json = Json.toJson(r)
                r.exported match {
                  case Some(returnsExported) =>
                    val lowVolumeLevy = returnsExported._1 * lowerBandCostPerLitre
                    val highVolumeLevy = returnsExported._2 * higherBandCostPerLitre
                    val levySubtotal = lowVolumeLevy + highVolumeLevy
                    val monetaryFields: Seq[(String, JsValue)] = Seq(
                      ("lowVolume", JsNumber(lowVolumeLevy)),
                      ("highVolume", JsNumber(highVolumeLevy)),
                      ("levySubtotal", JsNumber(levySubtotal))
                    )
                    assert((json \ "exporting" \ "monetaryValues").as[JsObject] == JsObject(monetaryFields))
                  //              TODO: Need to add correct assertion here
                  case None => assert(true)
                }
              }
            }
          }

          s"write lowVolume, highVolume, and levySubtotal - using original rates for Jan - Mar ${year + 1}" in {
            forAll(janToMarInt) { month =>
              implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year + 1, month, 1))
              forAll { r: ReturnsRequest =>
                val json = Json.toJson(r)
                r.exported match {
                  case Some(returnsExported) =>
                    val lowVolumeLevy = returnsExported._1 * lowerBandCostPerLitre
                    val highVolumeLevy = returnsExported._2 * higherBandCostPerLitre
                    val levySubtotal = lowVolumeLevy + highVolumeLevy
                    val monetaryFields: Seq[(String, JsValue)] = Seq(
                      ("lowVolume", JsNumber(lowVolumeLevy)),
                      ("highVolume", JsNumber(highVolumeLevy)),
                      ("levySubtotal", JsNumber(levySubtotal))
                    )
                    assert((json \ "exporting" \ "monetaryValues").as[JsObject] == JsObject(monetaryFields))
                  //              TODO: Need to add correct assertion here
                  case None => assert(true)
                }
              }
            }
          }
        }

        (2025 to 2025).foreach { year =>
          val lowerBandCostPerLitreMap: Map[Int, BigDecimal] = Map(2025 -> BigDecimal("0.194"))
          val higherBandCostPerLitreMap: Map[Int, BigDecimal] = Map(2025 -> BigDecimal("0.259"))

          s"write lowVolume, highVolume, and levySubtotal - using original rates for Apr - Dec $year" in {
            forAll(aprToDecInt) { month =>
              implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year, month, 1))
              forAll { r: ReturnsRequest =>
                val json = Json.toJson(r)
                r.exported match {
                  case Some(returnsExported) =>
                    val lowVolumeLevy = returnsExported._1 * lowerBandCostPerLitreMap(year)
                    val highVolumeLevy = returnsExported._2 * higherBandCostPerLitreMap(year)
                    val levySubtotal = lowVolumeLevy + highVolumeLevy
                    val monetaryFields: Seq[(String, JsValue)] = Seq(
                      ("lowVolume", JsNumber(lowVolumeLevy)),
                      ("highVolume", JsNumber(highVolumeLevy)),
                      ("levySubtotal", JsNumber(levySubtotal))
                    )
                    assert((json \ "exporting" \ "monetaryValues").as[JsObject] == JsObject(monetaryFields))
                  //              TODO: Need to add correct assertion here
                  case None => assert(true)
                }
              }
            }
          }

          s"write lowVolume, highVolume, and levySubtotal - using original rates for Jan - Mar ${year + 1}" in {
            forAll(janToMarInt) { month =>
              implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(year + 1, month, 1))
              forAll { r: ReturnsRequest =>
                val json = Json.toJson(r)
                r.exported match {
                  case Some(returnsExported) =>
                    val lowVolumeLevy = returnsExported._1 * lowerBandCostPerLitreMap(year)
                    val highVolumeLevy = returnsExported._2 * higherBandCostPerLitreMap(year)
                    val levySubtotal = lowVolumeLevy + highVolumeLevy
                    val monetaryFields: Seq[(String, JsValue)] = Seq(
                      ("lowVolume", JsNumber(lowVolumeLevy)),
                      ("highVolume", JsNumber(highVolumeLevy)),
                      ("levySubtotal", JsNumber(levySubtotal))
                    )
                    assert((json \ "exporting" \ "monetaryValues").as[JsObject] == JsObject(monetaryFields))
                  //              TODO: Need to add correct assertion here
                  case None => assert(true)
                }
              }
            }
          }
        }
      }
    }

    "wastage" should {
      "volumes" in {
        implicit val returnPeriod: ReturnPeriod = ReturnPeriod(LocalDate.of(2024, 1, 1))
        forAll { r: ReturnsRequest =>
          val json = Json.toJson(r)
          r.wastage match {
            case Some(returnsWastage) =>
              val volumesFields: Seq[(String, JsValue)] = Seq(
                ("lowVolume", JsString(returnsWastage._1.toString)),
                ("highVolume", JsString(returnsWastage._2.toString))
              )
              assert((json \ "wastage" \ "volumes").as[JsObject] == JsObject(volumesFields))
            //              TODO: Need to add correct assertion here
            case None => assert(true)
          }
        }
      }

      "monetaryWrites" should {
        "lowLevy" should {}

        "highLevy" should {}

        "dueLevy" should {}
      }
    }

    "netLevyDueTotal" should {}
  }
}
