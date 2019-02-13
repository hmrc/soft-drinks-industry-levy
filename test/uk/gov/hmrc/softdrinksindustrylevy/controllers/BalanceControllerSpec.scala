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

import com.softwaremill.macwire.wire
import org.mockito.ArgumentMatchers.{any, eq => matching}
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import sdil.models.des.{FinancialTransaction, FinancialTransactionResponse}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.retrieve.{Credentials, EmptyRetrieval}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.softdrinksindustrylevy.connectors.DesConnector
import uk.gov.hmrc.softdrinksindustrylevy.util.FakeApplicationSpec
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BalanceControllerSpec extends FakeApplicationSpec with MockitoSugar with BeforeAndAfterEach {

  val mockDesConnector: DesConnector = mock[DesConnector]
  val mockAuthConnector: AuthConnector = mock[AuthConnector]

  val testBalanceController = wire[BalanceController]

  implicit lazy val hc: HeaderCarrier = new HeaderCarrier

  override def beforeEach() {
    reset(mockDesConnector)
  }

  when(mockAuthConnector.authorise[Credentials](any(), any())(any(), any()))
    .thenReturn(Future.successful(Credentials("cred-id", "GovernmentGateway")))

  when(mockAuthConnector.authorise[Unit](any(), matching(EmptyRetrieval))(any(), any())).thenReturn(Future.successful(()))

  "financial data with 2 payments with contractAccountCategory of 32 and one charge should be converted using 'convert' to 3 line items" in {

    val stream = getClass.getResourceAsStream("/des-financial-data-32-two-payments.json")
    import FinancialTransaction._
    val obj = Json.parse(stream).as[FinancialTransactionResponse]

    BalanceController.convert(obj)
      .length must be(3)
  }

  "financial data with 1 payment with contractAccountCategory of 32, one with contractAccountCategory of 33 and" +
    " one charge should be converted using 'convert' to 2 line items" in {

    val stream = getClass.getResourceAsStream("/des-financial-data-32-one-payment.json")
    import FinancialTransaction._
    val obj = Json.parse(stream).as[FinancialTransactionResponse]

    BalanceController.convert(obj)
      .length must be(2)
  }

  "financial data with 2 payments with contractAccountCategory of 32 and one charge should be converted using 'convertWithoutAssessment' to 3 line items" in {

    val stream = getClass.getResourceAsStream("/des-financial-data-32-two-payments.json")
    import FinancialTransaction._
    val obj = Json.parse(stream).as[FinancialTransactionResponse]

    BalanceController.convertWithoutAssessment(obj)
      .length must be(3)
  }

  "financial data with 1 payment with contractAccountCategory of 32, one with contractAccountCategory of 33 and" +
    " one charge should be converted using 'convertWithoutAssessment' to 2 line items" in {

    val stream = getClass.getResourceAsStream("/des-financial-data-32-one-payment.json")
    import FinancialTransaction._
    val obj = Json.parse(stream).as[FinancialTransactionResponse]

    BalanceController.convertWithoutAssessment(obj)
      .length must be(2)
  }


  "financial data with unknown type, 2 payments with contractAccountCategory of 32 and one charge should be converted using 'convertWithoutAssessment' to 4 line items" in {

    val stream = getClass.getResourceAsStream("/des-financial-data-32-two-payments-and-unknown.json")
    import FinancialTransaction._
    val obj = Json.parse(stream).as[FinancialTransactionResponse]

    BalanceController.convertWithoutAssessment(obj)
      .length must be(4)
  }

  "financial data with unknown type, 2 payments with contractAccountCategory of 32 and one charge should be converted using 'convert' to 4 line items" in {

    val stream = getClass.getResourceAsStream("/des-financial-data-32-two-payments-and-unknown.json")
    import FinancialTransaction._
    val obj = Json.parse(stream).as[FinancialTransactionResponse]

    BalanceController.convert(obj)
      .length must be(4)
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
}
