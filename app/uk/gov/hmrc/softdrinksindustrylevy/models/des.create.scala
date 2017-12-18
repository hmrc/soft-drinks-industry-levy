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

import java.time.{LocalDate => Date}

import play.api.libs.json._
import uk.gov.hmrc.softdrinksindustrylevy.models._

//Reads the DES subscription create JSON to create a Subscription and writes it back

package object create {

  implicit val businessContactFormat = new Format[Contact] {
    override def writes(contact: Contact): JsValue = Json.obj(
      "name" -> contact.name,
      "positionInCompany" -> contact.positionInCompany,
      "telephone" -> contact.phoneNumber,
      "email" -> contact.email
    )

    override def reads(json: JsValue): JsResult[Contact] = JsSuccess(Contact(
      (json \ "name").asOpt[String],
      (json \ "positionInCompany").asOpt[String],
      (json \ "telephone").as[String],
      (json \ "email").as[String]
    ))
  }

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
        case (Some("GB") | None, false) => JsSuccess(UkAddress(lines, (json \ "postCode").as[String]))
        case (Some(_), false) => JsError("Country code is not GB, but notUKAddress is false")
        case (None, true) => JsError("notUKAddress is true, but no country is supplied")
        case (Some(c), true) => JsSuccess(ForeignAddress(lines, c))
      }
    }

    def writes(address: Address): JsValue = {

      val jsLines = address.lines.zipWithIndex.map { case (v, i) =>
        s"line${i + 1}" -> JsString(v)
      }

      JsObject(
        {
          address match {
            case UkAddress(_, postCode) => List(
              "notUKAddress" -> JsBoolean(false),
              "postCode" -> JsString(postCode)
            )
            case ForeignAddress(_, country) => List(
              "notUKAddress" -> JsBoolean(true),
              "country" -> JsString(country)
            )
          }
        } ::: jsLines
      )
    }
  }

  implicit val subscriptionFormat: Format[Subscription] = new Format[Subscription] {
    def reads(json: JsValue): JsResult[Subscription] = {

      val (warehouses, production) = json \ "sites" match {
        case JsDefined(JsArray(sites)) => sites.partition(site => (site \ "siteType").as[String] == "1")
        case _ => (Nil, Nil)
      }

      def sites(siteJson: Seq[JsValue]) = {
        siteJson map {
          site => Site(address = (site \ "siteAddress" \ "addressDetails").as[Address], ref = (site \ "newSiteRef").asOpt[String])
        }
      }.toList

      val regJson = json \ "registration"

      def litreReads(activityField: String) = (
        (regJson \ "activityQuestions" \ s"litres${activityField}UKLower").as[Litres],
        (regJson \ "activityQuestions" \ s"litres${activityField}UKHigher").as[Litres]
      )

      def activity = {
        val produced = ActivityType.ProducedOwnBrand -> litreReads("Produced")
        val imported = ActivityType.Imported -> litreReads("Imported")

        InternalActivity(Map(produced, imported))
      }

      JsSuccess(Subscription(
        utr = (regJson \ "cin").as[String],
        orgName = (regJson \ "tradingName").as[String],
        orgType = (regJson \ "organisationType").asOpt[String],
        address = (regJson \ "businessContact" \ "addressDetails").as[Address],
        activity = activity, // TODO this is wrong
        liabilityDate = (regJson \ "taxStartDate").as[Date],
        productionSites = sites(production),
        warehouseSites = sites(warehouses),
        contact = (regJson \ "primaryPersonContact").as[Contact]
      ))

    }


    def writes(s: Subscription): JsValue = {

      def activityMap = {
        import ActivityType._
        s.activity match {
          case a: InternalActivity => Map(
            "Produced" -> a.activity.getOrElse(ProducedOwnBrand, (0L,0L)), // TODO check logic
            "Imported" -> a.activity.getOrElse(Imported, (0L, 0L)),
            "Packaged" -> a.activity.getOrElse(CopackerAll, (0L, 0L)) // TODO check logic
          ).flatMap {
            case (k, (l, h)) => Map(
              s"litres${k}UKLower" -> JsNumber(l),
              s"litres${k}UKHigher" -> JsNumber(h)
            )
          }
          case _ => Map.empty[String, JsValue]
        }
      }

      def siteList(sites: List[Site], isWarehouse: Boolean, offset: Int = 0): List[JsObject] = {
        sites.zipWithIndex map {
          case (site, idx) =>
            Json.obj(
              "action" -> "1",
              "tradingName" -> s.orgName,
              "newSiteRef" -> site.ref.getOrElse[String](s"${idx + offset}"),
              "siteAddress" -> Json.obj(
                "addressDetails" -> site.address,
                "contactDetails" -> Json.obj(
                  "telephone" -> s.contact.phoneNumber,
                  "email" -> s.contact.email
                )
              ),
              "siteType" -> (if (isWarehouse) "1" else "2")
            )
        }
      }

      Json.obj(
        "registration" -> Json.obj(
          "organisationType" -> "1", // TODO!
          "applicationDate" -> Date.now.toString,
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
            "producer" -> s.activity.isProducer,
            "producerDetails" -> Json.obj(
              "produceMillionLitres" -> s.activity.isLarge,
              "producerClassification" -> (if (s.activity.isLarge) "1" else "0"),
              "smallProducerExemption" -> !s.activity.isLarge
            ),
            "importer" -> s.activity.isImporter,
            "contractPacker" -> s.activity.isContractPacker
          ),
          "activityQuestions" -> activityMap, // TODO here...
          "estimatedTaxAmount" -> s.activity.taxEstimation,
          "taxObligationStartDate" -> s.liabilityDate.toString
        ),
        "sites" -> (siteList(s.warehouseSites, true) ++ siteList(s.productionSites, false, s.warehouseSites.size))
      )
    }
  }

  implicit val createSubscriptionResponseFormat: OFormat[CreateSubscriptionResponse] =
    Json.format[CreateSubscriptionResponse]

}
