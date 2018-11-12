/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.softdrinksindustrylevy.controllers

import java.time.LocalDate

import uk.gov.hmrc.softdrinksindustrylevy.services.{SdilMongoPersistence, SdilPersistence}
import uk.gov.hmrc.softdrinksindustrylevy.util.FakeApplicationSpec
import com.softwaremill.macwire._
import org.mockito.ArgumentMatchers.{eq => matching, _}
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.softdrinksindustrylevy.connectors.DesConnector
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.softdrinksindustrylevy.models.{Contact, RetrievedActivity, Subscription, UkAddress}
import sdil.models._

//import scala.concurrent.ExecutionContext

import scala.concurrent.{ExecutionContext => EC}
import uk.gov.hmrc.softdrinksindustrylevy.config.SdilConfig

import scala.collection.mutable
import scala.concurrent.Future

class ReturnsControllerSpec extends FakeApplicationSpec with MockitoSugar {

  "GET /subscriptions/:idType/:idNumber/year/:year/quarter/:quarter" should {
//    "return 404 if the SDIL reference is not valid" in {
//      val res = testController.checkSmallProducerStatus("sdil", "XXSDIL000112233", 2018, 0)(FakeRequest())
//      status(res) mustBe OK
//      println(contentAsJson(res))
////      (contentAsJson(res) \ "errorCode").as[String] mustBe "INVALID_REFERENCE"
//    }
//
    "return false if the subscription did not exist during the quarter" in {
      when(desConnector.retrieveSubscriptionDetails(
        matching("sdil"),
        matching("XXSDIL000112233")
      )(any(), any())).thenReturn(Future.successful(None))

      val res = testController.checkSmallProducerStatus("sdil", "XXSDIL000112233", 2018, 0)(FakeRequest())
      status(res) mustBe OK
      contentAsJson(res).toString mustBe "false"
    }

    "return false if the subscription is a large producer during the entire quarter" in {
      lazy val largeProducer = subscription.copy(
        activity = RetrievedActivity(isProducer = true, isLarge = true, isContractPacker = false, isImporter = false)
      )

      when(desConnector.retrieveSubscriptionDetails(
        matching("sdil"),
        matching("XXSDIL000112235")
      )(any(), any())).thenReturn(Future.successful(Some(largeProducer)))

      val res = testController.checkSmallProducerStatus("sdil", "XXSDIL000112233", 2018, 0)(FakeRequest())
      status(res) mustBe OK
      contentAsJson(res).toString mustBe "false"
    }

    "return false if the subscription is not a producer during the quarter" in {
      lazy val nonProducer = subscription.copy(
        activity = RetrievedActivity(isProducer = false, isLarge = false, isContractPacker = false, isImporter = true)
      )

      when(desConnector.retrieveSubscriptionDetails(
        matching("sdil"),
        matching("XXSDIL000112236")
      )(any(), any())).thenReturn(Future.successful(Some(nonProducer)))

      val res = testController.checkSmallProducerStatus("sdil", "XXSDIL000112233", 2018, 0)(FakeRequest())
      status(res) mustBe OK
      contentAsJson(res).toString mustBe "false"
    }

    "return true if the subscription is an active small producer during the quarter" in {
      lazy val smallProducer = subscription.copy(
        activity = RetrievedActivity(isProducer = true, isLarge = false, isContractPacker = false, isImporter = true)
      )

      when(desConnector.retrieveSubscriptionDetails(
        matching("sdil"),
        matching("XXSDIL000112237")
      )(any(), any())).thenReturn(Future.successful(Some(smallProducer)))


      when(junkPersistence
        .subscriptions
        .list(smallProducer.utr)(defaultContext))
        .thenReturn(Future.successful(validSmallProducerSubscriptionList))


      val res = testController.checkSmallProducerStatus("sdil", "XXSDIL000112233", 2018, 0)(FakeRequest())
      println(res)
      status(res) mustBe OK
      contentAsJson(res).toString mustBe "true"
    }
  }

  lazy val subscription = Subscription(
    utr = "9876543210",
    sdilRef = None,
    orgName = "Somebody",
    orgType = None,
    address = UkAddress(Nil, "SW1A 1AA"),
    activity = RetrievedActivity(isProducer = false, isLarge = false, isContractPacker = false, isImporter = true),
    liabilityDate = LocalDate.now,
    productionSites = Nil,
    warehouseSites = Nil,
    contact = Contact(None, None, "½", "¾@⅛.com"),
    endDate = Some(LocalDate.now.plusDays(1))
  )

  lazy val validSmallProducerSubscriptionList = List(Subscription(
    utr = "9876543210",
    sdilRef = None,
    orgName = "Somebody",
    orgType = None,
    address = UkAddress(Nil, "SW1A 1AA"),
    activity = RetrievedActivity(isProducer = false, isLarge = false, isContractPacker = false, isImporter = true),
    liabilityDate = LocalDate.now,
    productionSites = Nil,
    warehouseSites = Nil,
    contact = Contact(None, None, "½", "¾@⅛.com"),
    endDate = Some(LocalDate.now.plusDays(1))
  ))


  lazy val authConnector: AuthConnector = {
    val m = mock[AuthConnector]
    when(m.authorise[Unit](any(), any())(any(), any())).thenReturn(Future.successful(()))
    m
  }

//  override def testPersistence.subscriptions.list(key: String)(implicit ec: EC): Future[List[Subscription]] =
//    data += data.get(key).fold(key -> List(value))(x => key -> (value +: x))
//    Future.successful(())
//  }

  lazy val desConnector: DesConnector = mock[DesConnector]
//  lazy val subs = mock[SubsDAO]
//  lazy val persistence: SdilPersistence = mock[SdilPersistence]
//  implicit val subscriptionsCollection: SdilPersistence = mock[SdilPersistence]

//  lazy val foo: testPersistence.SubsDAO[String, Subscription] = testPersistence.subscriptions
//  implicit val junkPersistence: SdilPersistence = testPersistence

  lazy val junkPersistence = new SdilPersistence {
    override def returns: DAO[String, ReturnPeriod, SdilReturn] = ???


    lazy val subscriptions = mock[SubsDAO[String, Subscription]]

    override def subscriptions: SubsDAO[String, Subscription] = new SubsDAO[String, Subscription] {
//      private var data: scala.collection.mutable.Map[String, List[Subscription]] = mutable.Map.empty

      override def insert(key: String, value: Subscription)(implicit ec: EC): Future[Unit] = ???

      override def list(key: String)(implicit ec: EC): Future[List[Subscription]] = {
        Future(List.empty[Subscription])(ec)
      }

    }
  }



  //  implicit val foo: testPersistence.SubsDAO[String, Subscription] = testPersistence.subscriptions
  implicit lazy val config = SdilConfig(None)
  lazy val testController = wire[ReturnsController]
}
