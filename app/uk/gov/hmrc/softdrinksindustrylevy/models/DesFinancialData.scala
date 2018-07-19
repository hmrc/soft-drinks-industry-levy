/*
 * Copyright 2018 HM Revenue & Customs
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

import java.time._

case class FinancialTransaction (
  chargeType: String,
  mainType: Option[String],
  periodKey: Option[String],
  taxPeriodFrom: Option[LocalDate],
  taxPeriodTo: Option[LocalDate],
  businessPartner: Option[String],
  contractAccountCategory: Option[String],
  contractAccount: Option[String],
  contractObjectType: Option[String],
  contractObject: Option[String],
  sapDocumentNumber: Option[String],
  sapDocumentNumberItem: Option[String],
  chargeReference: Option[String],
  mainTransaction: Option[String],
  subTransaction: Option[String],
  originalAmount: BigDecimal,
  clearedAmount: Option[BigDecimal],
  accruedInterest: Option[BigDecimal],
  items: List[SubItem]
)

case class SubItem (
  subItem: String,
  dueDate: LocalDate,
  amount: BigDecimal,
  clearingDate: Option[LocalDate],
  clearingReason: Option[String],
  outgoingPaymentMethod: Option[String],
  paymentLock: Option[String],
  clearingLock: Option[String],
  interestLock: Option[String],
  dunningLock: Option[String],
  returnFlag: Option[Boolean],
  paymentReference: Option[String],
  paymentAmount: Option[BigDecimal],
  paymentMethod: Option[String],
  paymentLot: Option[String],
  paymentLotItem: Option[String],
  clearingSAPDocument: Option[String],
  statisticalDocument: Option[String],
  returnReason: Option[String],
  promiseToPay: Option[String]
)

case class FinancialTransactionResponse (
  idType: String,
  idNumber: String,
  regimeType: String,
  processingDate: LocalDateTime,
  financialTransactions: List[FinancialTransaction]
)

object FinancialTransaction {
  import play.api.libs.json.Json

  implicit val subItemFormatter = Json.format[SubItem]
  implicit val financialTransactionFormatter = Json.format[FinancialTransaction]
  implicit val responseFormatter = Json.format[FinancialTransactionResponse]
}
