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
import uk.gov.hmrc.softdrinksindustrylevy.models.json.internal._ // TODO remove this import and implement missing formatters



//Reads the DES subscription create JSON to create a Subscription and writes it back

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
    def reads(json: JsValue): JsResult[Subscription] = {
//      def activityType = {
//
//        InternalActivity(ActivityType.values.map{ at =>
//          (json \ at.toString).asOpt[LitreBands].map{at -> _}
//        }.flatten.toMap)
//
//
////        json \ "activity" match {
////          case JsDefined(JsArray(arr)) => arr.toList.collect {
////            case obj: JsObject if {obj \ "siteType"}.as[String] == siteType => obj.as[Site]
////          }
////          case _ => List.empty[Site]
////        }
////
////
////        val smallProducer =(json \ "subscriptionDetails" \ "smallProducer").as[Boolean]
////        val largeProducer = (json \ "subscriptionDetails" \ "largeProducer").as[Boolean]
////        val contractPacker = (json \ "subscriptionDetails" \ "contractPacker").as[Boolean]
////        val importer = (json \ "subscriptionDetails" \ "importer").as[Boolean]
////        InternalActivity()
//      }

      JsSuccess(Subscription(
        utr = (json \ "utr").as[String],
        orgName = (json \ "orgName").as[String],
        address = (json \ "address").as[Address],
        activity = (json \ "activity").as[Activity], // TODO this is wrong
        liabilityDate = (json \ "liabilityDate").as[Date],
        productionSites = (json \ "productionSites").as[List[Site]],
        warehouseSites = (json \ "warehouseSites").as[List[Site]],
        contact = (json).as[Contact]
      ))

    }


    def writes(s: Subscription): JsValue = {

      def activityMap = {
        import ActivityType._
        s.activity match {
          case a: InternalActivity => Map(
            "Produced" -> a.sumOfLiableLitreRates,
            "Imported" -> a.activity.getOrElse(Imported, (0L, 0L)),
            "Packaged" -> a.activity.getOrElse(ProducedOwnBrand, (0L, 0L))
          ).flatMap {
            case (k, (l, h)) => Map(
              s"litres${k}UKLower" -> JsNumber(l),
              s"litres${k}UKHigher" -> JsNumber(h)
            )
          }
          case _ => Map.empty[String, JsValue]
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
            "positionInCompany" -> JsString(s.contact.positionInCompany.getOrElse(""))
          )),
          "details" -> JsObject(Map(
            "producer" -> JsBoolean{s.activity.isProducer},
            "producerDetails" -> JsObject(Map(
              "produceMillionLitres" ->
                JsBoolean(s.activity.isLarge),
              "producerClassification" ->
                JsString(if (s.activity.isLarge) "1" else "0"),
              "smallProducerExemption" ->
                JsBoolean(!s.activity.isLarge)
            )),
            "importer" ->
              JsBoolean(s.activity.isImporter),
            "contractPacker" ->
              JsBoolean(s.activity.isContractPacker)
          )),
          "activityQuestions" -> JsObject(activityMap), // TODO here...
          "estimatedTaxAmount" -> JsNumber(s.activity.taxEstimation),
          "taxObligationStartDate" -> JsString(s.liabilityDate.toString)
        ))
      ))
    }
  }

  implicit val createSubscriptionResponseFormat: OFormat[CreateSubscriptionResponse] =
    Json.format[CreateSubscriptionResponse]
    
}
