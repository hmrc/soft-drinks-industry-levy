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

package uk.gov.hmrc.softdrinksindustrylevy.models

import java.time.LocalDate

import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.api.mvc.{RequestHeader, Result}
import play.mvc.Http
import sdil.models.{CentralAssessment, CentralAsstInterest, OfficerAssessment, OfficerAsstInterest, PaymentOnAccount, ReturnCharge, ReturnChargeInterest, ReturnPeriod}
import uk.gov.hmrc.play.test.UnitSpec

class MessagesSpec extends UnitSpec with MockitoSugar {

  class MessagesApiStub(messagesMap: Map[String, Map[String, String]]) extends MessagesApi {
    override def messages: Map[String, Map[String, String]] = messagesMap

    override def preferred(candidates: Seq[Lang]): Messages = ???

    override def preferred(request: RequestHeader): Messages = ???

    override def preferred(request: Http.RequestHeader): Messages = ???

    override def setLang(result: Result, lang: Lang): Result = ???

    override def clearLang(result: Result): Result = ???

    override def apply(key: String, args: Any*)(implicit lang: Lang): String = {
      val firstKey = key.split("""\.""")(0)
      val secondKey = key.split("""\.""")(1)

      messages.get(firstKey) match {
        case None    => ""
        case Some(x) => x.getOrElse(secondKey, "")
      }
    }

    override def apply(keys: Seq[String], args: Any*)(implicit lang: Lang): String = ???

    override def translate(key: String, args: Seq[Any])(implicit lang: Lang): Option[String] = ???

    override def isDefinedAt(key: String)(implicit lang: Lang): Boolean = ???

    override def langCookieName: String = ???

    override def langCookieSecure: Boolean = ???

    override def langCookieHttpOnly: Boolean = ???
  }

  val returnchargeTestVal = "returncharge value"
  val returnchargeinterestTestVal = "returnchargeinterest value"
  val centralassessmentTestVal = "centralassessment value"
  val centralasstinterestTestVal = "centralasstinterest value"
  val officerassessmentTestVal = "officerassessment value"
  val officerasstinterestTestVal = "officerasstinterest value"
  val paymentonaccountTestVal = "paymentonaccount value"

  val messagesMap: Map[String, Map[String, String]] = Map(
    "financiallineitem" -> Map(
      "returncharge"         -> returnchargeTestVal,
      "returnchargeinterest" -> returnchargeinterestTestVal,
      "centralassessment"    -> centralassessmentTestVal,
      "centralasstinterest"  -> centralasstinterestTestVal,
      "officerassessment"    -> officerassessmentTestVal,
      "officerasstinterest"  -> officerasstinterestTestVal,
      "paymentonaccount"     -> paymentonaccountTestVal
    )
  )

  implicit val messagesStub: Messages = MessagesImpl(mock[Lang], new MessagesApiStub(messagesMap))

  "messages" should {
    "ReturnCharge" in {
      ReturnCharge(ReturnPeriod(2018, 1), 1).description shouldBe returnchargeTestVal
    }

    "ReturnChargeInterest" in {
      ReturnChargeInterest(LocalDate.now(), 1).description shouldBe returnchargeinterestTestVal
    }

    "CentralAssessment" in {
      CentralAssessment(LocalDate.now(), 1).description shouldBe centralassessmentTestVal
    }

    "CentralAsstInterest" in {
      CentralAsstInterest(LocalDate.now(), 1).description shouldBe centralasstinterestTestVal
    }

    "OfficerAssessment" in {
      OfficerAssessment(LocalDate.now(), 1).description shouldBe officerassessmentTestVal
    }

    "OfficerAsstInterest" in {
      OfficerAsstInterest(LocalDate.now(), 1).description shouldBe officerasstinterestTestVal
    }

    "PaymentOnAccount" in {
      PaymentOnAccount(LocalDate.now(), "", 1, "", "").description shouldBe paymentonaccountTestVal
    }
  }
}
