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

package sdil.models.des

import play.api.libs.json.OFormat

import java.time._

case class FinancialTransaction(
  chargeType: String,
  mainType: Option[String] = None,
  periodKey: Option[String] = None,
  taxPeriodFrom: Option[LocalDate] = None,
  taxPeriodTo: Option[LocalDate] = None,
  businessPartner: Option[String] = None,
  contractAccountCategory: Option[String] = None,
  contractAccount: Option[String] = None,
  contractObjectType: Option[String] = None,
  contractObject: Option[String] = None,
  sapDocumentNumber: Option[String] = None,
  sapDocumentNumberItem: Option[String] = None,
  chargeReference: Option[String] = None,
  mainTransaction: Option[String] = None,
  subTransaction: Option[String] = None,
  originalAmount: BigDecimal,
  clearedAmount: Option[BigDecimal] = None,
  accruedInterest: Option[BigDecimal] = None,
  items: List[SubItem],
  outstandingAmount: Option[BigDecimal] = None
)

case class SubItem(
  subItem: String,
  dueDate: LocalDate,
  amount: BigDecimal,
  clearingDate: Option[LocalDate] = None,
  clearingReason: Option[String] = None,
  outgoingPaymentMethod: Option[String] = None,
  paymentLock: Option[String] = None,
  clearingLock: Option[String] = None,
  interestLock: Option[String] = None,
  dunningLock: Option[String] = None,
  returnFlag: Option[Boolean] = None,
  paymentReference: Option[String] = None,
  paymentAmount: Option[BigDecimal] = None,
  paymentMethod: Option[String] = None,
  paymentLot: Option[String] = None,
  paymentLotItem: Option[String] = None,
  clearingSAPDocument: Option[String] = None,
  statisticalDocument: Option[String] = None,
  returnReason: Option[String] = None,
  promiseToPay: Option[String] = None
)

case class FinancialTransactionResponse(
  idType: String,
  idNumber: String,
  regimeType: String,
  processingDate: LocalDateTime,
  financialTransactions: List[FinancialTransaction]
)

object FinancialTransaction {
  import play.api.libs.json.Json

  implicit val subItemFormatter: OFormat[SubItem] = Json.format[SubItem]
  implicit val financialTransactionFormatter: OFormat[FinancialTransaction] = Json.format[FinancialTransaction]
  implicit val responseFormatter: OFormat[FinancialTransactionResponse] = Json.format[FinancialTransactionResponse]
}
