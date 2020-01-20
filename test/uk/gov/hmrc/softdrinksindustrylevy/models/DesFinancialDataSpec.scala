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

package sdil.models.des

import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.prop.PropertyChecks
import play.api.libs.json._

class DesFinancialDataSpec extends FlatSpec with Matchers with PropertyChecks {

  "A FinancialDataResponse" should "be readable from DES's sample record" in {
    val stream = getClass.getResourceAsStream("/des-financial-data.sample.json")
    import FinancialTransaction._

    val obj = Json.parse(stream).as[FinancialTransactionResponse]
    obj.idType == "ZSDIL"
  }

}
