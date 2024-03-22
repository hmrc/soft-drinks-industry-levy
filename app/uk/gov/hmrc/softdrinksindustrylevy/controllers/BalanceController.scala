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

package uk.gov.hmrc.softdrinksindustrylevy.controllers

import java.time._
import cats.implicits._
import play.api.Logger
import play.api.libs.json._
import play.api.mvc._
import sdil.models._
import sdil.models.des._
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.softdrinksindustrylevy.connectors.DesConnector
import com.google.inject.{Inject, Singleton}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent._
import scala.util.Random

@Singleton
class BalanceController @Inject()(
  val authConnector: AuthConnector,
  desConnector: DesConnector,
  val cc: ControllerComponents,
  val configuration: ServicesConfig
)(implicit ec: ExecutionContext)
    extends BackendController(cc) with AuthorisedFunctions {

  import BalanceController._

  def balance(sdilRef: String, withAssessment: Boolean = true): Action[AnyContent] =
    Action.async { implicit request =>
      desConnector
        .retrieveFinancialData(sdilRef, None)
        .map {
          case Some(r) =>
            val financialTransactions = ftWithCorrectContractAccountCategory(r.financialTransactions)
            val getOutstandingBalanceIfPresent =
              if (configuration.getBoolean("balance.useOutstandingAmount") &&
                  financialTransactions.length == 1) {
                financialTransactions.head.outstandingAmount
              } else {
                None
              }

            getOutstandingBalanceIfPresent match {
              case Some(balance)          => Ok(JsNumber(-balance))
              case None if withAssessment => Ok(JsNumber(convert(r).balance))
              case _                      => Ok(JsNumber(convertWithoutAssessment(r).balance))
            }
          case None => Ok(JsNumber(0))
        }
    }

  def balanceHistoryAll(sdilRef: String, withAssessment: Boolean): Action[AnyContent] =
    Action.async { implicit request =>
      val r: Future[List[FinancialLineItem]] = for {
        subscription <- desConnector.retrieveSubscriptionDetails("sdil", sdilRef).map { _.get }
        years = (subscription.liabilityDate.getYear to LocalDate.now.getYear).toList
        responses <- years.map { y =>
                      desConnector.retrieveFinancialData(sdilRef, y.some)
                    }.sequence
      } yield {
        deduplicatePayments(
          if (withAssessment)
            convert(responses.flatten)
          else
            convertWithoutAssessment(responses.flatten)
        ).sortBy(_.date.toString)

      }

      r.map { x =>
        Ok(JsArray(x.map { Json.toJson(_) }))
      }
    }

  def balanceHistory(sdilRef: String, year: Int): Action[AnyContent] =
    Action.async { implicit request =>
      desConnector.retrieveFinancialData(sdilRef, Some(year)).map { r =>
        val data: List[FinancialLineItem] = r.fold(List.empty[FinancialLineItem])(convert)
        Ok(JsArray(data.map { Json.toJson(_) }))
      }
    }
}

object BalanceController {
  val logger = Logger(this.getClass)
  type Payment = (String, LocalDate, BigDecimal)

  def transactionHasCorrectAccountCategory(in: FinancialTransaction): Boolean =
    in.contractAccountCategory == "32".some

  def ftWithCorrectContractAccountCategory(in: List[FinancialTransaction]): List[FinancialTransaction] =
    in.filter(transactionHasCorrectAccountCategory)

  def deduplicatePayments(in: List[FinancialLineItem]): List[FinancialLineItem] = {
    val (payments, other) = in.partition {
      _.isInstanceOf[PaymentOnAccount]
    }
    other ++ payments.distinct
  }

  def parseIntOpt(in: String): Option[Int] =
    scala.util.Try(in.toInt).toOption

  def interest(
    f: (LocalDate, BigDecimal) => FinancialLineItem,
    amount: Option[BigDecimal]
  ): List[FinancialLineItem] =
    amount
      .filter {
        _ != 0
      }
      .map { x =>
        f(LocalDate.now, -x)
      }
      .toList

  def deep(base: FinancialLineItem, in: FinancialTransaction): List[FinancialLineItem] =
    base :: {
      in.items collect {
        case i: SubItem
            if i.paymentReference.isDefined && i.outgoingPaymentMethod.isEmpty && transactionHasCorrectAccountCategory(in) =>
          PaymentOnAccount(
            i.clearingDate.get,
            i.paymentReference.get,
            i.paymentAmount.get,
            i.paymentLot.get,
            i.paymentLotItem.get
          )
      }
    }

  def amount(in: FinancialTransaction): BigDecimal =
    -in.items.map {
      _.amount
    }.sum

  def paymentAmount(items: List[SubItem]): BigDecimal = -items.map { _.amount }.sum

  def dueDate(in: FinancialTransaction): LocalDate = in.items.head.dueDate

  def convert(in: List[FinancialTransactionResponse]): List[FinancialLineItem] =
    deduplicatePayments(in.flatMap {
      _.financialTransactions.flatMap(convert)
    }).sortBy {
      _.date.toString
    }

  def convert(in: FinancialTransactionResponse): List[FinancialLineItem] =
    deduplicatePayments(in.financialTransactions.flatMap(convert)).sortBy {
      _.date.toString
    }.reverse

