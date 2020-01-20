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

import org.scalatest._
import org.scalacheck.Arbitrary._
import org.scalatest.prop.PropertyChecks

class RosmResponseAddressSpec extends FunSuite with Matchers with PropertyChecks {

  test("Remove invalid characters from address lines") {
    forAll { (line1: String, line2: Option[String], line3: Option[String], line4: Option[String]) =>
      val address = RosmResponseAddress(line1, line2, line3, line4, "GB", "AB12 3CD")
      val pattern = "^[A-Za-z0-9 \\-,.&'\\/]*$"

      List(
        Some(address.addressLine1),
        address.addressLine2,
        address.addressLine3,
        address.addressLine4
      ).flatten.foreach {
        _.matches(pattern) should be(true)
      }
    }
  }

  test("Convert extended latin characters to their base form") {
    val addr = RosmResponseAddress("12 T́ḣè Stréët", None, None, None, "GB", "AB12 3CD")
    addr.addressLine1 should be("12 The Street")
  }

}
