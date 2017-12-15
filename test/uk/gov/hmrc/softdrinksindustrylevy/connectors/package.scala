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

package uk.gov.hmrc.softdrinksindustrylevy.models

import org.scalacheck._
import uk.gov.hmrc.smartstub._
import uk.gov.hmrc.smartstub.AutoGen.{providerStringNamed => _,_}

import scala.collection.JavaConverters._

package object gen {

  private val nonEmptyString =
    Gen.alphaStr.flatMap{t => Gen.alphaChar.map(_ + t)}

  val genEmail: Gen[String] = for {
    prefix <- nonEmptyString.map(_.take(10))
    domain <- nonEmptyString.map(_.take(10))
    tld <- Gen.oneOf("com", "co.uk")
  } yield { s"${prefix}@${domain}.${tld}" }

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

//  val genProducerOrCopackee: Gen[Set[ActivityType.Value]] = {
//    Gen.oneOf(ActivityType.Copackee, ActivityType.ProducedOwnBrand) map {
//      act => Gen.sequence(Set(act, ActivityType.CopackerAll, ActivityType.Imported)).map{
//        x: ActivityType.Value => Gen.const(x).sometimes
//      }
//    }
//  }

  val genActivity: Gen[Activity] = for {
    types <- subset(ActivityType)
    // also Copackee and ProducedOwnBrand are mutually exclusive

    // TODO: Rewrite this shamefull mess
    // indeed... the problem here is that some of the numbers are subtracted and we're going to need them to be smaller
    typeTuples <- Gen.sequence(types.map{
      typeL => genLitreBands.flatMap{ typeL -> _ } })
  } yield {
    InternalActivity(typeTuples.asScala.toMap)
  }

  val genRetrievedActivity: Gen[Activity] = for {
    isProducer <- Gen.boolean
    isLarge <- Gen.boolean
    isContractPacker <- Gen.boolean
    isImporter <- Gen.boolean
  } yield RetrievedActivity(isProducer, isLarge, isContractPacker, isImporter)

  val genName: Gen[String] = for {
    fname <- Gen.forename
    sname <- Gen.surname
  } yield s"$fname $sname"

  val genContact: Gen[Contact] = for {
    fname <- Gen.forename
    sname <- Gen.surname
    positionInCompany <- nonEmptyString
    phoneNumber <- Gen.ukPhoneNumber
    email <- genEmail
  } yield
      Contact(Some(s"$fname $sname"), Some(positionInCompany), phoneNumber, email)

  val genSubscription: Gen[Subscription] = for {
    utr <- Enumerable.instances.utrEnum.gen
    orgName <- nonEmptyString
    address <- genUkAddress
    activity <- genActivity
    liabilityDate <- Gen.date
    productionSites <- Gen.listOf(genUkAddress map addressToSite)
    warehouseSites <- Gen.listOf(genUkAddress map addressToSite)
    contact <- genContact
  } yield Subscription(utr, orgName, address, activity, liabilityDate,
    productionSites, warehouseSites, contact)

  val genSite: Gen[Site] = for {
    ref <- nonEmptyString
    address <- genUkAddress
  } yield Site(address, ref)

  def genRetrievedSubscription: Gen[Subscription] = {
  for {
    utr <- Enumerable.instances.utrEnum.gen
    orgName <- nonEmptyString
    address <- genUkAddress
    activity <- genRetrievedActivity
    liabilityDate <- Gen.date
    productionSites <- Gen.listOf(genUkAddress map addressToSite)
    warehouseSites <- Gen.listOf(genUkAddress map addressToSite)
    contact <- genContact
  } yield Subscription(utr, orgName, address, activity, liabilityDate,
    productionSites, warehouseSites, contact)
  }

  implicit val arbSubGet = Arbitrary(genRetrievedSubscription)
  implicit val arbActivity = Arbitrary(genActivity)
  implicit val arbAddress = Arbitrary(
    genUkAddress.retryUntil(_.lines.forall(_.length <= 35))
  )
  implicit val arbContact = Arbitrary(genContact)
  implicit val arbSubRequest = Arbitrary(genSubscription)
  implicit val arbSite = Arbitrary(genSite)

}
