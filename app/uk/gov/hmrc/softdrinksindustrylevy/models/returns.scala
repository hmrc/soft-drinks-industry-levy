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

package uk.gov.hmrc.softdrinksindustrylevy.models.json.des

import cats.Monoid
import cats.implicits._
import play.api.libs.json._
import sdil.models.{ReturnPeriod, SdilReturn}
import uk.gov.hmrc.softdrinksindustrylevy.models._

package object returns {

  implicit def writesForAuditing(implicit period: ReturnPeriod, sdilReturn: SdilReturn): Writes[ReturnsRequest] =
    new Writes[ReturnsRequest] {
      override def writes(o: ReturnsRequest): JsValue = {
        val ownBrand = returnsRequestFormat
          .writes(o)
          .as[JsObject]
          .transform(
            (__ \ "packaging" \ "ownBrand").json.put(Json.toJson(sdilReturn).as[JsObject].value("ownBrand"))
          )
          .get

        returnsRequestFormat.writes(o).as[JsObject].deepMerge(ownBrand)
      }
    }

  implicit def returnsRequestFormat(implicit period: ReturnPeriod): Format[ReturnsRequest] =
    new Format[ReturnsRequest] {
      override def reads(json: JsValue): JsResult[ReturnsRequest] = {
        def litreReads(json: JsValue): LitreBands = (
          (json \ "lowRateVolume").as[Long],
          (json \ "highRateVolume").as[Long]
        )

        val returnsPackaging: Option[ReturnsPackaging] = json \ "packaged" match {
          case JsDefined(packagedJs) =>
            val vols = (packagedJs \ "volumesForSmallProducers").as[Seq[JsValue]]
            val smallProducerVolumes = vols map { packaged =>
              val ref = (packaged \ "producerRef").as[String]
              val litres = litreReads(packaged)
              SmallProducerVolume(ref, litres)
            }

            val largeProducerVolumes = litreReads((packagedJs \ "volumesForLargeProducers").get)

            Some(ReturnsPackaging(smallProducerVolumes, largeProducerVolumes))
          case _ => None
        }

        val returnsImporting: Option[ReturnsImporting] = json \ "imported" match {
          case JsDefined(importJs) =>
            val smallProdImports = litreReads((importJs \ "volumesForSmallProducers").get)
            val largeProdImports = litreReads((importJs \ "volumesForLargeProducers").get)

            Some(ReturnsImporting(smallProdImports, largeProdImports))
          case _ => None
        }

        def volumeBlock(activity: ActivityType.Value): Option[LitreBands] =
          json \ activity.toString.toLowerCase match {
            case JsDefined(act) => Some(litreReads(act))
            case _              => None
          }

        JsSuccess(
          ReturnsRequest(
            returnsPackaging,
            returnsImporting,
            volumeBlock(ActivityType.Exporting),
            volumeBlock(ActivityType.Wastage)
          ))
      }

      override def writes(o: ReturnsRequest): JsValue = {
        def litresWrites(litres: LitreBands): JsObject = Json.obj(
          "lowVolume"  -> litres._1.toString,
          "highVolume" -> litres._2.toString
        )

        def monetaryWrites(litreBands: LitreBands*)(implicit mb: Monoid[BigDecimal]): JsObject =
          Json.obj(
            "lowVolume"    -> litreBands.foldLeft(mb.empty)(_ + _.lowLevy),
            "highVolume"   -> litreBands.foldLeft(mb.empty)(_ + _.highLevy),
            "levySubtotal" -> litreBands.foldLeft(mb.empty)(_ + _.dueLevy)
          )

        def optLitreObj(litres: Option[LitreBands], activityType: ActivityType.Value) =
          litres.fold(Json.obj()) { l =>
            Json.obj(
              activityType.toString.toLowerCase -> Json.obj(
                "volumes"        -> litresWrites(l),
                "monetaryValues" -> monetaryWrites(l)
              )
            )
          }

        val packaged = o.packaged.fold(Json.obj()) { p =>
          Json.obj(
            "packaging" -> Json.obj(
              "volumeSmall" -> p.smallProducerVolumes.map { vols =>
                Json.obj(
                  "producerRef" -> vols.producerRef
                ) ++ litresWrites(vols.volumes)
              },
              "volumeLarge"    -> litresWrites(p.largeProducerVolumes),
              "monetaryValues" -> monetaryWrites(p.largeProducerVolumes)
            )
          )
        }

        val imported = o.imported.fold(Json.obj()) { i =>
          Json.obj(
            "importing" -> Json.obj(
              "volumeSmall"    -> litresWrites(i.smallProducerVolumes),
              "volumeLarge"    -> litresWrites(i.largeProducerVolumes),
              "monetaryValues" -> monetaryWrites(i.largeProducerVolumes)
            )
          )
        }

        val exported = optLitreObj(o.exported, ActivityType.Exporting)
        val wastage = optLitreObj(o.wastage, ActivityType.Wastage)

        val quarter = period.desPeriodKey

        Json.obj(
          "periodKey"       -> quarter,
          "formBundleType"  -> "ZSD1",
          "netLevyDueTotal" -> o.totalLevy
        ) ++ packaged ++ imported ++ exported ++ wastage
      }
    }
}
