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

package uk.gov.hmrc.softdrinksindustrylevy.services

import org.mongodb.scala.model.{IndexModel, IndexOptions, Indexes}
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{Json, Reads}
import sdil.models.{ReturnPeriod, SdilReturn, SmallProducer}
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import uk.gov.hmrc.softdrinksindustrylevy.controllers.validCreateSubscriptionRequest
import uk.gov.hmrc.softdrinksindustrylevy.models.Subscription
import uk.gov.hmrc.softdrinksindustrylevy.models.json.internal.subReads

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import org.mongodb.scala.ObservableFuture
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

class SdilPersistenceSpec
    extends PlaySpec with DefaultPlayMongoRepositorySupport[SubscriptionWrap] with MockitoSugar with BeforeAndAfterAll
    with ScalaCheckPropertyChecks {

  implicit val defaultTimeout: FiniteDuration = 5 seconds

  def await[A](future: Future[A])(implicit timeout: Duration): A = Await.result(future, timeout)

  implicit val readsSubscription: Reads[Subscription] = subReads

  override protected val repository: PlayMongoRepository[SubscriptionWrap] = new SdilMongoPersistence(mongoComponent)
  val service = new SdilMongoPersistence(mongoComponent)

  val sSubscriptionsMongo = repository.collection

  val returnsPersistenceRepository = new ReturnsPersistence(mongoComponent)

  val returnsMongo = returnsPersistenceRepository.collection

  val utr = "7674173564"
  val period = ReturnPeriod(2023, 1)

  val sampleReturn = SdilReturn(
    ownBrand = (1L, 2L),
    packLarge = (3L, 4L),
    packSmall = List.empty[SmallProducer],
    importSmall = (5L, 6L),
    importLarge = (7L, 8L),
    `export` = (9L, 10L),
    wastage = (11L, 12L),
    submittedOn = None
  )

  "SdilMongoPersistence.subscriptions" should {
    val subscription = Json.fromJson[Subscription](validCreateSubscriptionRequest).get

    "insert => successfully insert subscription" in {
      await(service.insert(utr, subscription))

      val result = await(sSubscriptionsMongo.find().toFuture()).toString

      Seq(utr, subscription.orgName, subscription.utr, "Wrap").foreach(testFor =>
        result.contains(testFor.toString) mustBe true
      )
    }

    "insert => allow for duplicate submissions" in {
      await(service.insert(utr, subscription))
      await(service.insert(utr, subscription))

      val result = await(sSubscriptionsMongo.find().toFuture())

      result.size mustBe 2
    }

    "list => find all subscriptions for given utr" in {
      await(service.insert(utr, subscription))
      await(service.insert(utr, subscription))
      await(service.insert(utr, subscription))

      val result = await(service.list(utr))
      result.size mustBe 3
    }

    "list => get 0 results when no results for specified utr" in {
      await(service.insert(utr, subscription))

      val result = await(service.list("otherUtr"))
      result.size mustBe 0
    }
  }

  "ReturnsPersistence" should {

    def createIndexes(): Unit =
      await(
        returnsMongo
          .createIndexes(
            Seq(
              IndexModel(Indexes.ascending("utr"), IndexOptions().name("utrIdx")),
              IndexModel(Indexes.descending("period.year"), IndexOptions().name("periodYearIdx")),
              IndexModel(Indexes.descending("period.quarter"), IndexOptions().name("periodQuarterIdx"))
            )
          )
          .toFuture()
      )

    "update => successfully insert or update return" in {
      createIndexes()
      await(returnsPersistenceRepository.update(utr, period, sampleReturn))

      val result = await(returnsMongo.find().toFuture())

      result.size mustBe 1
      result.head.utr mustBe utr
      result.head.period mustBe period
      result.head.sdilReturn mustBe sampleReturn
    }

    "update => upsert behavior with multiple calls" in {
      createIndexes()

      val updatedReturn = sampleReturn.copy(ownBrand = (100L, 200L))

      await(returnsPersistenceRepository.update(utr, period, sampleReturn))
      await(returnsPersistenceRepository.update(utr, period, updatedReturn))

      val result = await(returnsMongo.find().toFuture())

      result.size mustBe 1
      result.head.sdilReturn mustBe updatedReturn
    }

    "get => successfully retrieve a return" in {
      createIndexes()
      await(returnsPersistenceRepository.update(utr, period, sampleReturn))

      val result = await(returnsPersistenceRepository.get(utr, period))

      result mustBe Some(sampleReturn)
    }

    "get => return None for a non-existent return" in {
      val result = await(returnsPersistenceRepository.get(utr, period))

      result mustBe None
    }

    "list => retrieve all returns for a given utr" in {
      createIndexes()

      val otherPeriod = ReturnPeriod(2023, 2)
      val anotherReturn = SdilReturn(
        ownBrand = (3L, 4L),
        packLarge = (5L, 6L),
        packSmall = List.empty[SmallProducer],
        importSmall = (0L, 0L),
        importLarge = (0L, 0L),
        `export` = (0L, 0L),
        wastage = (0L, 0L),
        submittedOn = Some(LocalDateTime.now())
      )

      await(returnsPersistenceRepository.update(utr, period, sampleReturn))
      await(returnsPersistenceRepository.update(utr, otherPeriod, anotherReturn))

      val result = await(returnsPersistenceRepository.list(utr))

      result.size mustBe 2
      result(period) mustBe sampleReturn
      result(otherPeriod) mustBe anotherReturn
    }

    "list => return an empty map for no returns with a given utr" in {
      val result = await(service.list("nonexistentUtr"))

      result mustBe empty
    }

    "listVariable => retrieve returns within the last 4 years" in {
      createIndexes()

      val recentPeriod = ReturnPeriod(LocalDate.now.getYear, (LocalDate.now.getMonthValue - 1) / 3)
      val oldPeriod = ReturnPeriod(LocalDate.now.getYear - 5, 1)
      val recentReturn = SdilReturn(
        ownBrand = (10L, 20L),
        packLarge = (30L, 40L),
        packSmall = List.empty[SmallProducer],
        importSmall = (0L, 0L),
        importLarge = (0L, 0L),
        `export` = (0L, 0L),
        wastage = (0L, 0L),
        submittedOn = Some(LocalDateTime.now())
      )
      val oldReturn = SdilReturn(
        ownBrand = (50L, 60L),
        packLarge = (70L, 80L),
        packSmall = List.empty[SmallProducer],
        importSmall = (0L, 0L),
        importLarge = (0L, 0L),
        `export` = (0L, 0L),
        wastage = (0L, 0L),
        submittedOn = Some(LocalDateTime.now().minusYears(5))
      )

      await(returnsPersistenceRepository.update(utr, recentPeriod, recentReturn))
      await(returnsPersistenceRepository.update(utr, oldPeriod, oldReturn))

      val result = await(returnsPersistenceRepository.listVariable(utr))

      result.size mustBe 1
      result(recentPeriod) mustBe recentReturn
    }

    "dropCollection => clear all records" in {
      createIndexes()

      await(returnsPersistenceRepository.update(utr, period, sampleReturn))

      await(returnsPersistenceRepository.dropCollection)

      val result = await(returnsMongo.find().toFuture())

      result mustBe empty
    }
  }

}
