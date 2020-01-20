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

package uk.gov.hmrc.softdrinksindustrylevy.connectors

import org.scalatest._
import org.scalatest.prop.PropertyChecks
import play.api.libs.json._
import uk.gov.hmrc.softdrinksindustrylevy.models._
import uk.gov.hmrc.softdrinksindustrylevy.models.connectors.{arbAddress, arbSite, arbSubGet}

class GetConnectorSpec extends FunSuite with PropertyChecks with Matchers {

  import json.des.get._

  test("∀ Site: parse(toJson(x)) = x") {
    forAll { r: Site =>
      Json.toJson(r).as[Site] should be(r)
    }
  }

  test("∀ Address: parse(toJson(x)) = x") {
    forAll { r: Address =>
      Json.toJson(r).as[Address] should be(r)
    }
  }

  test("∀ Subscription: parse(toJson(x)) = x") {
    forAll { r: Subscription =>
      Json.toJson(r).as[Subscription] should be(r)
    }
  }
}
