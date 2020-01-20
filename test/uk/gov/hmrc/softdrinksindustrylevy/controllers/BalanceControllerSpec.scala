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

package uk.gov.hmrc.softdrinksindustrylevy.controllers

import java.time.{LocalDate, LocalDateTime}

import com.softwaremill.macwire.wire
import org.mockito.ArgumentMatchers.{any, eq => matching}
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import sdil.models.{CentralAssessment, CentralAsstInterest, OfficerAssessment, OfficerAsstInterest, PaymentOnAccount, ReturnChargeInterest, Unknown}
import sdil.models.des.{FinancialTransaction, FinancialTransactionResponse, SubItem}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.retrieve.{Credentials, EmptyRetrieval}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.softdrinksindustrylevy.config.SdilComponents
import uk.gov.hmrc.softdrinksindustrylevy.connectors.DesConnector
import uk.gov.hmrc.softdrinksindustrylevy.util.FakeApplicationSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BalanceControllerSpec extends FakeApplicationSpec with MockitoSugar with BeforeAndAfterEach {

  val mockDesConnector: DesConnector = mock[DesConnector]
  val mockAuthConnector: AuthConnector = mock[AuthConnector]

  lazy val cc = new SdilComponents(context).cc
  val testBalanceController = wire[BalanceController]

  implicit lazy val hc: HeaderCarrier = new HeaderCarrier

  override def beforeEach() {
    reset(mockDesConnector)
  }

  when(mockAuthConnector.authorise[Credentials](any(), any())(any(), any()))
    .thenReturn(Future.successful(Credentials("cred-id", "GovernmentGateway")))

  when(mockAuthConnector.authorise[Unit](any(), matching(EmptyRetrieval))(any(), any()))
    .thenReturn(Future.successful(()))

  "financial data with 2 payments with contractAccountCategory of 32 and one charge should be converted using 'convert' to 3 line items" in {

    val stream = getClass.getResourceAsStream("/des-financial-data-32-two-payments.json")
    import FinancialTransaction._
    val obj = Json.parse(stream).as[FinancialTransactionResponse]

    BalanceController.convert(obj).length must be(3)
  }

  "financial data with 1 payment with contractAccountCategory of 32, one with contractAccountCategory of 33 and" +
    " one charge should be converted using 'convert' to 2 line items" in {

    val stream = getClass.getResourceAsStream("/des-financial-data-32-one-payment.json")
    import FinancialTransaction._
    val obj = Json.parse(stream).as[FinancialTransactionResponse]

    BalanceController.convert(obj).length must be(2)
  }

  "financial data with 2 payments with contractAccountCategory of 32 and one charge should be converted using 'convertWithoutAssessment' to 3 line items" in {

    val stream = getClass.getResourceAsStream("/des-financial-data-32-two-payments.json")
    import FinancialTransaction._
    val obj = Json.parse(stream).as[FinancialTransactionResponse]

    BalanceController.convertWithoutAssessment(obj).length must be(3)
  }

  "financial data with 1 payment with contractAccountCategory of 32, one with contractAccountCategory of 33 and" +
    " one charge should be converted using 'convertWithoutAssessment' to 2 line items" in {

    val stream = getClass.getResourceAsStream("/des-financial-data-32-one-payment.json")
    import FinancialTransaction._
    val obj = Json.parse(stream).as[FinancialTransactionResponse]

    BalanceController.convertWithoutAssessment(obj).length must be(2)
  }

  "financial data with unknown type, 2 payments with contractAccountCategory of 32 and one charge should be converted using 'convertWithoutAssessment' to 4 line items" in {

    val stream = getClass.getResourceAsStream("/des-financial-data-32-two-payments-and-unknown.json")
    import FinancialTransaction._
    val obj = Json.parse(stream).as[FinancialTransactionResponse]

    BalanceController.convertWithoutAssessment(obj).length must be(4)
  }

  "financial data with unknown type, 2 payments with contractAccountCategory of 32 and one charge should be converted using 'convert' to 4 line items" in {

    val stream = getClass.getResourceAsStream("/des-financial-data-32-two-payments-and-unknown.json")
    import FinancialTransaction._
    val obj = Json.parse(stream).as[FinancialTransactionResponse]

    BalanceController.convert(obj).length must be(4)
  }

  "return Status: OK Body: None when checking for balance which has no financial transaction data" in {
    when(mockDesConnector.retrieveFinancialData(any(), any())(any()))
      .thenReturn(Future successful None)
    val response = testBalanceController.balance("0000222200", false)(FakeRequest())

    status(response) mustBe 200
  }

  "return Status: OK Body: None when checking for balance history but no data is found" in {
    when(mockDesConnector.retrieveFinancialData(any(), any())(any()))
      .thenReturn(Future successful None)
    val response = testBalanceController.balanceHistory("0000222200", 2018)(FakeRequest())

    status(response) mustBe 200
  }

  "return Status: OK Body: None when checking for balance history all but no data is found" in {
    when(mockDesConnector.retrieveFinancialData(any(), any())(any()))
      .thenReturn(Future successful None)
    when(mockDesConnector.retrieveSubscriptionDetails(any(), any())(any()))
      .thenReturn(Future successful Some(sub))
    val response = testBalanceController.balanceHistoryAll("0000222200", false)(FakeRequest())

    status(response) mustBe 200
  }

  "convert methods" should {
    val testDate = LocalDate.of(2019, 4, 9)
    val testBigDecimal = BigDecimal(123)
    val emptySubItem = SubItem(
      "",
      testDate,
      testBigDecimal,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None)

    val emptyFT = FinancialTransaction(
      "",
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      testBigDecimal,
      None,
      None,
      List(emptySubItem))
    val unknownFT = FinancialTransaction(
      "",
      None,
      None,
      None,
      None,
      None,
      Some("32"),
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      testBigDecimal,
      None,
      None,
      List(emptySubItem))
    val ReturnChargeInterestFT = FinancialTransaction(
      "",
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      Some("4815"),
      Some("2215"),
      testBigDecimal,
      None,
      None,
      List(emptySubItem))
    val CentralAssessmentFT = FinancialTransaction(
      "",
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      Some("4820"),
      Some("1540"),
      testBigDecimal,
      None,
      None,
      List(emptySubItem))
    val CentralAsstInterestFT = FinancialTransaction(
      "",
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      Some("4825"),
      Some("2215"),
      testBigDecimal,
      None,
      None,
      List(emptySubItem))
    val OfficerAssessmentFT = FinancialTransaction(
      "",
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      Some("4830"),
      Some("1540"),
      testBigDecimal,
      None,
      None,
      List(emptySubItem))
    val OfficerAsstInterestFT = FinancialTransaction(
      "",
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      Some("4835"),
      Some("2215"),
      testBigDecimal,
      None,
      None,
      List(emptySubItem))

    "convert(in: FinancialTransaction)" should {

      "empty FinancialTransaction returns Nil" in {
        BalanceController.convert(emptyFT) mustBe Nil
      }

      "FinancialTransaction contractAccountCategory 32" in {
        val result = BalanceController.convert(unknownFT)
        val expectedResult = List(Unknown(testDate, unknownFT.mainType.getOrElse("Unknown"), -testBigDecimal))
        result mustBe expectedResult
      }

      "FinancialTransaction case (4815,2215)" in {
        val result = BalanceController.convert(ReturnChargeInterestFT)
        val expectedResult = List(ReturnChargeInterest(testDate, -testBigDecimal))
        result mustBe expectedResult
      }

      "FinancialTransaction case (4820,1540)" in {
        val result = BalanceController.convert(CentralAssessmentFT)
        val expectedResult = List(CentralAssessment(testDate, -testBigDecimal))
        result mustBe expectedResult
      }

      "FinancialTransaction case (4825,2215)" in {
        val result = BalanceController.convert(CentralAsstInterestFT)
        val expectedResult = List(CentralAsstInterest(testDate, -testBigDecimal))
        result mustBe expectedResult
      }

      "FinancialTransaction case (4830,1540)" in {
        val result = BalanceController.convert(OfficerAssessmentFT)
        val expectedResult = List(OfficerAssessment(testDate, -testBigDecimal))
        result mustBe expectedResult
      }

      "FinancialTransaction case (4835,2215)" in {
        val result = BalanceController.convert(OfficerAsstInterestFT)
        val expectedResult = List(OfficerAsstInterest(testDate, -testBigDecimal))
        result mustBe expectedResult
      }

      "FinancialTransaction case (60,100)" in {
        val testFinancialTransaction = FinancialTransaction(
          "",
          None,
          None,
          None,
          None,
          None,
          Some("32"),
          None,
          None,
          None,
          None,
          None,
          None,
          Some("60"),
          Some("100"),
          testBigDecimal,
          None,
          None,
          List(emptySubItem))

        val result = BalanceController.convert(testFinancialTransaction)
        result.length mustBe 1

        result.head match {
          case PaymentOnAccount(date, reference, amount, lot, lotItem) => {
            date mustBe testDate
            reference.length mustBe 10
            amount mustBe 0
            lot.length mustBe 10
            lotItem.length mustBe 10
          }
          case _ => fail
        }
      }
    }

    "convertWithoutAssessment(in: FinancialTransaction)" should {

      "empty FinancialTransaction" in {
        BalanceController.convertWithoutAssessment(emptyFT) mustBe Nil
      }

      "FinancialTransaction contractAccountCategory 32" in {
        val result = BalanceController.convertWithoutAssessment(unknownFT)
        val expectedResult = List(Unknown(testDate, unknownFT.mainType.getOrElse("Unknown"), -testBigDecimal))
        result mustBe expectedResult
      }

      "FinancialTransaction case (4815,2215)" in {
        val result = BalanceController.convertWithoutAssessment(ReturnChargeInterestFT)
        val expectedResult = List(ReturnChargeInterest(testDate, -testBigDecimal))
        result mustBe expectedResult
      }

      "FinancialTransaction case (4820,1540)" in {
        val result = BalanceController.convertWithoutAssessment(CentralAssessmentFT)
        result mustBe Nil
      }

      "FinancialTransaction case (4825,2215)" in {
        val result = BalanceController.convertWithoutAssessment(CentralAsstInterestFT)
        result mustBe Nil
      }

      "FinancialTransaction case (4830,1540)" in {
        val result = BalanceController.convertWithoutAssessment(OfficerAssessmentFT)
        result mustBe Nil
      }

      "FinancialTransaction case (4835,2215)" in {
        val result = BalanceController.convertWithoutAssessment(OfficerAsstInterestFT)
        result mustBe Nil
      }
    }

    "convert on list" should {
      val testNowDate = LocalDate.now()
      val testFinancialTransaction1 = FinancialTransaction(
        "",
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        Some("4820"),
        Some("1540"),
        testBigDecimal,
        None,
        None,
        List(
          SubItem(
            "",
            testNowDate,
            testBigDecimal,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None))
      )
      val testFinancialTransaction2 = FinancialTransaction(
        "",
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        Some("4820"),
        Some("1540"),
        testBigDecimal,
        None,
        None,
        List(emptySubItem, emptySubItem))
      val testFinancialTransaction3 = FinancialTransaction(
        "",
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        Some("4815"),
        Some("2215"),
        testBigDecimal,
        None,
        None,
        List(
          SubItem(
            "",
            testNowDate,
            testBigDecimal,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None))
      )
      val testFinancialTransaction4 = FinancialTransaction(
        "",
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        Some("4815"),
        Some("2215"),
        testBigDecimal,
        None,
        None,
        List(emptySubItem))

      "convert(in: List[FinancialTransactionResponse]) returns order non-duplicates" in {
        val result = BalanceController.convert(
          List(
            FinancialTransactionResponse("", "", "", LocalDateTime.now(), List(testFinancialTransaction1)),
            FinancialTransactionResponse("", "", "", LocalDateTime.now(), List(testFinancialTransaction2))
          ))

        result mustBe List(
          CentralAssessment(testDate, -testBigDecimal * 2),
          CentralAssessment(testNowDate, -testBigDecimal))
      }
      "convertWithoutAssessment(in: List[FinancialTransactionResponse]) returns order non-duplicates and without assessment" in {

        val result = BalanceController.convertWithoutAssessment(
          List(
            FinancialTransactionResponse("", "", "", LocalDateTime.now(), List(testFinancialTransaction3)),
            FinancialTransactionResponse(
              "",
              "",
              "",
              LocalDateTime.now(),
              List(testFinancialTransaction2, testFinancialTransaction4))
          ))

        result mustBe List(
          ReturnChargeInterest(testDate, -testBigDecimal),
          ReturnChargeInterest(testNowDate, -testBigDecimal))
      }
    }
  }

  "RichLineItems balance" in {
    val testDate = LocalDate.now()
    val testAmount = 123
    val testReturnChargeInterest = ReturnChargeInterest(testDate, testAmount)
    val testCentralAssessment = CentralAssessment(testDate, testAmount)
    val testCentralAsstInterest = CentralAsstInterest(testDate, testAmount)
    val testOfficerAssessment = OfficerAssessment(testDate, testAmount)
    val testOfficerAsstInterest = OfficerAsstInterest(testDate, testAmount)

    BalanceController
      .RichLineItems(
        List(
          testReturnChargeInterest,
          testCentralAssessment,
          testCentralAsstInterest,
          testOfficerAssessment,
          testOfficerAsstInterest))
      .balance mustBe testAmount * 5
  }
}
