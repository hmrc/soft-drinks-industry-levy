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

import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{Json, Reads}
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import uk.gov.hmrc.softdrinksindustrylevy.controllers.validCreateSubscriptionRequest
import uk.gov.hmrc.softdrinksindustrylevy.models.Subscription
import uk.gov.hmrc.softdrinksindustrylevy.models.json.internal.subReads

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class SdilPersistenceSpec
    extends PlaySpec with DefaultPlayMongoRepositorySupport[SubscriptionWrap] with MockitoSugar with BeforeAndAfterAll
    with ScalaCheckPropertyChecks {

  implicit val defaultTimeout: FiniteDuration = 5 seconds

  def await[A](future: Future[A])(implicit timeout: Duration): A = Await.result(future, timeout)

  implicit val readsSubscription: Reads[Subscription] = subReads

  val repository = new SdilMongoPersistence(mongoComponent)

  val sSubscriptionsMongo = repository.collection

  val utr = "7674173564"

  "SdilMongoPersistence.subscriptions" should {
    val subscription = Json.fromJson[Subscription](validCreateSubscriptionRequest).get

    "insert => successfully insert subscription" in {
      await(repository.insert(utr, subscription))

      val result = await(sSubscriptionsMongo.find().toFuture()).toString

      Seq(utr, subscription.orgName, subscription.utr, "Wrap").foreach(testFor =>
        result.contains(testFor.toString) mustBe true
      )
    }

    "insert => allow for duplicate submissions" in {
      await(repository.insert(utr, subscription))
      await(repository.insert(utr, subscription))

      val result = await(sSubscriptionsMongo.find().toFuture())

      result.size mustBe 2
    }

    "list => find all subscriptions for given utr" in {
      await(repository.insert(utr, subscription))
      await(repository.insert(utr, subscription))
      await(repository.insert(utr, subscription))

      val result = await(repository.list(utr))
      result.size mustBe 3
    }

    "list => get 0 results when no results for specified utr" in {
      await(repository.insert(utr, subscription))

      val result = await(repository.list("otherUtr"))
      result.size mustBe 0
    }
  }

}
