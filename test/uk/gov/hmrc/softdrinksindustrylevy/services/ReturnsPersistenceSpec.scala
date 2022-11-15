/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.libs.json.Json
import sdil.models.{ReturnPeriod, SdilReturn, SmallProducer}
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class ReturnsPersistenceSpec
    extends PlaySpec with DefaultPlayMongoRepositorySupport[ReturnsWrapper] with MockitoSugar with BeforeAndAfterAll
    with ScalaCheckPropertyChecks {

  implicit val defaultTimeout: FiniteDuration = 5 seconds
  def await[A](future: Future[A])(implicit timeout: Duration): A = Await.result(future, timeout)

  val repository = new ReturnsPersistence(mongoComponent)

  val sReturnsMongo = repository.collection
  implicit val nilFormat = Json.format[Nil.type]
  implicit val listFormat = Json.format[List[SmallProducer]]
  implicit val sdilFormat = Json.format[SdilReturn]
  val rFormat = Json.format[ReturnsWrapper]

  val utr = "7674173564"

  "ReturnsPersistence.returns" should {
    val returnPeriod = new ReturnPeriod(2018, 1)
    val sdilReturn = SdilReturn((3, 3), (3, 3), Nil, (3, 3), (3, 3), (3, 3), (3, 3), None)

    "update => create if one does not exist successfully saves new record" in {
      await(repository.update(utr, returnPeriod, sdilReturn))

      val result = await(sReturnsMongo.find().toFuture()).toString

      Seq(utr, returnPeriod, sdilReturn, "Wrapper").foreach(testFor => result.contains(testFor.toString) mustBe true)
    }

    "update => successfully updated the record" in {
      await(repository.update(utr, returnPeriod, sdilReturn))
      await(repository.update(utr, returnPeriod, sdilReturn))

      val result = await(sReturnsMongo.find().toFuture())
      result.size mustBe 1

      Seq(utr, returnPeriod, sdilReturn, "Wrapper").foreach(testFor =>
        result.toString.contains(testFor.toString) mustBe true)
    }

    "get => successfully get Tuple(sdilReturn, BsonObjectId) when matching utr & returnPeriod" in {
      await(repository.update(utr, returnPeriod, sdilReturn))

      val result: SdilReturn = await(repository.get(utr, returnPeriod)).get

      result mustBe sdilReturn

    }

    "get => if exists get first from collection" in {
      await(repository.update(utr, returnPeriod, sdilReturn.copy(ownBrand = (123, 123))))
      await(repository.update(utr, returnPeriod, sdilReturn))

      val result = await(repository.get(utr, returnPeriod)).get

      result mustBe sdilReturn
    }

    "get => None when match not found" in {
      val result = await(repository.get(utr, returnPeriod))

      result.isDefined mustBe false
    }

    "list => get all results that match a given UTR but exclusively: (1/returnperiod, returning the latest submitted)" in {
      await(repository.update(utr, returnPeriod.copy(year = 9999), sdilReturn))
      await(repository.update(utr, returnPeriod, sdilReturn.copy(ownBrand = (1230000, 123))))
      await(repository.update(utr, returnPeriod, sdilReturn))

      val result = await(repository.list(utr))
      result.size mustBe 2
      result.get(returnPeriod).get.ownBrand mustBe sdilReturn.ownBrand
    }

    "list => get all" in {
      val result = await(repository.list(utr))
      result.size mustBe 0
    }
  }
}
