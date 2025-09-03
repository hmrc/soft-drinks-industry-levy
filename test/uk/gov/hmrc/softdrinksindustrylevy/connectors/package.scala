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

import java.time.LocalDate

import org.scalacheck._
import uk.gov.hmrc.smartstub._
import uk.gov.hmrc.softdrinksindustrylevy.models.ActivityType.{Copackee, CopackerAll, Imported, ProducedOwnBrand}
import scala.language.implicitConversions

import scala.jdk.CollectionConverters._

package object connectors {

  implicit def addressToSite(ad: Address): Site = Site(ad, None, None, None)

  private val nonEmptyString =
    Gen.alphaStr.flatMap { t =>
      Gen.alphaChar.map(c => s"$c$t")
    }

  val genEmail: Gen[String] = for {
    prefix <- nonEmptyString.map(_.take(10))
    domain <- nonEmptyString.map(_.take(10))
    tld    <- Gen.oneOf("com", "co.uk")
  } yield s"$prefix@$domain.$tld"

  private val genTwoLinesUkAddress = Gen.ukAddress
    .map { stubAddr =>
      UkAddress(stubAddr.init, stubAddr.last)
    }
    .retryUntil(_.lines.forall(_.matches("^[A-Za-z0-9 \\-,.&'\\/]{1,35}$")))

  private val genOneLineUkAddress = Gen.ukAddress
    .map { stubAddr =>
      UkAddress(List(stubAddr.head), stubAddr.last)
    }
    .retryUntil(_.lines.forall(_.matches("^[A-Za-z0-9 \\-,.&'\\/]{1,35}$")))

  val genUkAddress: Gen[Address] = for {
    gen  <- Gen.oneOf(List(genOneLineUkAddress, genTwoLinesUkAddress))
    item <- gen
  } yield item

  // could be part of scalacheck?
  def subset[A <: Enumeration](a: A): Gen[Set[a.Value]] =
    Gen
      .sequence(a.values.toSet.map { (x: a.Value) =>
        Gen.const(x).sometimes
      })
      .map(_.asScala.toSet.flatten)

  val genLitreBands: Gen[LitreBands] = for {
    l <- Gen.choose(0L, 1000000L)
    h <- Gen.choose(0L, 1000000L)
  } yield l -> h

  val genActivityTypes: Gen[Gen[Seq[Option[ActivityType.Value]]]] =
    Gen.oneOf(ActivityType.Copackee, ActivityType.ProducedOwnBrand) map { (x: ActivityType.Value) =>
      Gen.sequence[Seq[Option[ActivityType.Value]], Option[ActivityType.Value]](
        Seq(
          Gen.const(x).sometimes,
          Gen.const(ActivityType.Imported).sometimes,
          Gen.const(ActivityType.CopackerAll).sometimes
        )
      )
    }

  val genActivity: Gen[Activity] = for {
    types <- genActivityTypes flatMap (s =>
               s map { t =>
                 t.flatten
               }
             )
    typeTuples <- Gen.sequence[List[(ActivityType.Value, LitreBands)], (ActivityType.Value, LitreBands)](
                    types.map(typeL => genLitreBands.map(lb => (typeL, lb)))
                  )

    isLarge <- Gen.boolean
  } yield InternalActivity(typeTuples.toMap, isLarge)

  def genIsProducer(isLarge: Boolean) =
    Gen.boolean map { y =>
      y || isLarge
    }

  val genRetrievedActivity: Gen[Activity] =
    for {
      isLarge          <- Gen.boolean
      isProducer       <- genIsProducer(isLarge)
      isContractPacker <- Gen.boolean
      isImporter       <- Gen.boolean
    } yield RetrievedActivity(isProducer, isLarge, isContractPacker, isImporter)

  val genName: Gen[String] = for {
    fname <- Gen.forename()
    sname <- Gen.surname
  } yield s"$fname $sname"

  val genContact: Gen[Contact] = for {
    fname             <- Gen.forename()
    sname             <- Gen.surname
    positionInCompany <- nonEmptyString
    phoneNumber       <- Gen.ukPhoneNumber
    email             <- genEmail
  } yield Contact(Some(s"$fname $sname"), Some(positionInCompany), phoneNumber, email)

  val genSubscription: Gen[Subscription] = for {
    utr             <- Enumerable.instances.utrEnum.gen
    orgName         <- nonEmptyString
    orgType         <- Gen.oneOf("1", "2", "3", "5", "7")
    address         <- genUkAddress
    activity        <- genActivity
    liabilityDate   <- Gen.date
    productionSites <- Gen.listOf(genSite)
    warehouseSites  <- Gen.listOf(genSite)
    contact         <- genContact
  } yield Subscription(
    utr,
    None,
    orgName,
    Some(orgType),
    address,
    activity,
    liabilityDate,
    productionSites,
    warehouseSites,
    contact,
    None
  )

  val genSite: Gen[Site] = for {
    ref         <- Gen.oneOf("a", "b")
    address     <- genUkAddress
    tradingName <- genName
  } yield Site(address, Some(ref), Some(tradingName), None)

  def genRetrievedSubscription: Gen[Subscription] =
    for {
      utr             <- Enumerable.instances.utrEnum.gen
      orgName         <- nonEmptyString
      address         <- genUkAddress
      activity        <- genRetrievedActivity
      liabilityDate   <- Gen.date
      productionSites <- Gen.listOf(genUkAddress map addressToSite)
      warehouseSites  <- Gen.listOf(genUkAddress map addressToSite)
      contact         <- genContact
    } yield Subscription(
      utr,
      None,
      orgName,
      None,
      address,
      activity,
      liabilityDate,
      productionSites,
      warehouseSites,
      contact,
      Some(liabilityDate.plusYears(1))
    )

  val genSdil: Gen[String] =
    for {
      char   <- Gen.alphaUpperChar
      suffix <- pattern"999999"
    } yield s"X${char}SDIL000$suffix"

  val genReturnsSmallProducerVolume: Gen[SmallProducerVolume] =
    for {
      ref    <- genSdil
      litres <- genLitreBands
    } yield SmallProducerVolume(ref, litres)

  val genReturnsPackaging: Gen[ReturnsPackaging] =
    for {
      smallProds <- Gen.listOf(genReturnsSmallProducerVolume)
      litres     <- genLitreBands
    } yield ReturnsPackaging(smallProds, litres)

  val genReturnsImporting: Gen[ReturnsImporting] =
    for {
      smallVols <- genLitreBands
      largeVols <- genLitreBands
    } yield ReturnsImporting(smallVols, largeVols)

  val genReturnsRequest: Gen[ReturnsRequest] =
    for {
      packaged <- genReturnsPackaging.sometimes
      imported <- genReturnsImporting.sometimes
      exported <- genLitreBands.sometimes
      wastage  <- genLitreBands.sometimes
    } yield ReturnsRequest(packaged, imported, exported, wastage)

  val genDisplayDirectDebitResponse: Gen[DisplayDirectDebitResponse] =
    for {
      directDebitMandateFound <- Gen.boolean
    } yield DisplayDirectDebitResponse(directDebitMandateFound)

  implicit val arbSubGet: Arbitrary[Subscription] = Arbitrary(genRetrievedSubscription)
  implicit val arbActivity: Arbitrary[Activity] = Arbitrary(genActivity)
  implicit val arbAddress: Arbitrary[Address] = Arbitrary(genUkAddress)
  implicit val arbContact: Arbitrary[Contact] = Arbitrary(genContact)
  implicit val arbSite: Arbitrary[Site] = Arbitrary(genSite)
  implicit val arbSubRequest: Arbitrary[Subscription] = Arbitrary(genSubscription)
  implicit val arbReturnReq: Arbitrary[ReturnsRequest] = Arbitrary(genReturnsRequest)
  implicit val arbDisplayDirectDebitResponse: Arbitrary[DisplayDirectDebitResponse] = Arbitrary(
    genDisplayDirectDebitResponse
  )

  val sub = Subscription(
    "1234567890",
    Some("1234"),
    "org name",
    None,
    UkAddress(List("line1"), "AA11AA"),
    activity,
    LocalDate.now(),
    List(Site(UkAddress(List("line1"), "AA11AA"), None, None, None)),
    List(Site(UkAddress(List("line1"), "AA11AA"), None, None, None)),
    Contact(None, None, "0843858438", "test@test.com"),
    None,
    None
  )

  def internalActivity(
    produced: LitreBands = zero,
    copackedAll: LitreBands = zero,
    imported: LitreBands = zero,
    copackedByOthers: LitreBands = zero
  ) =
    InternalActivity(
      Map(
        ProducedOwnBrand -> produced,
        CopackerAll      -> copackedAll,
        Imported         -> imported,
        Copackee         -> copackedByOthers
      ),
      false
    )

  lazy val zero: LitreBands = (0, 0)

  lazy val activity = internalActivity(
    produced = (1, 2),
    copackedAll = (3, 4),
    imported = (5, 6)
  )

}
