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

import org.mockito.ArgumentMatchers.{any, eq => matching}
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.libs.json.{JsNumber, Json}
import play.api.mvc.{ControllerComponents, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import sdil.models.des.{FinancialTransaction, FinancialTransactionResponse, SubItem}
import sdil.models._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.retrieve.{Credentials, EmptyRetrieval}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.softdrinksindustrylevy.connectors.DesConnector
import uk.gov.hmrc.softdrinksindustrylevy.util.FakeApplicationSpec

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.{ExecutionContext, Future}

class BalanceControllerSpec extends FakeApplicationSpec with MockitoSugar with BeforeAndAfterEach with ScalaFutures {

  val mockDesConnector: DesConnector = mock[DesConnector]
  val mockAuthConnector: AuthConnector = mock[AuthConnector]

  implicit lazy val hc: HeaderCarrier = new HeaderCarrier

  implicit val messages: Messages = messagesApi.preferred(request)
  implicit lazy val request: Request[?] = FakeRequest()

  implicit lazy val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  val cc = app.injector.instanceOf[ControllerComponents]

  val serviceConfig = mock[ServicesConfig]

  val testBalanceController: BalanceController =
    new BalanceController(mockAuthConnector, mockDesConnector, cc, serviceConfig)

  override def beforeEach(): Unit =
    reset(mockDesConnector)

  when(mockAuthConnector.authorise[Credentials](any(), any())(using any(), any()))
    .thenReturn(Future.successful(Credentials("cred-id", "GovernmentGateway")))

  when(mockAuthConnector.authorise[Unit](any(), matching(EmptyRetrieval))(using any(), any()))
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
    when(mockDesConnector.retrieveFinancialData(any(), any())(using any()))
      .thenReturn(Future successful None)
    when(serviceConfig.getBoolean(any())).thenReturn(true)
    val response = testBalanceController.balance("0000222200", false)(FakeRequest())

    status(response) mustBe 200
  }

  "return Status: OK Body: outstandingAmount when checking for balance which has only 1 financial transaction with contractAccountCategory of 32 data that contains outstandingAmount - 1 transaction" in {
    val testDate = LocalDate.of(2019, 4, 9)
    val testBigDecimal = BigDecimal(123)
    val emptySubItem = SubItem("", testDate, testBigDecimal)

    val financialTransactionIncludingOutstandingBalance = FinancialTransaction(
      "",
      mainTransaction = Some("4815"),
      subTransaction = Some("2215"),
      originalAmount = testBigDecimal,
      items = List(emptySubItem),
      outstandingAmount = Some(-0.01),
      contractAccountCategory = Some("32")
    )
    val financialTransactionResponseIncludingOutstandingBalance = FinancialTransactionResponse(
      "",
      "",
      "",
      LocalDateTime.now(),
      List(financialTransactionIncludingOutstandingBalance)
    )

    when(serviceConfig.getBoolean(any())).thenReturn(true)

    when(mockDesConnector.retrieveFinancialData(any(), any())(using any()))
      .thenReturn(Future successful Some(financialTransactionResponseIncludingOutstandingBalance))
    val response = testBalanceController.balance("0000222200", false)(FakeRequest())

    status(response) mustBe 200
    contentAsJson(response) mustBe JsNumber(0.01)
  }

  "return Status: OK Body: outstandingAmount when checking for balance which has only 1 financial transaction with contractAccountCategory of 32 data that contains outstandingAmount - multiple transactions" in {
    val testDate = LocalDate.of(2019, 4, 9)
    val testBigDecimal = BigDecimal(123)
    val emptySubItem = SubItem("", testDate, testBigDecimal)

    val financialTransactionIncludingOutstandingBalance = FinancialTransaction(
      "",
      mainTransaction = Some("4815"),
      subTransaction = Some("2215"),
      originalAmount = testBigDecimal,
      items = List(emptySubItem),
      outstandingAmount = Some(-0.01),
      contractAccountCategory = Some("32")
    )
    val financialTransactionIncludingOutstandingBalanceWithIncorrectAccountCategory = FinancialTransaction(
      "",
      mainTransaction = Some("0060"),
      subTransaction = Some("0100"),
      originalAmount = testBigDecimal,
      items = List(emptySubItem),
      outstandingAmount = Some(10000),
      contractAccountCategory = Some("48")
    )
    val financialTransactionResponseIncludingOutstandingBalance = FinancialTransactionResponse(
      "",
      "",
      "",
      LocalDateTime.now(),
      List(
        financialTransactionIncludingOutstandingBalance,
        financialTransactionIncludingOutstandingBalanceWithIncorrectAccountCategory
      )
    )

    when(serviceConfig.getBoolean(any())).thenReturn(true)

    when(mockDesConnector.retrieveFinancialData(any(), any())(using any()))
      .thenReturn(Future successful Some(financialTransactionResponseIncludingOutstandingBalance))
    val response = testBalanceController.balance("0000222200", false)(FakeRequest())

    status(response) mustBe 200
    contentAsJson(response) mustBe JsNumber(0.01)
  }

  "return Status: OK Body: None when checking for balance history but no data is found" in {
    when(mockDesConnector.retrieveFinancialData(any(), any())(using any()))
      .thenReturn(Future successful None)
    val response = testBalanceController.balanceHistory("0000222200", 2018)(FakeRequest())

    status(response) mustBe 200
  }

  "return Status: OK Body: None when checking for balance history all but no data is found" in {
    when(mockDesConnector.retrieveFinancialData(any(), any())(using any()))
      .thenReturn(Future successful None)
    when(mockDesConnector.retrieveSubscriptionDetails(any(), any())(using any()))
      .thenReturn(Future successful Some(sub))
    val response = testBalanceController.balanceHistoryAll("0000222200", false)(FakeRequest())

    status(response) mustBe 200
  }

  "convert methods" should {
    val testDate = LocalDate.of(2019, 4, 9)
    val testBigDecimal = BigDecimal(123)
    val emptySubItem = SubItem("", testDate, testBigDecimal)

    val emptyFT = FinancialTransaction("", originalAmount = testBigDecimal, items = List(emptySubItem))

    val unknownFT = FinancialTransaction(
      "",
      contractAccountCategory = Some("32"),
      originalAmount = testBigDecimal,
      items = List(emptySubItem)
    )

    val ReturnChargeInterestFT = FinancialTransaction(
      "",
      mainTransaction = Some("4815"),
      subTransaction = Some("2215"),
      originalAmount = testBigDecimal,
      items = List(emptySubItem)
    )

    val CentralAssessmentFT = FinancialTransaction(
      "",
      mainTransaction = Some("4820"),
      subTransaction = Some("1540"),
      originalAmount = testBigDecimal,
      items = List(emptySubItem)
    )

    val CentralAsstInterestFT =
      FinancialTransaction(
        "",
        mainTransaction = Some("4825"),
        subTransaction = Some("2215"),
        originalAmount = testBigDecimal,
        items = List(emptySubItem)
      )

    val OfficerAssessmentFT = FinancialTransaction(
      "",
      mainTransaction = Some("4830"),
      subTransaction = Some("1540"),
      originalAmount = testBigDecimal,
      items = List(emptySubItem)
    )

    val OfficerAsstInterestFT = FinancialTransaction(
      "",
      mainTransaction = Some("4835"),
      subTransaction = Some("2215"),
      originalAmount = testBigDecimal,
      items = List(emptySubItem)
    )

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

      "FinancialTransaction case (60,100) " in {
        val testFinancialTransaction = FinancialTransaction(
          "",
          contractAccountCategory = Some("32"),
          mainTransaction = Some("60"),
          subTransaction = Some("100"),
          originalAmount = testBigDecimal,
          items = List(emptySubItem)
        )

        val result = BalanceController.convert(testFinancialTransaction)
        result.length mustBe 1

        result.head match {
          case PaymentOnAccount(date, reference, amount, lot, lotItem) =>
            date mustBe testDate
            reference.length mustBe 10
            amount mustBe -testBigDecimal
            lot.length mustBe 10
            lotItem.length mustBe 10
          case _ => fail()
        }
      }

      "FinancialTransaction case (60,100) with item for outgoing payment" in {
        val subItemWithOutgoingPayment = emptySubItem.copy(
          amount = -321,
          outgoingPaymentMethod = Some("Payment method")
        )
        val testFinancialTransaction = FinancialTransaction(
          "",
          contractAccountCategory = Some("32"),
          mainTransaction = Some("60"),
          subTransaction = Some("100"),
          originalAmount = testBigDecimal,
          items = List(emptySubItem, subItemWithOutgoingPayment)
        )

        val result = BalanceController.convert(testFinancialTransaction)
        result.length mustBe 1

        result.head match {
          case PaymentOnAccount(date, reference, amount, lot, lotItem) =>
            date mustBe testDate
            reference.length mustBe 10
            amount mustBe -testBigDecimal
            lot.length mustBe 10
            lotItem.length mustBe 10
          case _ => fail()
        }
      }

      "FinancialTransaction case (60,100) with item for outgoing payment only" in {
        val subItemWithOutgoingPayment = emptySubItem.copy(
          amount = -321,
          outgoingPaymentMethod = Some("Payment method")
        )
        val testFinancialTransaction = FinancialTransaction(
          "",
          contractAccountCategory = Some("32"),
          mainTransaction = Some("60"),
          subTransaction = Some("100"),
          originalAmount = testBigDecimal,
          items = List(subItemWithOutgoingPayment)
        )

        val result = BalanceController.convert(testFinancialTransaction)
        result.length mustBe 0

        result.isEmpty mustBe true
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
        mainTransaction = Some("4820"),
        subTransaction = Some("1540"),
        originalAmount = testBigDecimal,
        items = List(SubItem("", testNowDate, testBigDecimal))
      )
      val testFinancialTransaction2 =
        FinancialTransaction(
          "",
          mainTransaction = Some("4820"),
          subTransaction = Some("1540"),
          originalAmount = testBigDecimal,
          items = List(emptySubItem, emptySubItem)
        )

      val testFinancialTransaction3 = FinancialTransaction(
        "",
        mainTransaction = Some("4815"),
        subTransaction = Some("2215"),
        originalAmount = testBigDecimal,
        items = List(SubItem("", testNowDate, testBigDecimal))
      )
      val testFinancialTransaction4 = FinancialTransaction(
        "",
        mainTransaction = Some("4815"),
        subTransaction = Some("2215"),
        originalAmount = testBigDecimal,
        items = List(emptySubItem)
      )

      "convert(in: List[FinancialTransactionResponse]) returns order non-duplicates" in {
        val result = BalanceController.convert(
          List(
            FinancialTransactionResponse("", "", "", LocalDateTime.now(), List(testFinancialTransaction1)),
            FinancialTransactionResponse("", "", "", LocalDateTime.now(), List(testFinancialTransaction2))
          )
        )

        result mustBe List(
          CentralAssessment(testDate, -testBigDecimal * 2),
          CentralAssessment(testNowDate, -testBigDecimal)
        )
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
              List(testFinancialTransaction2, testFinancialTransaction4)
            )
          )
        )

        result mustBe List(
          ReturnChargeInterest(testDate, -testBigDecimal),
          ReturnChargeInterest(testNowDate, -testBigDecimal)
        )
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
          testOfficerAsstInterest
        )
      )
      .balance mustBe testAmount * 5
  }
}
