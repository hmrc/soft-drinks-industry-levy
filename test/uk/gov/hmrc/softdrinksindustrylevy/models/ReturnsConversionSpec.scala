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

import java.time.{Clock, LocalDate, LocalDateTime, ZoneId}
import com.github.fge.jackson.JsonLoader
import com.github.fge.jsonschema.main.JsonSchemaFactory
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FunSuite, Matchers}
import play.api.libs.json.Json
import uk.gov.hmrc.softdrinksindustrylevy.models.connectors.arbReturnReq
import uk.gov.hmrc.softdrinksindustrylevy.models.json.des.returns._
import sdil.models._

class ReturnsConversionSpec extends FunSuite with PropertyChecks with Matchers {

//  implicit val clock: Clock = Clock.systemDefaultZone()
  private val zone = ZoneId.systemDefault()

  implicit def period(implicit cl: Clock) = ReturnPeriod(LocalDate.now(cl))

  test("∀ Returns: toJson(x) is valid") {
    val validator = JsonSchemaFactory.byDefault.getValidator

    val stream = getClass.getResourceAsStream("/test/des-return.schema.json")
    val schemaText = scala.io.Source.fromInputStream(stream).getLines.mkString
    stream.close
    val schema = JsonLoader.fromString(schemaText)
    implicit val clock: Clock = Clock.systemDefaultZone()
    forAll { r: ReturnsRequest =>
      val json = JsonLoader.fromString(Json.prettyPrint(Json.toJson(r)))
      val report = validator.validate(schema, json)
      assert(report.isSuccess, report)
    }
  }

  test("Period key is first quarter when date is before 1st April") {
    val date = LocalDateTime.of(2018, 1, 1, 12, 0).atZone(zone).toInstant
    implicit val clock: Clock = Clock.fixed(date, zone)
    forAll { r: ReturnsRequest =>
      val json = Json.toJson(r)
      assert((json \ "periodKey").as[String] == "18C1")
    }
  }

  test("Period key is second quarter when date is equal to or after 1st April and before 1st July") {
    val date = LocalDateTime.of(2018, 6, 30, 12, 0).atZone(zone).toInstant
    implicit val clock: Clock = Clock.fixed(date, zone)
    forAll { r: ReturnsRequest =>
      val json = Json.toJson(r)
      assert((json \ "periodKey").as[String] == "18C2")
    }
  }

  test("Period key is third quarter when date is equal to or after 1st July and before 1st October") {
    val date = LocalDateTime.of(2018, 7, 1, 12, 0).atZone(zone).toInstant
    implicit val clock: Clock = Clock.fixed(date, zone)
    forAll { r: ReturnsRequest =>
      val json = Json.toJson(r)
      assert((json \ "periodKey").as[String] == "18C3")
    }
  }

  test("Period key is fourth quarter when date is equal to or after 1st October and equal to or before 31st December") {
    val date = LocalDateTime.of(2018, 12, 31, 12, 0).atZone(zone).toInstant
    implicit val clock: Clock = Clock.fixed(date, zone)
    forAll { r: ReturnsRequest =>
      val json = Json.toJson(r)
      assert((json \ "periodKey").as[String] == "18C4")
    }
  }

}
