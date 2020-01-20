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

import java.time.Instant

import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.softdrinksindustrylevy.models.{UkAddress, VariationsRequest}
import uk.gov.hmrc.softdrinksindustrylevy.util.MongoConnectorCustom

import scala.concurrent.ExecutionContext.Implicits.global

class VariationSubmissionServiceSpec
    extends UnitSpec with BeforeAndAfterAll with BeforeAndAfterEach with MongoConnectorCustom {

  private val service = new VariationSubmissionService

  override def beforeEach() {
    service.drop
  }

  val address = UkAddress(List("My House", "My Lane"), "AA111A")
  val tradingName = "Generic Soft Drinks Company Inc Ltd LLC Intl GB UK"
  val variationsRequest =
    VariationsRequest(tradingName = Some(tradingName), displayOrgName = tradingName, ppobAddress = address)
  val sdilRef = "XCSDIL000000000"

  "save" should {
    "successfully save the data within a VariationWrapper" in {
      await(service.save(variationsRequest, sdilRef))

      val storedItem = await(service.find()).head

      storedItem.isInstanceOf[VariationWrapper] shouldBe true
      storedItem.submission shouldBe variationsRequest
      storedItem.sdilRef shouldBe sdilRef
      storedItem._id.isInstanceOf[BSONObjectID] shouldBe true
      storedItem.timestamp.isInstanceOf[Instant] shouldBe true
    }
  }

  "get" should {
    "retrieve a match of existing variaionRequest when looked up using sdilRef" in {
      await(service.save(variationsRequest, sdilRef))

      val storedItem = await(service.get(sdilRef)).get
      storedItem shouldBe variationsRequest
    }

    "assure the result is headOption from list when sortedWith _.timestamp isAfter _.timestamp" in {
      val tradingNameToCompare: String = "checkOrderOfThisObjectCreationUsingThis"
      await(service.save(variationsRequest, sdilRef))
      await(service.save(variationsRequest.copy(tradingName = Some(tradingNameToCompare)), sdilRef))

      val storedItem = await(service.get(sdilRef)).get
      storedItem.tradingName.get shouldBe tradingNameToCompare
    }
  }
}
