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

import org.mockito.Mockito.when
import org.mongodb.scala.MongoDatabase
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import uk.gov.hmrc.softdrinksindustrylevy.models.{ReturnsVariationRequest, UkAddress}
import uk.gov.hmrc.softdrinksindustrylevy.util.FakeApplicationSpec

import java.time.Instant
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class ReturnsVariationSubmissionServiceSpec
    extends PlaySpec with DefaultPlayMongoRepositorySupport[ReturnsVariationWrapper] with MockitoSugar
    with BeforeAndAfterEach with ScalaFutures {

  implicit val defaultTimeout: FiniteDuration = 5.seconds

  def await[A](future: Future[A])(implicit timeout: Duration): A = Await.result(future, timeout)

  val repository: ReturnsVariationSubmissionService = new ReturnsVariationSubmissionService(mongoComponent)
  val address = UkAddress(List("My House", "My Lane"), "AA111A")
  val tradingName = "Generic Soft Drinks Company Inc Ltd LLC Intl GB UK"
  val returnVariationRequest =
    ReturnsVariationRequest(
      orgName = tradingName,
      ppobAddress = address,
      importer = (false, (0, 0)),
      packer = (false, (0, 0)),
      warehouses = Nil,
      packingSites = Nil,
      phoneNumber = "",
      email = "email",
      taxEstimation = BigDecimal("1.1")
    )
  val sdilRef = "XCSDIL000000000"
  val sdilRef1 = "XCSDIL000000001"

  "save method" should {
    " successfully save the data within ReturnsVariationWrapper" in {
      await(repository.save(returnVariationRequest, sdilRef))
      val storedItem = await(repository.collection.find().toFuture()).head
      storedItem.isInstanceOf[ReturnsVariationWrapper] mustBe true
      storedItem.submission mustBe returnVariationRequest
      storedItem.sdilRef mustBe sdilRef
      storedItem.timestamp.isInstanceOf[Instant] mustBe true
    }
  }

  "get method" should {
    "retrieve None when there are no records in the db" in {
      val storedItem = await(repository.get(sdilRef))
      storedItem mustBe None
    }

    "retrieve One record that matches the sdilRef " in {
      val tradingNameToCompare: String = "checkOrderOfThisObjectCreationUsingThis"
      await(repository.save(returnVariationRequest, sdilRef))
      await(repository.save(returnVariationRequest.copy(orgName = tradingNameToCompare), sdilRef1))
      val storedItem = await(repository.get(sdilRef)).get
      storedItem mustBe returnVariationRequest
    }

    "retrieve the first record if more than one records are found (sorted by timestamp descending)" in {
      val tradingNameToCompare: String = "checkOrderOfThisObjectCreationUsingThis"
      await(repository.save(returnVariationRequest, sdilRef))
      await(repository.save(returnVariationRequest.copy(orgName = tradingNameToCompare), sdilRef))
      val storedItem = await(repository.get(sdilRef)).get
      storedItem.orgName mustBe tradingNameToCompare
    }
  }

}
