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

package uk.gov.hmrc.softdrinksindustrylevy.models.json

import play.api.libs.json._
import uk.gov.hmrc.softdrinksindustrylevy.models.{longTupleFormatter => _, _}

package object internal {

  // SDIL create and retrieve subscription formatters
  implicit val addressFormat = new Format[Address] {
    lazy val ukAddressFormat = Json.format[UkAddress]
    lazy val foreignAddressFormat = Json.format[ForeignAddress]

    def reads(json: JsValue): JsResult[Address] =
      (json \ "country").asOpt[String].map(_.toLowerCase) match {
        case Some("uk") | None => ukAddressFormat.reads(json)
        case _                 => foreignAddressFormat.reads(json)
      }

    def writes(address: Address): JsValue = address match {
      case uk: UkAddress           => ukAddressFormat.writes(uk)
      case foreign: ForeignAddress => foreignAddressFormat.writes(foreign)
    }
  }

  implicit val litreBandsFormat: Format[LitreBands] = new Format[LitreBands] {
    def reads(json: JsValue): JsResult[LitreBands] =
      JsSuccess(
        (
          (json \ "lower").as[Litres],
          (json \ "upper").as[Litres]
        ))
    def writes(value: LitreBands): JsValue = JsObject(
      List("lower" -> JsNumber(value._1), "upper" -> JsNumber(value._2))
    )
  }

  implicit val businessContactFormat: OFormat[Contact] = Json.format[Contact]
  implicit val siteFormat: OFormat[Site] = Json.format[Site]

  implicit val activityMapFormat: Format[Activity] = new Format[Activity] {
    def reads(json: JsValue): JsResult[Activity] = JsSuccess {
      try {
        InternalActivity(ActivityType.values.flatMap { at =>
          (json \ at.toString).asOpt[LitreBands].map {
            at -> _
          }
        }.toMap, (json \ "isLarge").as[Boolean])
      } catch {
        case _: JsResultException =>
          val smallProducer = (json \ "smallProducer").asOpt[Boolean]
          val largeProducer = (json \ "largeProducer").asOpt[Boolean]
          val contractPacker = (json \ "contractPacker").asOpt[Boolean]
          val importer = (json \ "importer").asOpt[Boolean]
          RetrievedActivity(
            isProducer = smallProducer.contains(true) || largeProducer.contains(true),
            isLarge = largeProducer.contains(true),
            isContractPacker = contractPacker.contains(true),
            isImporter = importer.contains(true)
          )
      }
    }

    def writes(activity: Activity): JsValue = JsObject(
      activity match {
        case InternalActivity(a, lg) =>
          a.map {
            case (t, lb) =>
              t.toString -> litreBandsFormat.writes(lb)
          } ++ Map("isLarge" -> JsBoolean(lg))
        case a: RetrievedActivity =>
          Map(
            "voluntaryRegistration" -> JsBoolean(a.isVoluntaryRegistration),
            "smallProducer"         -> JsBoolean(a.isSmallProducer),
            "largeProducer"         -> JsBoolean(a.isLarge),
            "contractPacker"        -> JsBoolean(a.isContractPacker),
            "importer"              -> JsBoolean(a.isImporter)
          )
      }
    )
  }

  val subReads: Reads[Subscription] = Json.reads[Subscription]
  val subWrites: Writes[Subscription] = new Writes[Subscription] {
    override def writes(o: Subscription): JsValue = Json.obj(
      "utr"             -> o.utr,
      "sdilRef"         -> o.sdilRef,
      "orgName"         -> o.orgName,
      "orgType"         -> o.orgType,
      "address"         -> o.address,
      "activity"        -> o.activity,
      "liabilityDate"   -> o.liabilityDate,
      "productionSites" -> o.productionSites,
      "warehouseSites"  -> o.warehouseSites,
      "contact"         -> o.contact,
      "_id"             -> o.utr
    )
  }

  implicit val subscriptionFormat: OFormat[Subscription] = Json.format[Subscription]

}
