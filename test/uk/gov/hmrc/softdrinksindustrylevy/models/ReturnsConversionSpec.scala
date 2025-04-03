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

package uk.gov.hmrc.softdrinksindustrylevy.models

import java.time.{Clock, LocalDate, LocalDateTime, ZoneId}
import com.github.fge.jackson.JsonLoader
import com.github.fge.jsonschema.main.JsonSchemaFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.Json
import uk.gov.hmrc.softdrinksindustrylevy.models.connectors.arbReturnReq
import uk.gov.hmrc.softdrinksindustrylevy.models.json.des.returns._
import sdil.models._

class ReturnsConversionSpec extends AnyWordSpec with ScalaCheckPropertyChecks with Matchers {

//  implicit val clock: Clock = Clock.systemDefaultZone()
  private val zone = ZoneId.systemDefault()

  implicit def period(implicit cl: Clock): ReturnPeriod = ReturnPeriod(LocalDate.now(cl))

  "ReturnsConversion" should {
    "parse Returns as expected" in {
      val validator = JsonSchemaFactory.byDefault.getValidator

      val stream = getClass.getResourceAsStream("/test/des-return.schema.json")
      val schemaText = scala.io.Source.fromInputStream(stream).getLines().mkString
      stream.close
      val schema = JsonLoader.fromString(schemaText)
      implicit val clock: Clock = Clock.systemDefaultZone()
      forAll { r: ReturnsRequest =>
        val json = JsonLoader.fromString(Json.prettyPrint(Json.toJson(r)))
        val report = validator.validate(schema, json)
        assert(report.isSuccess, report)
      }
    }

    "Period key is first quarter when date is before 1st April" in {
      val date = LocalDateTime.of(2018, 1, 1, 12, 0).atZone(zone).toInstant
      implicit val clock: Clock = Clock.fixed(date, zone)
      forAll { r: ReturnsRequest =>
        val json = Json.toJson(r)
        assert((json \ "periodKey").as[String] == "18C1")
      }
    }

    "Period key is second quarter when date is equal to or after 1st April and before 1st July" in {
      val date = LocalDateTime.of(2018, 6, 30, 12, 0).atZone(zone).toInstant
      implicit val clock: Clock = Clock.fixed(date, zone)
      forAll { r: ReturnsRequest =>
        val json = Json.toJson(r)
        assert((json \ "periodKey").as[String] == "18C2")
      }
    }

    "Period key is third quarter when date is equal to or after 1st July and before 1st October" in {
      val date = LocalDateTime.of(2018, 7, 1, 12, 0).atZone(zone).toInstant
      implicit val clock: Clock = Clock.fixed(date, zone)
      forAll { r: ReturnsRequest =>
        val json = Json.toJson(r)
        assert((json \ "periodKey").as[String] == "18C3")
      }
    }

    "Period key is fourth quarter when date is equal to or after 1st October and equal to or before 31st December" in {
      val date = LocalDateTime.of(2018, 12, 31, 12, 0).atZone(zone).toInstant
      implicit val clock: Clock = Clock.fixed(date, zone)
      forAll { r: ReturnsRequest =>
        val json = Json.toJson(r)
        assert((json \ "periodKey").as[String] == "18C4")
      }
    }

    //TODO: Test lowLevy and highLevy are used in monetaryWrites of models/returns.scala (as is dueLevy) to form lowVolume and highVolume within the returnsRequestFormat/writesForAuditing.
    //
    //This is then used in Line 96 of ReturnsController within the buildReturnAuditDetail method and also (more importantly) in Line 134 Json.toJson(returnsRequest) of the method submitReturn within DesConnector.
    //
    //dueLevy is also used in this way to form levySubtotal
    // monetaryWrites
    // in addition, can test netLevyDueTotal/totalLevy
    //  RELATING TO RETURNS - DO FIRST
    "packaged" should {
      "volumeSmall" should {

      }

      "volumeLarge" should {

      }

      "monetaryWrites" should {
        "lowLevy" should {

        }

        "highLevy" should {

        }

        "dueLevy" should {

        }
      }
    }

    "imported" should {
      "volumeSmall" should {

      }

      "volumeLarge" should {

      }

      "monetaryWrites" should {
        "lowLevy" should {

        }

        "highLevy" should {

        }

        "dueLevy" should {

        }
      }
    }

    "exported" should {
      "volumeSmall" should {

      }

      "volumeLarge" should {

      }

      "monetaryWrites" should {
        "lowLevy" should {

        }

        "highLevy" should {

        }

        "dueLevy" should {

        }
      }
    }

    "wastage" should {
      "volumeSmall" should {

      }

      "volumeLarge" should {

      }

      "monetaryWrites" should {
        "lowLevy" should {

        }

        "highLevy" should {

        }

        "dueLevy" should {

        }
      }
    }

    "netLevyDueTotal" should {

    }
  }
}
