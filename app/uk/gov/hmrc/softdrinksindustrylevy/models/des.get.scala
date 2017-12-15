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

import java.time.LocalDate

import play.api.libs.json._
import play.api.libs.functional.syntax._
import uk.gov.hmrc.softdrinksindustrylevy.models._


// Reads the DES retrieve subscription JSON to create a Subscription.

package object get {

  implicit val contactFormat: Format[Contact] = new Format[Contact] {

    override def reads(json: JsValue): JsSuccess[Contact] = {
      JsSuccess(Contact(
        name = Some((json \ "subscriptionDetails" \ "primaryContactName").as[String]),
        positionInCompany = Some((json \ "subscriptionDetails" \ "primaryPositionInCompany").as[String]),
        phoneNumber = (json \ "subscriptionDetails" \"primaryTelephone").as[String],
        email = (json \ "subscriptionDetails" \ "primaryEmail").as[String]
      ))
    }

    override def writes(o: Contact): JsObject = {
      Json.obj(
        "name" -> o.name,
        "positionInCompany" -> o.positionInCompany,
        "telephone" -> o.phoneNumber,
        "email" -> o.email
      )
    }

  }

  implicit val addressFormat: Format[Address] = new Format[Address] {

    override def reads(json: JsValue): JsResult[Address] = {

      val lines = List(
        Some((json \ "line1").as[String]),
        Some((json \ "line2").as[String]),
        (json \ "line3").asOpt[String],
        (json \ "line4").asOpt[String]
      ).flatten

      val country = (json \ "country").asOpt[String].map(_.toUpperCase)
      val post = (json \ "postCode").asOpt[String]
      (country, post) match {
        case (Some("GB") | None, Some(p)) => JsSuccess(UkAddress(lines, p))
        case (Some(c), _) => JsSuccess(ForeignAddress(lines, c))
        case (None, None) => JsError("Neither country code nor postcode supplied")
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

  implicit val siteFormat: Format[Site] = (
    (JsPath \ "siteAddress").format[Address] and
      (JsPath \ "siteReference").format[String]
    )(Site.apply, unlift(Site.unapply))


  implicit val subscriptionFormat: Format[Subscription] = new Format[Subscription] {

    override def writes(o: Subscription): JsValue = {

      def writeSites(siteType: Int): List[JsObject] = {
        val siteTradingName = o.orgName
        val s = siteType match {
          case 1 => o.productionSites
          case 2 => o.warehouseSites
        }
        s.map {
          x => JsObject(Map(
              "tradingName" -> JsString(siteTradingName),
              "siteAddress" -> addressFormat.writes(x.address)
            ))
        }
      }

      Json.obj(
        "utr" -> o.utr,
        "subscriptionDetails" -> Json.obj(
          "sdilRegistrationNumber" -> "unknown",
          "taxObligationStartDate" -> o.liabilityDate.toString,
          "taxObligationEndDate" -> o.liabilityDate.plusYears(1).toString,
          "tradingName" -> o.orgName,
          "voluntaryRegistration" -> o.activity.isVoluntaryRegistration,
          "smallProducer" -> o.activity.isSmallProducer,
          "largeProducer" -> o.activity.isLarge,
          "contractPacker" -> o.activity.isContractPacker,
          "importer" -> o.activity.isImporter,
          "primaryContactName" -> o.contact.name,
          "primaryPositionInCompany" -> o.contact.positionInCompany,
          "primaryTelephone" -> o.contact.phoneNumber,
          "primaryEmail" -> o.contact.email
        ),
        "businessAddress" -> addressFormat.writes(o.address),
        "businessContact" -> Json.obj(
          "telephone" -> o.contact.phoneNumber,
          "email" -> o.contact.email
        ),
        "productionSites" -> writeSites(1),
        "warehouseSites" -> writeSites(2)

      )
    }

    override def reads(json: JsValue): JsResult[Subscription] = {
      def activityType = {
        val smallProducer = (json \ "subscriptionDetails" \ "smallProducer").as[Boolean]
        val largeProducer = (json \ "subscriptionDetails" \ "largeProducer").as[Boolean]
        val contractPacker = (json \ "subscriptionDetails" \ "contractPacker").as[Boolean]
        val importer = (json \ "subscriptionDetails" \ "importer").as[Boolean]
        RetrievedActivity(
          isProducer = smallProducer || largeProducer,
          isLarge = largeProducer,
          isContractPacker = contractPacker,
          isImporter = importer
        )
      }
      def getSites(siteType: String): List[Site] =
        json \ "sites" match {
          case JsDefined(JsArray(arr)) => arr.toList.collect {
            case obj: JsObject if {obj \ "siteType"}.as[String] == siteType => obj.as[Site]
          }
          case _ => List.empty[Site]
        }

      JsSuccess(Subscription(
        utr = (json \ "utr").as[String],
        orgName = (json \ "subscriptionDetails" \ "tradingName").as[String],
        address = (json \ "businessAddress").as[Address],
        activity = activityType,
        liabilityDate = (json \ "subscriptionDetails" \ "taxObligationStartDate").as[LocalDate],
        productionSites = getSites("1"),
        warehouseSites = getSites("2"),
        contact = json.as[Contact]
      ))
    }

  }
    
}
