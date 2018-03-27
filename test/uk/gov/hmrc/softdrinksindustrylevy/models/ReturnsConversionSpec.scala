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

package uk.gov.hmrc.softdrinksindustrylevy.models

import com.github.fge.jackson.JsonLoader
import com.github.fge.jsonschema.main.JsonSchemaFactory
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FunSuite, Matchers}
import play.api.libs.json.Json
import uk.gov.hmrc.softdrinksindustrylevy.models.json.des.returns._
import uk.gov.hmrc.softdrinksindustrylevy.models.gen.arbReturnReq

class ReturnsConversionSpec extends FunSuite with PropertyChecks with Matchers {

  test("∀ Returns: toJson(x) is valid") {
    val validator = JsonSchemaFactory.byDefault.getValidator

    val stream = getClass.getResourceAsStream(
      "/test/des-return.schema.json")
    val schemaText = scala.io.Source.fromInputStream(stream).getLines.mkString
    stream.close
    val schema = JsonLoader.fromString(schemaText)
    forAll { r: ReturnsRequest =>
      val json = JsonLoader.fromString(Json.prettyPrint(Json.toJson(r)))
      val report = validator.validate(schema, json)
      println(json.toString)
      assert(report.isSuccess, report)
    }
  }
}
