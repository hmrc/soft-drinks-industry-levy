/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.softdrinksindustrylevy.models.json

import play.api.libs.json._
import uk.gov.hmrc.softdrinksindustrylevy.models._


package object internal {

  // SDIL create and retrieve subscription formatters
  implicit val addressFormat = new Format[Address] {
    lazy val ukAddressFormat = Json.format[UkAddress]
    lazy val foreignAddressFormat = Json.format[ForeignAddress]

    def reads(json: JsValue): JsResult[Address] =
      (json \ "country").asOpt[String].map(_.toLowerCase) match {
        case Some("uk") | None => ukAddressFormat.reads(json)
        case _ => foreignAddressFormat.reads(json)
      }
    
    def writes(address: Address): JsValue = address match {
      case uk: UkAddress => ukAddressFormat.writes(uk)
      case foreign: ForeignAddress => foreignAddressFormat.writes(foreign)
    }
  }

  implicit val litreBandsFormat: Format[LitreBands] = new Format[LitreBands] {
    def reads(json: JsValue): JsResult[LitreBands] = JsSuccess( (
      (json \ "lower").as[Litres],
      (json \ "upper").as[Litres]        
    ) )
    def writes(value: LitreBands): JsValue = JsObject(
      List("lower" -> JsNumber(value._1), "upper" -> JsNumber(value._2))
    )
  }

  implicit val businessContactFormat: OFormat[Contact] = Json.format[Contact]
  implicit val siteFormat: OFormat[Site] = Json.format[Site]

//  implicit val activityMapFormat: Format[Activity] = new Format[Activity] {
//    def reads(json: JsValue): JsResult[Activity] = JsSuccess {
//      ActivityType.values.map{ at =>
//        (json \ at.toString).asOpt[LitreBands].map{at -> _}
//      }.flatten.toMap
//    }
//    def writes(address: Activity): JsValue = JsObject(
//      address.map{ case (t,litreBand) =>
//        t.toString -> litreBandsFormat.writes(litreBand)
//      }
//    )
//  }

  implicit val activityMapFormat: Format[Activity] = new Format[Activity] {
    def reads(json: JsValue): JsResult[Activity] = JsSuccess {
          // if json contains internal model.. TODO what to do with other
            InternalActivity(ActivityType.values.map{ at =>
               (json \ at.toString).asOpt[LitreBands].map{at -> _}
            }.flatten.toMap)

          }


    def writes(activity: Activity): JsValue = JsObject(
      activity match {
        case InternalActivity(a) => a.map { case (t, lb) =>
          t.toString -> litreBandsFormat.writes(lb)
        }
        case a:RetrievedActivity => Map(
          "voluntaryRegistration" -> JsBoolean(a.isVoluntaryRegistration),
          "smallProducer" -> JsBoolean(a.isSmallProducer),
          "largeProducer" -> JsBoolean(a.isLarge),
          "contractPacker" -> JsBoolean(a.isContractPacker),
          "importer" -> JsBoolean(a.isImporter)
        )
      }
    )
  }

  implicit val subscriptionFormat: OFormat[Subscription] = Json.format[Subscription]
    
}
