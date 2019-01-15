/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.softdrinksindustrylevy.controllers

import org.scalatest.prop.PropertyChecks
import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.Json
import sdil.models.des.{FinancialTransaction, FinancialTransactionResponse}

class BalanceControllerSpec extends FlatSpec with Matchers with PropertyChecks  {


  "financial data with 2 payments with contractAccountCategory of 32 and one charge " should " be converted " +
    " using 'convert' to 3 line items" in {

    val stream = getClass.getResourceAsStream("/des-financial-data-32-two-payments.json")
    import FinancialTransaction._
    val obj = Json.parse(stream).as[FinancialTransactionResponse]

    BalanceController.convert(obj)
      .length should be (3)
  }

  "financial data with 1 payment with contractAccountCategory of 32, one with contractAccountCategory of 33 and" +
    " one charge " should " be converted using 'convert' to 2 line items" in {

    val stream = getClass.getResourceAsStream("/des-financial-data-32-one-payment.json")
    import FinancialTransaction._
    val obj = Json.parse(stream).as[FinancialTransactionResponse]

    BalanceController.convert(obj)
      .length should be (2)
  }

  "financial data with 2 payments with contractAccountCategory of 32 and one charge " should " be converted " +
    " using 'convertWithoutAssessment' to 3 line items" in {

    val stream = getClass.getResourceAsStream("/des-financial-data-32-two-payments.json")
    import FinancialTransaction._
    val obj = Json.parse(stream).as[FinancialTransactionResponse]

    BalanceController.convertWithoutAssessment(obj)
      .length should be (3)
  }

  "financial data with 1 payment with contractAccountCategory of 32, one with contractAccountCategory of 33 and" +
    " one charge " should " be converted using 'convertWithoutAssessment' to 2 line items" in {

    val stream = getClass.getResourceAsStream("/des-financial-data-32-one-payment.json")
    import FinancialTransaction._
    val obj = Json.parse(stream).as[FinancialTransactionResponse]

    BalanceController.convertWithoutAssessment(obj)
      .length should be (2)
  }

  "financial data with unknown type, 2 payments with contractAccountCategory of 32 and one charge " should " be converted " +
    " using 'convertWithoutAssessment' to 4 line items" in {

    val stream = getClass.getResourceAsStream("/des-financial-data-32-two-payments-and-unknown.json")
    import FinancialTransaction._
    val obj = Json.parse(stream).as[FinancialTransactionResponse]

    BalanceController.convertWithoutAssessment(obj)
      .length should be (4)
  }

  "financial data with unknown type, 2 payments with contractAccountCategory of 32 and one charge " should " be converted " +
    " using 'convert' to 4 line items" in {

    val stream = getClass.getResourceAsStream("/des-financial-data-32-two-payments-and-unknown.json")
    import FinancialTransaction._
    val obj = Json.parse(stream).as[FinancialTransactionResponse]

    BalanceController.convert(obj)
      .length should be (4)
  }

}
