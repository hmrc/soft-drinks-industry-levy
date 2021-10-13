/*
 * Copyright 2021 HM Revenue & Customs
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

import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json._
import uk.gov.hmrc.softdrinksindustrylevy.util.FakeApplicationSpec

import java.time.LocalDate

class FinancialLineItemSpec
    extends FakeApplicationSpec with MockitoSugar with BeforeAndAfterAll with ScalaCheckPropertyChecks {

  val date: LocalDate = LocalDate.now
  val bigDecimal: BigDecimal = 1000

  "A FinancialLineItem" should {
    "be serialisable" in {

      val returnCharge: FinancialLineItem = ReturnCharge(ReturnPeriod(date), bigDecimal)
      val returnChargeInterest: FinancialLineItem = ReturnChargeInterest(date, bigDecimal)
      val centralAssessment: FinancialLineItem = CentralAssessment(date, bigDecimal)
      val centralAsstInterest: FinancialLineItem = CentralAsstInterest(date, bigDecimal)
      val officerAssessment: FinancialLineItem = OfficerAssessment(date, bigDecimal)
      val officerAsstInterest: FinancialLineItem = OfficerAsstInterest(date, bigDecimal)
      val paymentOnAccount: FinancialLineItem = PaymentOnAccount(date, "blah", bigDecimal, "one", "two")
      val unknown: FinancialLineItem = Unknown(date, "someTitle", bigDecimal)

      Json.toJson(returnCharge).as[FinancialLineItem] mustBe (returnCharge)
      Json.toJson(returnChargeInterest).as[FinancialLineItem] mustBe (returnChargeInterest)
      Json.toJson(centralAssessment).as[FinancialLineItem] mustBe (centralAssessment)
      Json.toJson(centralAsstInterest).as[FinancialLineItem] mustBe (centralAsstInterest)
      Json.toJson(officerAssessment).as[FinancialLineItem] mustBe (officerAssessment)
      Json.toJson(officerAsstInterest).as[FinancialLineItem] mustBe (officerAsstInterest)
      Json.toJson(paymentOnAccount).as[FinancialLineItem] mustBe (paymentOnAccount)
      Json.toJson(unknown).as[FinancialLineItem] mustBe (unknown)
    }
  }
}
