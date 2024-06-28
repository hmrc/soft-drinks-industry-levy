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
import sdil.models.{ReturnPeriod, ReturnVariationData, SdilReturn}
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import uk.gov.hmrc.softdrinksindustrylevy.models.UkAddress

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class ReturnsAdjustmentSubmissionSpec
    extends PlaySpec with DefaultPlayMongoRepositorySupport[ReturnVariationWrapper] with MockitoSugar
    with BeforeAndAfterEach with ScalaFutures {
  implicit val defaultTimeout: FiniteDuration = 5.seconds

  def await[A](future: Future[A])(implicit timeout: Duration): A = Await.result(future, timeout)

  val repository: ReturnsAdjustmentSubmissionService = new ReturnsAdjustmentSubmissionService(mongoComponent)

  val address = UkAddress(List("My House", "My Lane"), "AA111A")

  val testOriginal = SdilReturn(submittedOn = None)
  val testRevised = SdilReturn(
    ownBrand = (3, 3),
    packLarge = (3, 3),
    packSmall = Nil,
    importSmall = (3, 3),
    importLarge = (3, 3),
    export = (3, 3),
    wastage = (3, 3),
    submittedOn = None
  )

  val returnVariationData =
    ReturnVariationData(
      original = testOriginal,
      revised = testRevised,
      period = ReturnPeriod(2018, 1),
      orgName = "testOrg",
      address = address,
      reason = "",
      repaymentMethod = None
    )
  val sdilRef = "XCSDIL000000000"
  val sdilRef1 = "XCSDIL000000001"

  "save method" should {
    "successfully save the data within ReturnVariationWrapper" in {
      await(repository.save(returnVariationData, sdilRef))
      val storedItem = await(repository.collection.find().toFuture()).head
      storedItem.submission mustBe returnVariationData
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
      await(repository.save(returnVariationData, sdilRef))
      await(repository.save(returnVariationData.copy(orgName = tradingNameToCompare), sdilRef1))
      val storedItem = await(repository.get(sdilRef)).get
      storedItem mustBe returnVariationData
    }

    "retrieve the first record if more than one records are found (sorted by timestamp descending)" in {
      val tradingNameToCompare: String = "checkOrderOfThisObjectCreationUsingThis"
      await(repository.save(returnVariationData, sdilRef))
      await(repository.save(returnVariationData.copy(orgName = tradingNameToCompare), sdilRef))
      val storedItem = await(repository.get(sdilRef)).get
      storedItem.orgName mustBe tradingNameToCompare
    }
  }

}