  private def randomNumbers(stringLength: Int, id: String): String = {
    val n = Seq.fill(stringLength)(Random.nextInt(9)).mkString("")
    logger.warn(s"$id not retrieved from get financial data api, replaced with $n")
    n
  }

  private def logBigDec(default: BigDecimal, id: String): BigDecimal = {
    logger.warn(s"$id not retrieved from get financial data api, replaced with $default")
    default
  }

  def convert(in: FinancialTransaction): List[FinancialLineItem] = {
    val mainTransaction = in.mainTransaction >>= parseIntOpt
    val subTransaction = in.subTransaction >>= parseIntOpt
    (mainTransaction, subTransaction) match {
      case (Some(main), Some(sub)) if sub == 1540                            => convertReturnOrAssessmentFinancialTransaction(in, main)
      case (Some(main), Some(sub)) if sub == 2215                            => convertInterestFinancialTransaction(in, main)
      case (Some(60), Some(100)) if transactionHasCorrectAccountCategory(in) => convertPaymentFinancialTransaction(in)
      case _                                                                 => handleUnrecognisedFinancialTransaction(in, mainTransaction, subTransaction)
    }
  }

  private def convertReturnOrAssessmentFinancialTransaction(
    in: FinancialTransaction,
    mainTransaction: Int): List[FinancialLineItem] =
    mainTransaction match {
      case 4810 =>
        deep(ReturnCharge(ReturnPeriod.fromPeriodKey(in.periodKey.get), -in.originalAmount), in) ++ interest(
          ReturnChargeInterest.apply,
          in.accruedInterest)
      case 4820 =>
        deep(CentralAssessment(dueDate(in), amount(in)), in) ++ interest(CentralAsstInterest.apply, in.accruedInterest)
      case 4830 =>
        deep(OfficerAssessment(dueDate(in), amount(in)), in) ++ interest(OfficerAsstInterest.apply, in.accruedInterest)
      case _ => handleUnrecognisedFinancialTransaction(in, Some(mainTransaction), Some(1540))
    }
  private def convertInterestFinancialTransaction(
    in: FinancialTransaction,
    mainTransaction: Int): List[FinancialLineItem] =
    mainTransaction match {
      case 4815 => deep(ReturnChargeInterest(dueDate(in), amount(in)), in)
      case 4825 => deep(CentralAsstInterest(dueDate(in), amount(in)), in)
      case 4835 => deep(OfficerAsstInterest(dueDate(in), amount(in)), in)
      case _    => handleUnrecognisedFinancialTransaction(in, Some(mainTransaction), Some(2215))
    }

  private def convertPaymentFinancialTransaction(in: FinancialTransaction): List[FinancialLineItem] = {
    val incomingPaymentItems = in.items.collect { case item if item.outgoingPaymentMethod.isEmpty => item }
    if (incomingPaymentItems.nonEmpty) {
      PaymentOnAccount(
        dueDate(in),
        incomingPaymentItems.head.paymentReference.getOrElse(randomNumbers(10, "payment reference")),
        paymentAmount(incomingPaymentItems),
        incomingPaymentItems.head.paymentLot.getOrElse(randomNumbers(10, "payment lot")),
        incomingPaymentItems.head.paymentLotItem.getOrElse(randomNumbers(10, "payment lot item"))
      ).pure[List]
    } else {
      List.empty
    }
  }

  private def handleUnrecognisedFinancialTransaction(
    in: FinancialTransaction,
    mainTransaction: Option[Int] = None,
    subTransaction: Option[Int] = None): List[FinancialLineItem] =
    (mainTransaction, subTransaction) match {
      case (a, b) if transactionHasCorrectAccountCategory(in) =>
        logger.warn(
          s"Unknown ${in.mainType} of ${amount(in)} at ${dueDate(in)}, mainTransaction: $a, subTransaction: $b, contractAccountCategory 32")
        Unknown(dueDate(in), in.mainType.getOrElse("Unknown"), amount(in)).pure[List]
      case _ if transactionHasCorrectAccountCategory(in) =>
        logger.warn(s"Unknown ${in.mainType} of ${amount(in)} at ${dueDate(in)}, contractAccountCategory 32")
        Unknown(dueDate(in), in.mainType.getOrElse("Unknown"), amount(in)).pure[List]
      case _ =>
        logger.warn(s"Unknown ${in.mainType} of ${amount(in)} at ${dueDate(in)}")
        List.empty
    }

  def convertWithoutAssessment(in: List[FinancialTransactionResponse]): List[FinancialLineItem] =
    deduplicatePayments(in.flatMap { _.financialTransactions.flatMap(convertWithoutAssessment) })
      .sortBy { _.date.toString }

  def convertWithoutAssessment(in: FinancialTransactionResponse): List[FinancialLineItem] =
    deduplicatePayments(in.financialTransactions.flatMap(convertWithoutAssessment)).sortBy { _.date.toString }.reverse

  def convertWithoutAssessment(in: FinancialTransaction): List[FinancialLineItem] =
    convert(in).filterNot {
      case _: CentralAssessment   => true
      case _: CentralAsstInterest => true
      case _: OfficerAssessment   => true
      case _: OfficerAsstInterest => true
      case _                      => false
    }

  implicit class RichLineItems(lineItems: List[FinancialLineItem]) {
    def balance: BigDecimal = lineItems.map(_.amount).sum
  }
}
