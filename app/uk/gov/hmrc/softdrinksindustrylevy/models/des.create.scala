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

package uk.gov.hmrc.softdrinksindustrylevy.models.json.des

import play.api.libs.json._
import uk.gov.hmrc.softdrinksindustrylevy.models._
import java.time.{LocalDate => Date}

package object create {

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

  implicit val subscriptionFormat: Format[Subscription] = new Format[Subscription] {
    def reads(json: JsValue): JsResult[Subscription] = ???
    def writes(s: Subscription): JsValue = {

      def isProducer: Boolean = {
        import ActivityType._
        List(ProducedOwnBrand, CopackerAll, CopackerSmall).
          foldLeft(false){ case (acc, t) =>
            acc || s.activity.contains(t)
          }
      }

      def isLarge: Boolean =
        s.upperLitres + s.lowerLitres >= 1000000

      def activityMap = {
        import ActivityType._
        Map(
          "Produced" -> ((s.lowerLitres, s.upperLitres)),
          "Imported" -> s.activity.getOrElse(Imported, (0L,0L)),
          "Packaged" -> s.activity.getOrElse(ProducedOwnBrand, (0L,0L))
        ).flatMap {
          case (_,(0L,0L)) => Map.empty[String,JsValue]
          case (k,(l,h))   => Map(
            s"litres${k}UKLower" -> JsNumber(l),
            s"litres${k}UKHigher" -> JsNumber(h)
          )
        }
      }

      JsObject(Map(
        "registration" -> JsObject(Map(
          "organisationType" -> JsString("1"), // TODO!
          "applicationDate" -> JsString(Date.now.toString),
          "taxStartDate" -> JsString(s.liabilityDate.toString), 
          "cin" -> JsString(s.utr),
          "tradingName" -> JsString(s.orgName),
          "businessContact" -> JsObject(Map(
            "addressDetails" -> Json.toJson(s.address),
            "contactDetails" -> JsObject(Map(
              "telephone" -> JsString(s.contact.phoneNumber),
              "email" -> JsString(s.contact.email)
            ))
          )),
          "primaryPersonContact" -> JsObject(Map(
            "name" -> Json.toJson(s.contact.name),
            "telephone" -> JsString(s.contact.phoneNumber),
            "email" -> JsString(s.contact.email),
            "positionInCompany" -> JsString(s.contact.positionInCompany)
          )),
          "details" -> JsObject(Map(
            "producer" -> JsBoolean{isProducer},
            "producerDetails" -> JsObject(Map(
              "produceMillionLitres" ->
                JsBoolean(isLarge), 
              "producerClassification" ->
                JsString(if (isLarge) "1" else "0"),
              "smallProducerExemption" ->
                JsBoolean(!isLarge)
            )),
            "importer" ->
              JsBoolean(s.activity.contains(ActivityType.Imported)),
            "contractPacker" ->
              JsBoolean(s.activity.contains(ActivityType.Copackee))
          )),
          "activityQuestions" -> JsObject(activityMap),
          "estimatedTaxAmount" -> JsNumber(s.taxEstimatePounds),
          "taxObligationStartDate" -> JsString(s.liabilityDate.toString)
        ))
      ))
    }
  }

  implicit val createSubscriptionResponseFormat: OFormat[CreateSubscriptionResponse] =
    Json.format[CreateSubscriptionResponse]
    
}
