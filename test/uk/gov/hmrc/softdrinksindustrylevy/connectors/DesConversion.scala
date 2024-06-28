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

import java.time.LocalDate
import com.github.fge.jackson.JsonLoader
import com.github.fge.jsonschema.main._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json._
import uk.gov.hmrc.softdrinksindustrylevy.models.ActivityType._
import uk.gov.hmrc.softdrinksindustrylevy.models._
import uk.gov.hmrc.softdrinksindustrylevy.models.connectors.arbSubRequest
import uk.gov.hmrc.softdrinksindustrylevy.models.json.des.create._
import uk.gov.hmrc.softdrinksindustrylevy.services.JsonSchemaChecker
import uk.gov.hmrc.softdrinksindustrylevy.util.FakeApplicationSpec

class DesConversionSpec
    extends FakeApplicationSpec with MockitoSugar with BeforeAndAfterEach with ScalaCheckPropertyChecks {

  "DesConversion" should {
    "parse Subscription as expected" in {
      val validator = JsonSchemaFactory.byDefault.getValidator
      val schema = JsonSchemaChecker.retrieveSchema("des-create-subscription")
      forAll { r: Subscription =>
        val json = JsonLoader.fromString(Json.prettyPrint(Json.toJson(r)))
        val report = validator.validate(schema, json)
        assert(report.isSuccess, report)
      }
    }

    "Small producer and copackee conversion includes all producer details" in {
      val copackeeSmallProd = baseSubscription.copy(
        activity = InternalActivity(
          Map(
            ProducedOwnBrand -> ((100L, 100L)),
            Copackee         -> ((100L, 100L))
          ),
          isLarge = false
        )
      )

      val transformedJson = Json.toJson(copackeeSmallProd)
      val details = (transformedJson \\ "details").head
      val producerDetails = (details \\ "producerDetails").head

      assert((details \ "producer").as[Boolean])
      assert(!(details \ "importer").as[Boolean])
      assert(!(details \ "contractPacker").as[Boolean])
      assert((producerDetails \ "produceMillionLitres").as[Boolean])
      assert((producerDetails \ "producerClassification").as[String] == "0")
      assert((producerDetails \ "smallProducerExemption").as[Boolean])
      assert((producerDetails \ "useContractPacker").as[Boolean])
      assert((producerDetails \ "voluntarilyRegistered").as[Boolean])
    }

    "Large producer conversion includes only produceMillionLitres and producerClassification" in {
      val largeProducer = baseSubscription.copy(
        activity = InternalActivity(
          Map(
            ProducedOwnBrand -> ((1000000L, 1000000L))
          ),
          isLarge = true
        )
      )

      val transformedJson = Json.toJson(largeProducer)
      val details = (transformedJson \\ "details").head
      val producerDetails = (details \\ "producerDetails").head

      assert((details \ "producer").as[Boolean])
      assert(!(details \ "importer").as[Boolean])
      assert(!(details \ "contractPacker").as[Boolean])
      assert(!(producerDetails \ "produceMillionLitres").as[Boolean])
      assert((producerDetails \ "producerClassification").as[String] == "1")
      assert((producerDetails \ "smallProducerExemption").asOpt[Boolean].isEmpty)
      assert((producerDetails \ "useContractPacker").asOpt[Boolean].isEmpty)
      assert((producerDetails \ "voluntarilyRegistered").asOpt[Boolean].isEmpty)
    }

    "Large producer and importer conversion includes only produceMillionLitres and producerClassification" in {
      val largeProducerImport = baseSubscription.copy(
        activity = InternalActivity(
          Map(
            ProducedOwnBrand -> ((1000000L, 1000000L)),
            Imported         -> ((1L, 1L))
          ),
          isLarge = true
        )
      )

      val transformedJson = Json.toJson(largeProducerImport)
      val details = (transformedJson \\ "details").head
      val producerDetails = (details \\ "producerDetails").head

      assert((details \ "producer").as[Boolean])
      assert((details \ "importer").as[Boolean])
      assert(!(details \ "contractPacker").as[Boolean])
      assert(!(producerDetails \ "produceMillionLitres").as[Boolean])
      assert((producerDetails \ "producerClassification").as[String] == "1")
      assert((producerDetails \ "smallProducerExemption").asOpt[Boolean].isEmpty)
      assert((producerDetails \ "useContractPacker").asOpt[Boolean].isEmpty)
      assert((producerDetails \ "voluntarilyRegistered").asOpt[Boolean].isEmpty)
    }

    "Large producer and contract packer conversion includes only produceMillionLitres and producerClassification" in {
      val largeProducerCopacker = baseSubscription.copy(
        activity = InternalActivity(
          Map(
            ProducedOwnBrand -> ((1000000L, 1000000L)),
            CopackerAll      -> ((1L, 1L))
          ),
          isLarge = true
        )
      )

      val transformedJson = Json.toJson(largeProducerCopacker)
      val details = (transformedJson \\ "details").head
      val producerDetails = (details \\ "producerDetails").head

      assert((details \ "producer").as[Boolean])
      assert(!(details \ "importer").as[Boolean])
      assert((details \ "contractPacker").as[Boolean])
      assert(!(producerDetails \ "produceMillionLitres").as[Boolean])
      assert((producerDetails \ "producerClassification").as[String] == "1")
      assert((producerDetails \ "smallProducerExemption").asOpt[Boolean].isEmpty)
      assert((producerDetails \ "useContractPacker").asOpt[Boolean].isEmpty)
      assert((producerDetails \ "voluntarilyRegistered").asOpt[Boolean].isEmpty)
    }

    "Large producer, importer and contract packer conversion includes only produceMillionLitres and producerClassification" in {
      val largeProducerCopackerImport = baseSubscription.copy(
        activity = InternalActivity(
          Map(
            ProducedOwnBrand -> ((1000000L, 1000000L)),
            CopackerAll      -> ((1L, 1L)),
            Imported         -> ((1L, 1L))
          ),
          isLarge = true
        )
      )

      val transformedJson = Json.toJson(largeProducerCopackerImport)
      val details = (transformedJson \\ "details").head
      val producerDetails = (details \\ "producerDetails").head

      assert((details \ "producer").as[Boolean])
      assert((details \ "importer").as[Boolean])
      assert((details \ "contractPacker").as[Boolean])
      assert(!(producerDetails \ "produceMillionLitres").as[Boolean])
      assert((producerDetails \ "producerClassification").as[String] == "1")
      assert((producerDetails \ "smallProducerExemption").asOpt[Boolean].isEmpty)
      assert((producerDetails \ "useContractPacker").asOpt[Boolean].isEmpty)
      assert((producerDetails \ "voluntarilyRegistered").asOpt[Boolean].isEmpty)
    }

    "Large producer and copackee conversion includes only produceMillionLitres and producerClassification" in {
      val largeProducerCopackerImport = baseSubscription.copy(
        activity = InternalActivity(
          Map(
            ProducedOwnBrand -> ((1000000L, 1000000L)),
            Copackee         -> ((1L, 1L))
          ),
          isLarge = true
        )
      )

      val transformedJson = Json.toJson(largeProducerCopackerImport)
      val details = (transformedJson \\ "details").head
      val producerDetails = (details \\ "producerDetails").head

      assert((details \ "producer").as[Boolean])
      assert(!(details \ "importer").as[Boolean])
      assert(!(details \ "contractPacker").as[Boolean])
      assert(!(producerDetails \ "produceMillionLitres").as[Boolean])
      assert((producerDetails \ "producerClassification").as[String] == "1")
      assert((producerDetails \ "smallProducerExemption").asOpt[Boolean].isEmpty)
      assert((producerDetails \ "useContractPacker").asOpt[Boolean].isEmpty)
      assert((producerDetails \ "voluntarilyRegistered").asOpt[Boolean].isEmpty)
    }

    "Small producer and contract packer conversion includes only produceMillionLitres and producerClassification" in {
      val smallProducerCopacker = baseSubscription.copy(
        activity = InternalActivity(
          Map(
            ProducedOwnBrand -> ((1L, 1L)),
            CopackerAll      -> ((1L, 1L))
          ),
          isLarge = false
        )
      )

      val transformedJson = Json.toJson(smallProducerCopacker)
      val details = (transformedJson \\ "details").head
      val producerDetails = (details \\ "producerDetails").head

      assert((details \ "producer").as[Boolean])
      assert(!(details \ "importer").as[Boolean])
      assert((details \ "contractPacker").as[Boolean])
      assert((producerDetails \ "produceMillionLitres").as[Boolean])
      assert((producerDetails \ "producerClassification").as[String] == "0")
      assert((producerDetails \ "smallProducerExemption").asOpt[Boolean].isEmpty)
      assert((producerDetails \ "useContractPacker").asOpt[Boolean].isEmpty)
      assert((producerDetails \ "voluntarilyRegistered").asOpt[Boolean].isEmpty)
    }

    "Small producer and importer conversion includes only produceMillionLitres and producerClassification" in {
      val smallProducerImport = baseSubscription.copy(
        activity = InternalActivity(
          Map(
            ProducedOwnBrand -> ((1L, 1L)),
            Imported         -> ((1L, 1L))
          ),
          isLarge = false
        )
      )

      val transformedJson = Json.toJson(smallProducerImport)
      val details = (transformedJson \\ "details").head
      val producerDetails = (details \\ "producerDetails").head

      assert((details \ "producer").as[Boolean])
      assert((details \ "importer").as[Boolean])
      assert(!(details \ "contractPacker").as[Boolean])
      assert((producerDetails \ "produceMillionLitres").as[Boolean])
      assert((producerDetails \ "producerClassification").as[String] == "0")
      assert((producerDetails \ "smallProducerExemption").asOpt[Boolean].isEmpty)
      assert((producerDetails \ "useContractPacker").asOpt[Boolean].isEmpty)
      assert((producerDetails \ "voluntarilyRegistered").asOpt[Boolean].isEmpty)
    }

    "Small producer, importer and copacker conversion includes only produceMillionLitres and producerClassification" in {
      val smallProducerCopackerImport = baseSubscription.copy(
        activity = InternalActivity(
          Map(
            ProducedOwnBrand -> ((1L, 1L)),
            Imported         -> ((1L, 1L)),
            CopackerAll      -> ((1L, 1L))
          ),
          isLarge = false
        )
      )

      val transformedJson = Json.toJson(smallProducerCopackerImport)
      val details = (transformedJson \\ "details").head
      val producerDetails = (details \\ "producerDetails").head

      assert((details \ "producer").as[Boolean])
      assert((details \ "importer").as[Boolean])
      assert((details \ "contractPacker").as[Boolean])
      assert((producerDetails \ "produceMillionLitres").as[Boolean])
      assert((producerDetails \ "producerClassification").as[String] == "0")
      assert((producerDetails \ "smallProducerExemption").asOpt[Boolean].isEmpty)
      assert((producerDetails \ "useContractPacker").asOpt[Boolean].isEmpty)
      assert((producerDetails \ "voluntarilyRegistered").asOpt[Boolean].isEmpty)
    }

    "Small producer, importer and copackee conversion includes only produceMillionLitres and producerClassification" in {
      val smallProducerCopackeeImport = baseSubscription.copy(
        activity = InternalActivity(
          Map(
            ProducedOwnBrand -> ((1L, 1L)),
            Imported         -> ((1L, 1L)),
            Copackee         -> ((1L, 1L))
          ),
          isLarge = false
        )
      )

      val transformedJson = Json.toJson(smallProducerCopackeeImport)
      val details = (transformedJson \\ "details").head
      val producerDetails = (details \\ "producerDetails").head

      assert((details \ "producer").as[Boolean])
      assert((details \ "importer").as[Boolean])
      assert(!(details \ "contractPacker").as[Boolean])
      assert((producerDetails \ "produceMillionLitres").as[Boolean])
      assert((producerDetails \ "producerClassification").as[String] == "0")
      assert((producerDetails \ "smallProducerExemption").asOpt[Boolean].isEmpty)
      assert((producerDetails \ "useContractPacker").asOpt[Boolean].isEmpty)
      assert((producerDetails \ "voluntarilyRegistered").asOpt[Boolean].isEmpty)
    }
  }

  private lazy val baseSubscription = Subscription(
    utr = "1111111111",
    sdilRef = None,
    orgName = "I AM THE ORGANISATION TO RULE",
    orgType = Some("3"),
    address = UkAddress(List("6A Gunson Street, South East London"), "SE79 6NF"),
    activity = InternalActivity(Map.empty, isLarge = false),
    liabilityDate = LocalDate.parse("2018-04-06"),
    productionSites =
      List(Site(UkAddress(List("99 Burntscarthgreen", "North West London"), "NW33 9CV"), None, None, None)),
    warehouseSites = List(
      Site(UkAddress(List("128 Willowbank Close", "Bristol"), "BS78 5CB"), None, None, None),
      Site(UkAddress(List("17 Trebarthen Terrace", "Northampton"), "NN08 2CC"), None, None, None)
    ),
    contact = Contact(
      Some("Evelyn Hindmarsh"),
      Some("pimirzalvqlsljtiwgIiqzljnKpofqguhwKiwkcbzfoykggiwskbarsbikwwfsgI"),
      "00779 705682",
      "nkzkjldisu@zmzlddpexr.co.uk"
    ),
    endDate = None
  )

}
