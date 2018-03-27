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

import play.api.libs.json._
import uk.gov.hmrc.softdrinksindustrylevy.models.{ActivityType, _}

package object returns {
  implicit val importingFormat: Format[ReturnsImporting] = new Format[ReturnsImporting] {
    override def reads(json: JsValue): JsResult[ReturnsImporting] = ???

    override def writes(o: ReturnsImporting): JsValue = ???
  }

  implicit val returnsRequestFormat: Format[ReturnsRequest] = new Format[ReturnsRequest] {
    override def reads(json: JsValue): JsResult[ReturnsRequest] = {
      def litreReads(json: JsValue): LitreBands =
        ((json \ "lowRateVolume").as[Litres], (json \ "highRateVolume").as[Litres])

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

      def otherActivity(activityType: ActivityType.Value): Map[ActivityType.Value, LitreBands] = {
        json \ activityType.toString.toLowerCase match {
          case JsDefined(activityJs) =>
            Map(activityType -> litreReads(activityJs))
          case _ => Map.empty
        }
      }

      JsSuccess(ReturnsRequest(returnsPackaging, returnsImporting, otherActivity(ActivityType.Exported) ++ otherActivity(ActivityType.Wastage)))
    }

    override def writes(o: ReturnsRequest): JsValue = {
      Json.obj(
        "formBundleType" -> "ZSD1"
      )
    }
  }
}
