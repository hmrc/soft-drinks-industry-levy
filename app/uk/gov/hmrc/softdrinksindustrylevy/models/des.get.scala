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

import java.time.LocalDate

import play.api.libs.json._
import play.api.libs.functional.syntax._
import uk.gov.hmrc.softdrinksindustrylevy.models._

// Reads the DES retrieve subscription JSON to create a Subscription.

package object get {

  implicit val contactFormat: Format[Contact] = new Format[Contact] {

    override def reads(json: JsValue): JsSuccess[Contact] =
      JsSuccess(
        Contact(
          name = (json \ "subscriptionDetails" \ "primaryContactName").asOpt[String],
          positionInCompany = (json \ "subscriptionDetails" \ "primaryPositionInCompany").asOpt[String],
          phoneNumber = (json \ "subscriptionDetails" \ "primaryTelephone").as[String],
          email = (json \ "subscriptionDetails" \ "primaryEmail").as[String]
        ))

    override def writes(o: Contact): JsObject =
      Json.obj(
        "name"              -> o.name,
        "positionInCompany" -> o.positionInCompany,
        "telephone"         -> o.phoneNumber,
        "email"             -> o.email
      )

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
        case (Some(c), _)                 => JsSuccess(ForeignAddress(lines, c))
        case (None, None)                 => JsError("Neither country code nor postcode supplied")
      }

    }

    def writes(address: Address): JsValue = {

      val jsLines = address.lines.zipWithIndex.map {
        case (v, i) =>
          s"line${i + 1}" -> JsString(v)
      }

      JsObject(
        {
          address match {
            case UkAddress(_, postCode) =>
              List(
                "notUKAddress" -> JsBoolean(false),
                "postCode"     -> JsString(postCode)
              )
            case ForeignAddress(_, country) =>
              List(
                "notUKAddress" -> JsBoolean(true),
                "country"      -> JsString(country)
              )
          }
        } ::: jsLines
      )
    }

  }

  implicit val siteFormat: Format[Site] = (
    (JsPath \ "siteAddress").format[Address] and
      (JsPath \ "siteReference").formatNullable[String] and
      (JsPath \ "tradingName").formatNullable[String] and
      (__ \ "closureDate").formatNullable[LocalDate]
  )(Site.apply, unlift(Site.unapply))

  implicit val subscriptionFormat: Format[Subscription] = new Format[Subscription] {

    override def writes(o: Subscription): JsValue = {

      def siteList(sites: List[Site], isWarehouse: Boolean): List[JsObject] =
        sites map { site =>
          Json.obj(
            "tradingName"   -> site.tradingName,
            "siteReference" -> site.ref,
            "siteAddress"   -> site.address,
            "siteContact" -> Json.obj(
              "telephone" -> o.contact.phoneNumber,
              "email"     -> o.contact.email
            ),
            "siteType" -> (if (isWarehouse) "1" else "2")
          )
        }

      Json.obj(
        "utr" -> o.utr,
        "subscriptionDetails" -> Json.obj(
          "sdilRegistrationNumber"   -> o.sdilRef,
          "taxObligationStartDate"   -> o.liabilityDate.toString,
          "taxObligationEndDate"     -> o.liabilityDate.plusYears(1).toString,
          "tradingName"              -> o.orgName,
          "voluntaryRegistration"    -> o.activity.isVoluntaryRegistration,
          "smallProducer"            -> o.activity.isSmallProducer,
          "largeProducer"            -> o.activity.isLarge,
          "contractPacker"           -> o.activity.isContractPacker,
          "importer"                 -> o.activity.isImporter,
          "primaryContactName"       -> o.contact.name,
          "primaryPositionInCompany" -> o.contact.positionInCompany,
          "primaryTelephone"         -> o.contact.phoneNumber,
          "primaryEmail"             -> o.contact.email
        ),
        "businessAddress" -> addressFormat.writes(o.address),
        "businessContact" -> Json.obj(
          "telephone" -> o.contact.phoneNumber,
          "email"     -> o.contact.email
        ),
        "sites" -> (siteList(o.warehouseSites, true) ++ siteList(o.productionSites, false))
      )
    }

    override def reads(json: JsValue): JsResult[Subscription] = {
      def activityType = {
        val smallProducer = (json \ "subscriptionDetails" \ "smallProducer").asOpt[Boolean]
        val largeProducer = (json \ "subscriptionDetails" \ "largeProducer").asOpt[Boolean]
        val contractPacker = (json \ "subscriptionDetails" \ "contractPacker").asOpt[Boolean]
        val importer = (json \ "subscriptionDetails" \ "importer").asOpt[Boolean]
        RetrievedActivity(
          isProducer = smallProducer.contains(true) || largeProducer.contains(true),
          isLarge = largeProducer.contains(true),
          isContractPacker = contractPacker.contains(true),
          isImporter = importer.contains(true)
        )
      }

      def getSites(siteType: String): List[Site] =
        json \ "sites" match {
          case JsDefined(JsArray(arr)) =>
            arr.toList.collect {
              case obj: JsObject if {
                    obj \ "siteType"
                  }.as[String] == siteType =>
                obj.as[Site]
            }
          case _ => List.empty[Site]
        }

      JsSuccess(
        Subscription(
          utr = (json \ "utr").as[String],
          sdilRef = (json \ "subscriptionDetails" \ "sdilRegistrationNumber").asOpt[String],
          orgName = (json \ "subscriptionDetails" \ "tradingName").as[String],
          orgType = None,
          address = (json \ "businessAddress").as[Address],
          activity = activityType,
          liabilityDate = (json \ "subscriptionDetails" \ "taxObligationStartDate").as[LocalDate],
          productionSites = getSites("2"),
          warehouseSites = getSites("1"),
          contact = json.as[Contact],
          endDate = (json \ "subscriptionDetails" \ "taxObligationEndDate").asOpt[LocalDate],
          deregDate = (json \ "subscriptionDetails" \ "deregistrationDate").asOpt[LocalDate]
        ))
    }

  }

}
