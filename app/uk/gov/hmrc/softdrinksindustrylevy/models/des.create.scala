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
import scala.language.implicitConversions

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

      val reg = json \ "registration"
      
      val activity: Activity = {
        import ActivityType._
        def litres(field: String) =
          {reg \ "activityQuestions" \ field}.as[Litres]

        Map (
          Imported -> "Imported",
          ProducedOwnBrand -> "Produced"
        ) mapValues {
          k => (
            litres(s"litres${k}UKLower"),
            litres(s"litres${k}UKHigher")
          )
        }
      }

      val primaryPerson = reg \ "primaryPersonContact"

      JsSuccess(Subscription(
        {reg \ "cin"}.as[String],
        {reg \ "tradingName"}.as[String],
        {reg \ "businessContact" \ "addressDetails"}.as[Address],
        activity,
        {reg \ "taxStartDate"}.as[Date],
        Nil: List[Site], // TODO
        Nil: List[Site], // TODO
        Contact(
          {primaryPerson \ "name"}.as[String],
          {primaryPerson \ "positionInCompany"}.as[String],
          {primaryPerson \ "telephone"}.as[String],
          {primaryPerson \ "email"}.as[String]
        )
      ))
    }

    def writes(s: Subscription): JsValue = {
      import ActivityType._
      def isProducer: Boolean = {

        List(ProducedOwnBrand, CopackerAll, CopackerSmall).
          foldLeft(false){ case (acc, t) =>
            acc || s.activity.contains(t)
          }
      }

      def isLarge: Boolean =
        s.upperLitres + s.lowerLitres >= 1000000

      def activityMap = 
        Map(
          "Produced" -> ((s.lowerLitres, s.upperLitres)),
          "Imported" -> s.activity.getOrElse(Imported, (0L,0L)),
          "Packaged" -> s.activity.getOrElse(ProducedOwnBrand, (0L,0L))
        ).flatMap {
          case (_,(0L,0L)) => Map.empty[String,Litres]
          case (k,(l,h))   => Map(
            s"litres${k}UKLower" -> l,
            s"litres${k}UKHigher" -> h
          )
        }
      

      Json.obj(
        "registration" -> Json.obj(
          "organisationType" -> "1", // TODO!
          "applicationDate" -> Date.now.toString, // ???
          "taxStartDate" -> s.liabilityDate.toString, 
          "cin" -> s.utr,
          "tradingName" -> s.orgName,
          "businessContact" -> Json.obj(
            "addressDetails" -> s.address,
            "contactDetails" -> Json.obj(
              "telephone" -> s.contact.phoneNumber,
              "email" -> s.contact.email
            )
          ),
          "primaryPersonContact" -> Json.obj(
            "name" -> s.contact.name,
            "telephone" -> s.contact.phoneNumber,
            "email" -> s.contact.email,
            "positionInCompany" -> s.contact.positionInCompany
          ),
          "details" -> Json.obj(
            "producer" -> isProducer,
            "producerDetails" -> Json.obj(
              "produceMillionLitres"   -> isLarge, 
              "producerClassification" -> {if (isLarge) "1" else "0"},
              "smallProducerExemption" -> !isLarge
            ),
            "importer" ->
              s.activity.contains(ActivityType.Imported),
            "contractPacker" ->
              s.activity.contains(ActivityType.Copackee)
          ),
          "activityQuestions" -> activityMap,
          "estimatedTaxAmount" -> JsNumber(s.taxEstimatePounds),
          "taxObligationStartDate" -> Date.now.toString
        )
      )
    }
  }

  implicit val createSubscriptionResponseFormat: OFormat[CreateSubscriptionResponse] =
    Json.format[CreateSubscriptionResponse]
    
}
