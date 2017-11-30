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

package uk.gov.hmrc.softdrinksindustrylevy

import play.api.libs.json._

package object models {

  type Litres = Long
  type PhoneNo = String
  type EmailAddress = String
  type LitreBands = (Litres, Litres)
  type Activity = Map[ActivityType.Value, LitreBands]

  // SDIL create and retrieve subscription formatters
  implicit val addressFormat = new Format[Address] {
    def reads(json: JsValue): JsResult[Address] = {

      val lines = List(
        Some((json \ "line1").as[String]), 
        Some((json \ "line2").as[String]),
        (json \ "line3").asOpt[String], 
        (json \ "line4").asOpt[String]
      ).flatten

      val country = (json \ "country").asOpt[String].map(_.toUpperCase)
      val nonUk = (json \ "notUKAddress").as[Boolean]      
      (country, nonUk) match {
        case (Some("GB"), true) => JsError("Country code is GB, but notUKAddress is true")
        case (Some("GB") | None, false) => JsSuccess(UkAddress( lines, (json \ "postCode").as[String] ))
        case (Some(_), false) => JsError("Country code is not GB, but notUKAddress is false")
        case (None, true) => JsError("notUKAddress is true, but no country is supplied")          
        case (Some(c), true) => JsSuccess(ForeignAddress( lines, c ))
      }
    }

    def writes(address: Address): JsValue = {

      val jsLines = address.lines.zipWithIndex.map{ case (v,i) =>
        s"line${i + 1}" -> JsString(v)
      }

      JsObject(
        { address match {
          case UkAddress(_, postCode) => List(
            "notUKAddress" -> JsBoolean(false),
            "postCode" -> JsString(postCode)              
          )
          case ForeignAddress(_, country) => List(
            "notUKAddress" -> JsBoolean(true),
            "country" -> JsString(country)
          )
        } } ::: jsLines
      )
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

  implicit val activityMapFormat: Format[Activity] = new Format[Activity] {
    def reads(json: JsValue): JsResult[Activity] = JsSuccess {
      ActivityType.values.map{ at =>
        (json \ at.toString).asOpt[LitreBands].map{at -> _}
      }.flatten.toMap
    }
    def writes(address: Activity): JsValue = JsObject(
      address.map{ case (t,litreBand) =>
        t.toString -> litreBandsFormat.writes(litreBand)
      }
    )
  }

  implicit val subscriptionFormat: OFormat[Subscription] = Json.format[Subscription]
  implicit val createSubscriptionResponseFormat: OFormat[CreateSubscriptionResponse] =
    Json.format[CreateSubscriptionResponse]
    
  implicit def addressToSite(ad: Address): Site = Site(ad)

}
