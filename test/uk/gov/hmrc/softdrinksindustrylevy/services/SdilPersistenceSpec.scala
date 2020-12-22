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

package uk.gov.hmrc.softdrinksindustrylevy.services

import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import play.api.libs.json.Json
import sdil.models.{ReturnPeriod, SdilReturn}
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.softdrinksindustrylevy.controllers.validCreateSubscriptionRequest
import uk.gov.hmrc.softdrinksindustrylevy.models.Subscription
import uk.gov.hmrc.softdrinksindustrylevy.models.json.internal.subReads
import uk.gov.hmrc.softdrinksindustrylevy.util.MongoConnectorCustom

import scala.concurrent.ExecutionContext.Implicits.global

class SdilPersistenceSpec extends UnitSpec with BeforeAndAfterAll with BeforeAndAfterEach with MongoConnectorCustom {

  implicit val readsSubscription = subReads
  val service = new SdilMongoPersistence(mongoConnector)

  val sDbReturns = service.returns
  val sDbSubscriptions = service.subscriptions

  val sReturnsMongo = service.returns.returnsMongo
  val sSubscriptionsMongo = service.subscriptions.subscriptionsMongo

  override def beforeEach() {
    sReturnsMongo.drop
    sSubscriptionsMongo.drop
  }

  val utr = "7674173564"

  "SdilMongoPersistence.subscriptions" should {
    val subscription = Json.fromJson[Subscription](validCreateSubscriptionRequest).get

    "insert => successfully insert subscription" in {
      await(sDbSubscriptions.insert(utr, subscription))

      val result = await(sSubscriptionsMongo.find()).toString

      Seq(utr, subscription.orgName, subscription.utr, "Wrapper").foreach(testFor =>
        result.contains(testFor.toString) shouldBe true)
    }

    "insert => allow for duplicate submissions" in {
      await(sDbSubscriptions.insert(utr, subscription))
      await(sDbSubscriptions.insert(utr, subscription))

      val result = await(sSubscriptionsMongo.find())

      result.size shouldBe 2
    }

    "list => find all subscriptions for given utr" in {
      await(sDbSubscriptions.insert(utr, subscription))
      await(sDbSubscriptions.insert(utr, subscription))
      await(sDbSubscriptions.insert(utr, subscription))

      val result = await(sDbSubscriptions.list(utr))
      result.size shouldBe 3
    }

    "list => get 0 results when no results for specified utr" in {
      await(sDbSubscriptions.insert(utr, subscription))

      val result = await(sDbSubscriptions.list("otherUtr"))
      result.size shouldBe 0
    }
  }

  "SdilMongoPersistence.returns" should {
    val returnPeriod = new ReturnPeriod(2018, 1)
    val sdilReturn = SdilReturn((3, 3), (3, 3), Nil, (3, 3), (3, 3), (3, 3), (3, 3), None)

    "update => create if one does not exist successfully saves new record" in {
      await(sDbReturns.update(utr, returnPeriod, sdilReturn))

      val result = await(sReturnsMongo.find()).toString

      Seq(utr, returnPeriod, sdilReturn, "Wrapper").foreach(testFor => result.contains(testFor.toString) shouldBe true)
    }

    "update => successfully updated the record" in {
      await(sDbReturns.update(utr, returnPeriod, sdilReturn))
      await(sDbReturns.update(utr, returnPeriod, sdilReturn))

      val result = await(sReturnsMongo.find())
      result.size shouldBe 1

      Seq(utr, returnPeriod, sdilReturn, "Wrapper").foreach(testFor =>
        result.toString.contains(testFor.toString) shouldBe true)
    }

    "get => successfully get Tuple(sdilReturn, BsonObjectId) when matching utr & returnPeriod" in {
      await(sDbReturns.update(utr, returnPeriod, sdilReturn))

      val result = await(sDbReturns.get(utr, returnPeriod)).get

      result._1 shouldBe sdilReturn
      result._2.isDefined shouldBe true
    }

    "get => if exists get first from collection" in {
      await(sDbReturns.update(utr, returnPeriod, sdilReturn.copy(ownBrand = (123, 123))))
      await(sDbReturns.update(utr, returnPeriod, sdilReturn))

      val result = await(sDbReturns.get(utr, returnPeriod)).get

      result._1 shouldBe sdilReturn
      result._2.isDefined shouldBe true
    }

    "get => None when match not found" in {
      val result = await(sDbReturns.get(utr, returnPeriod))

      result.isDefined shouldBe false
    }

    "list => get all results that match a given UTR but exclusively: (1/returnperiod, returning the latest submitted)" in {
      await(sDbReturns.update(utr, returnPeriod.copy(year = 9999), sdilReturn))
      await(sDbReturns.update(utr, returnPeriod, sdilReturn.copy(ownBrand = (1230000, 123))))
      await(sDbReturns.update(utr, returnPeriod, sdilReturn))

      val result = await(sDbReturns.list(utr))
      result.size shouldBe 2
      result.get(returnPeriod).get.ownBrand shouldBe sdilReturn.ownBrand
    }

    "list => get all" in {
      val result = await(sDbReturns.list(utr))
      result.size shouldBe 0
    }
  }
}
