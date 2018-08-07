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

package uk.gov.hmrc.softdrinksindustrylevy.controllers

import play.api.libs.json._
import play.api.mvc._
import scala.concurrent._
import uk.gov.hmrc.auth.core.{AuthorisedFunctions, AuthConnector}
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.softdrinksindustrylevy.connectors.DesConnector
import sdil.models._
import sdil.models.des._
import java.time._
import uk.gov.hmrc.http.HeaderCarrier
import cats.implicits._
import cats.syntax.either._
import cats.data.OptionT

class BalanceController(
  val authConnector: AuthConnector,
  desConnector: DesConnector
)(implicit ec: ExecutionContext) extends BaseController with AuthorisedFunctions {

  import BalanceController._

  def balance(sdilRef: String): Action[AnyContent] =
    Action.async { implicit request =>
      desConnector.retrieveFinancialData(sdilRef, None)
        .map{r =>
          val lineItems = convert(r)
          Ok(JsNumber(lineItems.balance))
        }
    }

  def balanceHistoryAll(sdilRef: String): Action[AnyContent] =
    Action.async { implicit request =>

      val r: Future[List[FinancialLineItem]] = for {
        subscription <- desConnector.retrieveSubscriptionDetails("sdil", sdilRef).map{_.get}
        years        =  (subscription.liabilityDate.getYear to LocalDate.now.getYear).toList
        lineItems    <- years.map{y => desConnector.retrieveFinancialData(sdilRef, y.some)}.sequence
      } yield {
        deduplicatePayments(convert(lineItems)).sortBy(_.date.toString)
      }

      r.map{x => Ok(JsArray(x.map{Json.toJson(_)}))}
    }

  def balanceHistory(sdilRef: String, year: Int): Action[AnyContent] =
    Action.async { implicit request =>
      desConnector.retrieveFinancialData(sdilRef, Some(year)).map{ r =>
        val data: List[FinancialLineItem] = convert(r)
        Ok(JsArray(data.map{Json.toJson(_)}))
      }
    }
}

object BalanceController {

  type Payment = (String, LocalDate, BigDecimal)
  def deduplicatePayments(in: List[FinancialLineItem]): List[FinancialLineItem] = {
    val (payments,other) = in.partition { _.isInstanceOf[PaymentOnAccount] }
    other ++ payments.distinct
  }

  def convert(in: List[FinancialTransactionResponse]): List[FinancialLineItem] = 
    deduplicatePayments(in.flatMap{_.financialTransactions.flatMap(convert)})
      .sortBy{_.date.toString}

  def convert(in: FinancialTransactionResponse): List[FinancialLineItem] = {
    deduplicatePayments(in.financialTransactions.flatMap(convert))
      .sortBy{_.date.toString}
      .reverse
  }

  def convert(in: FinancialTransaction): List[FinancialLineItem] = {

    def deep(base: FinancialLineItem): List[FinancialLineItem] =
      base :: {
        in.items collect {
          case i: SubItem if i.paymentReference.isDefined =>
            PaymentOnAccount(
              i.clearingDate.get,
              i.paymentReference.get,
              i.paymentAmount.get,
              i.paymentLot.get,
              i.paymentLotItem.get
            )
        }
      }

    def parseIntOpt(in: String): Option[Int] =
      scala.util.Try(in.toInt).toOption

    def interest(
      f: (LocalDate, BigDecimal) => FinancialLineItem,
      amount: Option[BigDecimal]
    ): List[FinancialLineItem] = amount.filter{_ != 0}.map { x =>
      f(LocalDate.now, -x)
    }.toList

    val amount = -in.items.map{_.amount}.sum
    def dueDate = in.items.head.dueDate

    (
      in.mainTransaction >>= parseIntOpt,
      in.subTransaction >>= parseIntOpt
    ) match {
      case (Some(main), Some(sub)) => (main,sub) match {
        case (4810,1540) => deep(ReturnCharge(ReturnPeriod.fromPeriodKey(in.periodKey.get), -in.originalAmount)) ++ interest(ReturnChargeInterest, in.accruedInterest)
        case (4815,2215) => deep(ReturnChargeInterest(dueDate, amount))
        case (4820,1540) => deep(CentralAssessment(dueDate, amount)) ++ interest(CentralAsstInterest, in.accruedInterest)
        case (4825,2215) => deep(CentralAsstInterest(dueDate, amount))
        case (4830,1540) => deep(OfficerAssessment(dueDate, amount)) ++ interest(OfficerAsstInterest, in.accruedInterest)
        case (4835,2215) => deep(OfficerAsstInterest(dueDate, amount))
        case (60,100)    => PaymentOnAccount(dueDate,
                                             in.items.head.paymentReference.get,
                                             in.items.head.paymentAmount.get,
                                             in.items.head.paymentLot.get,
                                             in.items.head.paymentLotItem.get).pure[List]
        case _           => Unknown(dueDate, in.mainType.getOrElse("Unknown"), amount).pure[List]
      }
      case _             => Unknown(dueDate, in.mainType.getOrElse("Unknown"), amount).pure[List]
    }
  }

  implicit class RichLineItems(lineItems: List[FinancialLineItem]) {
    def balance: BigDecimal = lineItems.map(_.amount).sum
  }

}
