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

import controllers._
import org.scalacheck._
import org.scalatest._
import org.scalatest.prop.PropertyChecks
import uk.gov.hmrc.softdrinksindustrylevy.models._
import uk.gov.hmrc.smartstub._
import uk.gov.hmrc.smartstub.AutoGen.{providerStringNamed => _,_}
import play.api.libs.json._
import com.github.fge.jsonschema.core.report.ProcessingReport
import com.github.fge.jsonschema.main._
import com.github.fge.jackson.JsonLoader
import com.fasterxml.jackson.databind.JsonNode


import java.time._

class DesConnectorSpec extends FunSuite with PropertyChecks with Matchers {

  implicit def providerLongNamed(s: String): GenProvider[Long] =
    instance(Gen.choose(1, 1000))

  implicit def providerLocalDate(s: String): GenProvider[LocalDate] =
    instance(Gen.date)

  implicit def providerLDTNamed(s: String): GenProvider[LocalDateTime] =
    instance(Gen.date.map{_.atStartOfDay})

  implicit def providerBDNamed(s: String): GenProvider[BigDecimal] =
    instance(Gen.choose(1, 1000).map{x => BigDecimal(x.toString)})  

  private val nonEmptyString =
    Gen.alphaStr.flatMap{t => Gen.alphaChar.map(_ + t)}

  implicit def providerStringNamed(s: String): GenProvider[String] = instance ({
    s.toLowerCase match {
      case "action" => Gen.const("1")
      case "producerclassification" => Gen.oneOf({0 to 1}.map(_.toString))        
      case "organisationtype" => Gen.oneOf({1 to 5}.map(_.toString))
      case "sitetype" => Gen.oneOf({1 to 2}.map(_.toString))        
      case "postcode" => Gen.postcode
      case "entitytype" => Gen.const("4")
      case "line1" =>
        Gen.ukAddress.
          map(_.head.take(35)).
          retryUntil(_.matches("^[A-Za-z0-9 \\-,.&'\\/]{1,35}$"))
      case l if l.startsWith("line") =>
        Gen.ukAddress.
          flatMap{x => Gen.oneOf(x.tail.init.map(_.take(35)))}.
          retryUntil(_.matches("^[A-Za-z0-9 \\-,.&'\\/]{1,35}$"))
      case "country" => Gen.oneOf("ADAEAFAGAIALAMANAOAQARASATAUAWAXAZBABBBDBEBFBGBHBIBJBLBMBNBOBQBRBSBTBVBWBYBZCACCCDCFCGCHCICKCLCMCNCOCRCSCUCVCWCXCYCZDEDJDKDMDODZECEEEGEHERESETEUFCFIFJFKFMFOFRGAGBGDGEGFGGGHGIGLGMGNGPGQGRGSGTGUGWGYHKHMHNHRHTHUIDIEILIMINIOIQIRISITJEJMJOJPKEKGKHKIKMKNKPKRKWKYKZLALBLCLILKLRLSLTLULVLYMAMCMDMEMFMGMHMKMLMMMNMOMPMQMRMSMTMUMVMWMXMYMZNANCNENFNGNINLNONPNRNTNUNZOMORPAPEPFPGPHPKPLPMPNPRPSPTPWPYQARERORSRURWSASBSCSDSESGSHSISJSKSLSMSNSOSRSSSTSVSXSYSZTCTDTFTGTHTJTKTLTMTNTOTPTRTTTVTWTZUAUGUMUNUSUYUZVAVCVEVGVIVNVUWFWSYEYTZAZMZWZZ".grouped(2).toList)
      case "tradingname" | "positionincompany" => nonEmptyString
      case "cin" => Enumerable.instances.utrEnum.gen
      case "telephone" | "mobile" | "fax" => Gen.ukPhoneNumber
      case "email" =>
        for {
          prefix <- nonEmptyString.map(_.take(50))
          domain <- nonEmptyString.map(_.take(50))
          tld <- Gen.oneOf("com", "co.uk")
        } yield { s"${prefix}@${domain}.${tld}" }
      case "name" =>
        for {
          f <- Gen.forename
          s <- Gen.surname
        } yield s"$f $s"
      case "newsiteref" => nonEmptyString.map(_.take(20))
      case _ => Gen.alphaStr
    }
  })
  implicit val arbSubRequest = Arbitrary(AutoGen[CreateSubscriptionRequest])

  test("∀ CreateSubscriptionRequest: parse(toJson(x)) = x") {
    forAll { r: CreateSubscriptionRequest =>
      Json.toJson(r).as[CreateSubscriptionRequest] should be (r)
    }
  }

  test("∀ CreateSubscriptionRequest: toJson(x) is valid") {
    val validator = JsonSchemaFactory.byDefault.getValidator

    val stream = getClass.getResourceAsStream(
      "/test/des-create-subscription.schema.json")
    val schemaText = scala.io.Source.fromInputStream( stream ).getLines.mkString
    stream.close
    val schema = JsonLoader.fromString(schemaText)
    forAll { r: CreateSubscriptionRequest =>
      val json = JsonLoader.fromString(Json.prettyPrint(Json.toJson(r)))
      val report = validator.validate(schema, json)
      assert(report.isSuccess, report)
    }
  }  
}
