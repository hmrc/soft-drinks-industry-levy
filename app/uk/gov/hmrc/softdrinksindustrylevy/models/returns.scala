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

package uk.gov.hmrc.softdrinksindustrylevy.models.json.des

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import play.api.libs.json._
import uk.gov.hmrc.softdrinksindustrylevy.models._

package object returns {
  implicit val returnsRequestFormat: Format[ReturnsRequest] = new Format[ReturnsRequest] {
    override def reads(json: JsValue): JsResult[ReturnsRequest] = {
      def litreReads(json: JsValue): VolumeBands = VolumeBands(
        (json \ "lowRateVolume").as[Long],
        (json \ "highRateVolume").as[Long]
      )

      val returnsPackaging: Option[ReturnsPackaging] = json \ "packaged" match {
        case JsDefined(packagedJs) =>
          val vols = (packagedJs \ "volumesForSmallProducers").as[Seq[JsValue]]
          val smallProducerVolumes = vols map {
            packaged =>
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

      def volumeBlock(activity: ActivityType.Value): Option[VolumeBands] = {
        json \ activity.toString.toLowerCase match {
          case JsDefined(act) => Some(litreReads(act))
          case _ => None
        }
      }

      JsSuccess(ReturnsRequest(
        returnsPackaging,
        returnsImporting,
        volumeBlock(ActivityType.Exported),
        volumeBlock(ActivityType.Wastage)
      ))
    }

    override def writes(o: ReturnsRequest): JsValue = {
      def litresWrites(litres: VolumeBands): JsObject = Json.obj(
        "lowVolume" -> litres.low.toString,
        "highVolume" -> litres.high.toString
      )

      def monetaryWrites(litreBands: VolumeBands*): JsObject = {
        val zero = BigDecimal(0)

        Json.obj(
          "lowVolume" -> litreBands.foldLeft(zero)(_ + _.lowLevy),
          "highVolume" -> litreBands.foldLeft(zero)(_ + _.highLevy),
          "levySubtotal" -> litreBands.foldLeft(zero)(_ + _.dueLevy)
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
            "volumeLarge" -> litresWrites(p.largeProducerVolumes),
            "monetaryValues" -> monetaryWrites(p.largeProducerVolumes, p.totalSmallProdVolumes)
          )
        )
      }

      val imported = o.imported.fold(Json.obj()) { i =>
        Json.obj(
          "importing" -> Json.obj(
            "volumeSmall" -> litresWrites(i.smallProducerVolumes),
            "volumeLarge" -> litresWrites(i.largeProducerVolumes),
            "monetaryValues" -> monetaryWrites(i.smallProducerVolumes, i.largeProducerVolumes)
          )
        )
      }

      val exported = o.exported.fold(Json.obj()) { e =>
        Json.obj(
          "exporting" -> Json.obj(
            "volumes" -> litresWrites(e),
            "monetaryValues" -> monetaryWrites(e)
          )
        )
      }

      val wastage = o.wastage.fold(Json.obj()) { w =>
        Json.obj(
          "wastage" -> Json.obj(
            "volumes" -> litresWrites(w),
            "monetaryValues" -> monetaryWrites(w)
          )
        )
      }

      Json.obj(
        "periodKey" -> LocalDate.now.format(DateTimeFormatter.ofPattern("yy'C'q")),
        "formBundleType" -> "ZSD1",
        "netLevyDueTotal" -> o.totalLevy
      ) ++ packaged ++ imported ++ exported ++ wastage
    }
  }
}
