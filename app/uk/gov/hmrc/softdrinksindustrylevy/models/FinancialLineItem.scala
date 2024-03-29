/*
 * Copyright 2023 HM Revenue & Customs
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

package sdil.models

import java.time.{LocalDate => Date}
import play.api.libs.json._

sealed trait FinancialLineItem {
  def date: Date
  def amount: BigDecimal
}

case class ReturnCharge(period: ReturnPeriod, amount: BigDecimal) extends FinancialLineItem {

  val formatter = java.time.format.DateTimeFormatter.ofPattern("MMMM")
  def date = period.deadline
}

case class ReturnChargeInterest(date: Date, amount: BigDecimal) extends FinancialLineItem {}

case class CentralAssessment(date: Date, amount: BigDecimal) extends FinancialLineItem {}

case class CentralAsstInterest(date: Date, amount: BigDecimal) extends FinancialLineItem {}

case class OfficerAssessment(date: Date, amount: BigDecimal) extends FinancialLineItem {}

case class OfficerAsstInterest(date: Date, amount: BigDecimal) extends FinancialLineItem {}

case class PaymentOnAccount(date: Date, reference: String, amount: BigDecimal, lot: String, lotItem: String)
    extends FinancialLineItem {

  override def equals(any: Any): Boolean = any match {
    case that: PaymentOnAccount =>
      that.lot == this.lot && that.lotItem == this.lotItem
    case _ => false
  }

  override def hashCode: Int = (lot, lotItem).hashCode
}

case class Unknown(date: Date, title: String, amount: BigDecimal) extends FinancialLineItem {}

object FinancialLineItem {

  implicit val formatter: Format[FinancialLineItem] =
    new Format[FinancialLineItem] {
      def reads(json: JsValue): JsResult[FinancialLineItem] =
        (json \ "type").as[String] match {
          case "ReturnCharge"         => Json.format[ReturnCharge].reads(json)
          case "ReturnChargeInterest" => Json.format[ReturnChargeInterest].reads(json)
          case "CentralAssessment"    => Json.format[CentralAssessment].reads(json)
          case "CentralAsstInterest"  => Json.format[CentralAsstInterest].reads(json)
          case "OfficerAssessment"    => Json.format[OfficerAssessment].reads(json)
          case "OfficerAsstInterest"  => Json.format[OfficerAsstInterest].reads(json)
          case "PaymentOnAccount"     => Json.format[PaymentOnAccount].reads(json)
          case "Unknown"              => Json.format[Unknown].reads(json)
        }

      def writes(o: FinancialLineItem): JsValue =
        o match {
          case i: ReturnCharge =>
            Json
              .format[ReturnCharge]
              .writes(i)
              .as[JsObject] ++ Json.obj("type" -> JsString("ReturnCharge"))
          case i: ReturnChargeInterest =>
            Json
              .format[ReturnChargeInterest]
              .writes(i)
              .as[JsObject] ++ Json.obj("type" -> JsString("ReturnChargeInterest"))
          case i: CentralAssessment =>
            (Json
              .format[CentralAssessment]
              .writes(i)
              .as[JsObject]) ++ (Json.obj("type" -> JsString("CentralAssessment")))
          case i: CentralAsstInterest =>
            Json
              .format[CentralAsstInterest]
              .writes(i)
              .as[JsObject] ++ (Json.obj("type" -> JsString("CentralAsstInterest")))
          case i: OfficerAssessment =>
            Json
              .format[OfficerAssessment]
              .writes(i)
              .as[JsObject] ++ (Json.obj("type" -> JsString("OfficerAssessment")))
          case i: OfficerAsstInterest =>
            Json
              .format[OfficerAsstInterest]
              .writes(i)
              .as[JsObject] ++ (Json.obj("type" -> JsString("OfficerAsstInterest")))
          case i: PaymentOnAccount =>
            Json
              .format[PaymentOnAccount]
              .writes(i)
              .as[JsObject] ++ (Json.obj("type" -> JsString("PaymentOnAccount")))
          case i: Unknown =>
            Json.format[Unknown].writes(i).as[JsObject] ++ (Json.obj("type" -> JsString("Unknown")))
        }
    }
}
