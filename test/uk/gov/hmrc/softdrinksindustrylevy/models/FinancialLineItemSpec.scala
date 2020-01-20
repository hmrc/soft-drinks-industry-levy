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

package sdil.models

import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.prop.PropertyChecks
import play.api.libs.json._
import java.time.LocalDate

class FinancialLineItemSpec extends FlatSpec with Matchers with PropertyChecks {

  val date: LocalDate = LocalDate.now
  val bigDecimal: BigDecimal = 1000

  "A FinancialLineItem" should "be serialisable" in {

    val returnCharge = new ReturnCharge(ReturnPeriod(date), bigDecimal)
    val returnChargeInterest = new ReturnChargeInterest(date, bigDecimal)
    val centralAssessment = new CentralAssessment(date, bigDecimal)
    val centralAsstInterest = new CentralAsstInterest(date, bigDecimal)
    val officerAssessment = new OfficerAssessment(date, bigDecimal)
    val officerAsstInterest = new OfficerAsstInterest(date, bigDecimal)
    val paymentOnAccount = new PaymentOnAccount(date, "blah", bigDecimal, "one", "two")
    val unknown = new Unknown(date, "someTitle", bigDecimal)

    Json.toJson(returnCharge).as[FinancialLineItem] should be(returnCharge)
    Json.toJson(returnChargeInterest).as[FinancialLineItem] should be(returnChargeInterest)
    Json.toJson(centralAssessment).as[FinancialLineItem] should be(centralAssessment)
    Json.toJson(centralAsstInterest).as[FinancialLineItem] should be(centralAsstInterest)
    Json.toJson(officerAssessment).as[FinancialLineItem] should be(officerAssessment)
    Json.toJson(officerAsstInterest).as[FinancialLineItem] should be(officerAsstInterest)
    Json.toJson(paymentOnAccount).as[FinancialLineItem] should be(paymentOnAccount)
    Json.toJson(unknown).as[FinancialLineItem] should be(unknown)
  }
}
