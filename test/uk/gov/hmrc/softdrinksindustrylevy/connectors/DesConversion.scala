/*
 * Copyright 2017 HM Revenue & Customs
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

import com.github.fge.jackson.JsonLoader
import com.github.fge.jsonschema.main._
import org.scalacheck._
import org.scalatest._
import org.scalatest.prop.PropertyChecks
import play.api.libs.json._
import uk.gov.hmrc.softdrinksindustrylevy.models._
import uk.gov.hmrc.softdrinksindustrylevy.models.gen.{arbSubRequest}

class DesConversionSpec extends FunSuite with PropertyChecks with Matchers {

  test("∀ Subscription: toJson(x) is valid") {
    import json.des.create._
    
    val validator = JsonSchemaFactory.byDefault.getValidator

    val stream = getClass.getResourceAsStream(
      "/test/des-create-subscription.schema.json")
    val schemaText = scala.io.Source.fromInputStream( stream ).getLines.mkString
    stream.close
    val schema = JsonLoader.fromString(schemaText)
    forAll { r: Subscription =>
      val json = JsonLoader.fromString(Json.prettyPrint(Json.toJson(r)))
      val report = validator.validate(schema, json)
      assert(report.isSuccess, report)
    }
  }  
}
