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

package uk.gov.hmrc.softdrinksindustrylevy.connectors

import org.scalacheck._
import org.scalatest._
import org.scalatest.prop.PropertyChecks
import play.api.libs.json._
import uk.gov.hmrc.softdrinksindustrylevy.models._
import uk.gov.hmrc.softdrinksindustrylevy.models.gen.{arbAddress, arbActivity, arbContact, arbSubRequest}

class DesConnectorSpec extends FunSuite with PropertyChecks with Matchers {

  import json.internal._

  test("∀ Activity: parse(toJson(x)) = x") {
    forAll { r: Activity =>
      Json.toJson(r).as[Activity] should be (r)
    }
  }

  test("∀ UkAddress: parse(toJson(x)) = x") {
    forAll { r: Address =>
      Json.toJson(r).as[Address] should be (r)
    }
  }

  test("∀ Contact: parse(toJson(x)) = x") {
    forAll { r: Contact =>
      Json.toJson(r).as[Contact] should be (r)
    }
  }  

  test("∀ Subscription: parse(toJson(x)) = x") {
    forAll { r: Subscription =>
      Json.toJson(r).as[Subscription] should be (r)
    }
  }
}
