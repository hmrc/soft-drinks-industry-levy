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

package uk.gov.hmrc.softdrinksindustrylevy.connectors

import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json._
import uk.gov.hmrc.softdrinksindustrylevy.models._
import uk.gov.hmrc.softdrinksindustrylevy.models.connectors.{arbAddress, arbSite, arbSubGet}
import uk.gov.hmrc.softdrinksindustrylevy.util.FakeApplicationSpec

class GetConnectorSpec
    extends FakeApplicationSpec with MockitoSugar with BeforeAndAfterEach with ScalaCheckPropertyChecks {

  import json.des.get._

  "GetConnector" should {
    "parse Site as expected" in {
      forAll { (r: Site) =>
        Json.toJson(r).as[Site] mustBe r
      }
    }

    "parse Address as expected" in {
      forAll { (r: Address) =>
        Json.toJson(r).as[Address] mustBe r
      }
    }

    "parse Subscription as expected" in {
      forAll { (r: Subscription) =>
        Json.toJson(r).as[Subscription] mustBe r
      }
    }
  }
}
