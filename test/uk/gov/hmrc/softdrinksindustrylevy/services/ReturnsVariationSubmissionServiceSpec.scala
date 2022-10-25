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
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.softdrinksindustrylevy.models.{ReturnsVariationRequest, UkAddress}
import uk.gov.hmrc.softdrinksindustrylevy.util.{FakeApplicationSpec, MongoConnectorCustom}

import java.time.Instant
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class ReturnsVariationSubmissionServiceSpec
    extends FakeApplicationSpec with MockitoSugar with BeforeAndAfterEach with ScalaFutures with MongoConnectorCustom {

  implicit val defaultTimeout: FiniteDuration = 5.seconds

  def await[A](future: Future[A])(implicit timeout: Duration): A = Await.result(future, timeout)

  implicit val mc: ReactiveMongoComponent = mock[ReactiveMongoComponent]
  when(mc.mongoConnector).thenReturn(mongoConnector)
  private val service: ReturnsVariationSubmissionService = new ReturnsVariationSubmissionService()
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

  override def beforeEach() {
    service.drop
  }

  "save method" should {
    " successfully save the data within ReturnsVariationWrapper" in {
      await(service.save(returnVariationRequest, sdilRef))
      val storedItem = await(service.find()).head
      storedItem.isInstanceOf[ReturnsVariationWrapper] mustBe true
      storedItem.submission mustBe returnVariationRequest
      storedItem.sdilRef mustBe sdilRef
      storedItem._id.isInstanceOf[BSONObjectID] mustBe true
      storedItem.timestamp.isInstanceOf[Instant] mustBe true
    }
  }

  "get method" should {
    "retrieve None when there are no records in the db" in {
      val storedItem = await(service.get(sdilRef))
      storedItem mustBe None
    }

    "retrieve One record that matches the sdilRef " in {
      val tradingNameToCompare: String = "checkOrderOfThisObjectCreationUsingThis"
      await(service.save(returnVariationRequest, sdilRef))
      await(service.save(returnVariationRequest.copy(orgName = tradingNameToCompare), sdilRef1))
      val storedItem = await(service.get(sdilRef)).get
      storedItem mustBe returnVariationRequest
    }

    "retrieve the first record if more than one records are found (sorted by timestamp descending)" in {
      val tradingNameToCompare: String = "checkOrderOfThisObjectCreationUsingThis"
      await(service.save(returnVariationRequest, sdilRef))
      await(service.save(returnVariationRequest.copy(orgName = tradingNameToCompare), sdilRef))
      val storedItem = await(service.get(sdilRef)).get
      storedItem.orgName mustBe tradingNameToCompare
    }
  }

}
