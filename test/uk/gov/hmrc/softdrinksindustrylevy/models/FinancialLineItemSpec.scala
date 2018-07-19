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

package sdil.models

import java.time._
import org.scalatest.{ FlatSpec, Matchers }
import org.scalacheck.Arbitrary._
import org.scalacheck.Prop._
import org.scalacheck.Gen
import org.scalatest.prop.PropertyChecks
import play.api.libs.json._
import cats.implicits._
import java.time.LocalDate

class FinancialLineItemSpec extends FlatSpec with Matchers with PropertyChecks {

  "A FinancialLineItem" should "be serialisable" in {
    val item = new PaymentOnAccount(LocalDate.now, "blah", 1000)
    Json.toJson(item).as[FinancialLineItem] should be (item)
  }

}
