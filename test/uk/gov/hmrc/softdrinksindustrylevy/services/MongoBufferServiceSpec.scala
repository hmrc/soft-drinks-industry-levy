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
import uk.gov.hmrc.softdrinksindustrylevy.models.{Activity, Address, Contact, InternalActivity, Site, Subscription, UkAddress}
import uk.gov.hmrc.softdrinksindustrylevy.util.{FakeApplicationSpec, MongoConnectorCustom}
import java.time.Instant
import java.time.LocalDate
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class MongoBufferServiceSpec
    extends FakeApplicationSpec with MockitoSugar with BeforeAndAfterEach with ScalaFutures with MongoConnectorCustom {
  implicit val defaultTimeout: FiniteDuration = 5.seconds

  def await[A](future: Future[A])(implicit timeout: Duration): A = Await.result(future, timeout)

  implicit val mc: ReactiveMongoComponent = mock[ReactiveMongoComponent]
  when(mc.mongoConnector).thenReturn(mongoConnector)
  private val service = new MongoBufferService()

  val testUtr = "testUtr"
  val testSdilRef = "someSdilRef"
  val sdilRef = "XCSDIL000000000"
  val sdilRef1 = "XCSDIL000000001"
  val sdilRef2 = "XCSDIL000000002"
  val orgName = "Generic Soft Drinks Company Inc Ltd LLC Intl GB UK"
  val address = UkAddress(List("My House", "My Lane"), "AA111A")

  val subscription = Subscription(
    testUtr,
    Some(sdilRef),
    orgName,
    None,
    address,
    InternalActivity(Map.empty, isLarge = false),
    LocalDate.parse("2018-04-06"),
    List(Site(UkAddress(List("99 Burntscarthgreen", "North West London"), "NW33 9CV"), None, None, None)),
    List(
      Site(UkAddress(List("128 Willowbank Close", "Bristol"), "BS78 5CB"), None, None, None),
      Site(UkAddress(List("17 Trebarthen Terrace", "Northampton"), "NN08 2CC"), None, None, None)
    ),
    contact = Contact(
      Some("Evelyn Hindmarsh"),
      Some("pimirzalvqlsljtiwgIiqzljnKpofqguhwKiwkcbzfoykggiwskbarsbikwwfsgI"),
      "00779 705682",
      "nkzkjldisu@zmzlddpexr.co.uk"
    ),
    None,
    None
  )

  val subscriptionWrapper = SubscriptionWrapper(
    _id = "SubscriptionWrapperId1",
    subscription = subscription,
    formBundleNumber = "formBundle1",
    timestamp = Instant.ofEpochMilli(1666262385) //20 oct 2022 10:39:45
  )

  override def beforeEach() {
    service.drop
  }

  "update status method" should {

    "not set any collection document if record is not found" in {
      await(service.insert(subscriptionWrapper))
      await(service.updateStatus("SubscriptionWrapperId2", "finished"))
      val itemFetched = await(service.find("status" -> "finished")).headOption
      itemFetched mustBe None
    }

    "update one record if found" in {
      await(service.insert(subscriptionWrapper))
      await(service.updateStatus("SubscriptionWrapperId1", "finished"))
      val itemFetched = await(service.findById("SubscriptionWrapperId1")).get
      itemFetched.subscription mustBe subscription
      itemFetched.status mustBe "finished"
    }

  }

  "find overdue method" should {
    "retrieve no records if  created Before criteria is not met" in {
      await(service.insert(subscriptionWrapper)) //created date 20 oct
      val items = await(service.findOverdue(Instant.ofEpochMilli(1666175985))) //find before 19 oct
      items mustBe Seq.empty
    }

    "retrieve no records if  created Before criteria is met but status is not pending" in {
      await(service.insert(subscriptionWrapper.copy(status = "finished", timestamp = Instant.ofEpochMilli(1666089585)))) //created date 18 oct
      val items = await(service.findOverdue(Instant.ofEpochMilli(1666175985))) //find before 19 oct
      items mustBe Seq.empty
    }

    "retrieve one record if  created Before criteria is  met" in {
      await(service.insert(subscriptionWrapper)) //created date of 20 oct
      await(
        service.insert(
          subscriptionWrapper.copy(
            timestamp = Instant.ofEpochMilli(1666089585), //created date 18 oct
            _id = "SubscriptionWrapperId2",
            subscription = subscription.copy(sdilRef = Some(sdilRef1)))))
      val items = await(service.findOverdue(Instant.ofEpochMilli(1666175985))) //find before 19 oct
      items.size mustBe 1
      items.head.subscription.sdilRef.get mustBe sdilRef1
    }

    "retrieve more  record if  created Before criteria is  met" in {
      await(service.insert(subscriptionWrapper)) //created date of 20 oct
      await(
        service.insert(
          subscriptionWrapper.copy(
            timestamp = Instant.ofEpochMilli(1666089585), //created date 18 oct
            _id = "SubscriptionWrapperId2",
            subscription = subscription.copy(sdilRef = Some(sdilRef1)))))
      await(
        service.insert(
          subscriptionWrapper.copy(
            timestamp = Instant.ofEpochMilli(1665484785), //created date 11 oct
            _id = "SubscriptionWrapperId3",
            subscription = subscription.copy(sdilRef = Some(sdilRef2)))))
      val items = await(service.findOverdue(Instant.ofEpochMilli(1666175985))) //find before 19 oct
      items.size mustBe 2
      items.map(_._id) mustBe Seq("SubscriptionWrapperId2", "SubscriptionWrapperId3")
      items.map(_.subscription.sdilRef.get) mustBe Seq(sdilRef1, sdilRef2)
    }

  }

}
