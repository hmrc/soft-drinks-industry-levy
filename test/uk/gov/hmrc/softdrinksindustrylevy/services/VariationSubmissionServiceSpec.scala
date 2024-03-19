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

import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import uk.gov.hmrc.softdrinksindustrylevy.models.{UkAddress, VariationsRequest}

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class VariationSubmissionServiceSpec
    extends PlaySpec with DefaultPlayMongoRepositorySupport[VariationWrapper] with MockitoSugar with BeforeAndAfterEach
    with ScalaFutures {

  implicit val defaultTimeout: FiniteDuration = 5 seconds

  def await[A](future: Future[A])(implicit timeout: Duration): A = Await.result(future, timeout)

  val repository = new VariationSubmissionService(mongoComponent)

  val address = UkAddress(List("My House", "My Lane"), "AA111A")
  val tradingName = "Generic Soft Drinks Company Inc Ltd LLC Intl GB UK"
  val variationsRequest =
    VariationsRequest(tradingName = Some(tradingName), displayOrgName = tradingName, ppobAddress = address)
  val sdilRef = "XCSDIL000000000"

  "save" should {
    "successfully save the data within a VariationWrapper" in {
      await(repository.save(variationsRequest, sdilRef))

      val storedItem = await(repository.collection.find().toFuture()).head

      storedItem.isInstanceOf[VariationWrapper] mustBe true
      storedItem.submission mustBe variationsRequest
      storedItem.sdilRef mustBe sdilRef
      storedItem.timestamp.isInstanceOf[Instant] mustBe true
    }
  }

  "get" should {
    "retrieve a match of existing variaionRequest when looked up using sdilRef" in {
      await(repository.save(variationsRequest, sdilRef))

      val storedItem = await(repository.get(sdilRef)).get
      storedItem mustBe variationsRequest
    }

    "assure the result is headOption from list when sortedWith _.timestamp isAfter _.timestamp" in {
      val tradingNameToCompare: String = "checkOrderOfThisObjectCreationUsingThis"
      await(repository.save(variationsRequest, sdilRef))
      await(repository.save(variationsRequest.copy(tradingName = Some(tradingNameToCompare)), sdilRef))

      val storedItem = await(repository.get(sdilRef)).get
      storedItem.tradingName.get mustBe tradingNameToCompare
    }
  }
}
