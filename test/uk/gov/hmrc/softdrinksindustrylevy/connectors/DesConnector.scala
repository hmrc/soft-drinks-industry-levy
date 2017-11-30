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
import scala.collection.JavaConverters._

class DesConnectorSpec extends FunSuite with PropertyChecks with Matchers {

  private val nonEmptyString =
    Gen.alphaStr.flatMap{t => Gen.alphaChar.map(_ + t)}

  val genEmail: Gen[String] = for {
    prefix <- nonEmptyString.map(_.take(50))
    domain <- nonEmptyString.map(_.take(50))
    tld <- Gen.oneOf("com", "co.uk")
  } yield { s"${prefix}@${domain}.${tld}" }

  def genAddress = genUkAddress

  val genUkAddress: Gen[Address] = Gen.ukAddress.map {
    stubAddr => UkAddress(stubAddr.init, stubAddr.last)
  }

  // could be part of scalacheck?
  def subset[A <: Enumeration](a: A): Gen[Set[A#Value]] = {
    Gen.sequence(a.values.toSet.map{
      x: A#Value => Gen.const(x).sometimes
    }).map(_.asScala.toSet.flatten)
  }

  val genLitreBands: Gen[LitreBands] = for {
    l <- Gen.choose(0, 1000000).sometimes.map{_.getOrElse(0).toLong}
    h <- Gen.choose(0, 1000000).sometimes.map{_.getOrElse(0).toLong}
  } yield ( (l,h) )

  val genActivity: Gen[Activity] = for {
    types <- subset(ActivityType)

    // TODO: Rewrite this shamefull mess
    typeTuples <- Gen.sequence(types.map{
      typeL => genLitreBands.flatMap{ typeL -> _ } })
  } yield {
    typeTuples.asScala.toMap
  }

  val genContact: Gen[Contact] = for {
    fname <- Gen.forename
    sname <- Gen.surname
    positionInCompany <- nonEmptyString
    phoneNumber <- Gen.ukPhoneNumber
    email <- genEmail
  } yield Contact(fname, sname, positionInCompany, email)

  val genSubscription: Gen[Subscription] = for {
    utr <- Enumerable.instances.utrEnum.gen
    orgName <- Gen.alphaStr
    address <- genUkAddress
    activity <- genActivity
    liabilityDate <- Gen.date
    productionSites <- Gen.listOf(genUkAddress map addressToSite)
    warehouseSites <- Gen.listOf(genUkAddress map addressToSite)
    contact <- genContact
  } yield Subscription(utr, orgName, address, activity, liabilityDate,
    productionSites, warehouseSites, contact)

  implicit val arbActivity = Arbitrary(genActivity)
  test("∀ Activity: parse(toJson(x)) = x") {
    forAll { r: Activity =>
      Json.toJson(r).as[Activity] should be (r)
    }
  }

  implicit val arbAddress = Arbitrary(genAddress)
  test("∀ UkAddress: parse(toJson(x)) = x") {
    forAll { r: Address =>
      Json.toJson(r).as[Address] should be (r)
    }
  }

  implicit val arbContact = Arbitrary(genContact)      
  test("∀ Contact: parse(toJson(x)) = x") {
    forAll { r: Contact =>
      Json.toJson(r).as[Contact] should be (r)
    }
  }  

  implicit val arbSubRequest = Arbitrary(genSubscription)  
  test("∀ Subscription: parse(toJson(x)) = x") {
    forAll { r: Subscription =>
      Json.toJson(r).as[Subscription] should be (r)
    }
  }

  test("∀ Subscription: toJson(x) is valid") {
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
