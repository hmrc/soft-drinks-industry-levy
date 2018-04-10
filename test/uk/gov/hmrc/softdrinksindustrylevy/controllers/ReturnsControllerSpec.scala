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
import uk.gov.hmrc.softdrinksindustrylevy.models.{Contact, RetrievedActivity, Subscription, UkAddress}

import scala.concurrent.Future

class ReturnsControllerSpec extends FakeApplicationSpec with MockitoSugar {

  "GET /small-producer" should {
    "return Bad Request and a INVALID_REFERENCE error code if the SDIL reference is not valid" in {
      val res = testController.validateSmallProducer("Definitely not a valid SDIL number")(FakeRequest())
      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "errorCode").as[String] mustBe "INVALID_REFERENCE"
    }

    "return Not Found if the subscription does not exist" in {
      when(desConnector.retrieveSubscriptionDetails(
        matching("sdil"),
        matching("XXSDIL000112233")
      )(any(), any())).thenReturn(Future.successful(None))

      val res = testController.validateSmallProducer("XXSDIL000112233")(FakeRequest())
      status(res) mustBe NOT_FOUND
    }

    "return Not Found and a DEREGISTERED error code if the subscription has been deactivated" in {
      lazy val deregistered = subscription.copy(endDate = Some(LocalDate.now.minusDays(1)))

      when(desConnector.retrieveSubscriptionDetails(
        matching("sdil"),
        matching("XXSDIL000112234")
      )(any(), any())).thenReturn(Future.successful(Some(deregistered)))

      val res = testController.validateSmallProducer("XXSDIL000112234")(FakeRequest())
      status(res) mustBe NOT_FOUND
      (contentAsJson(res) \ "errorCode").as[String] mustBe "DEREGISTERED"
    }

    "return Not Found and a NOT_SMALL_PRODUCER error code if the subscription is a large producer" in {
      lazy val largeProducer = subscription.copy(
        activity = RetrievedActivity(isProducer = true, isLarge = true, isContractPacker = false, isImporter = false)
      )

      when(desConnector.retrieveSubscriptionDetails(
        matching("sdil"),
        matching("XXSDIL000112235")
      )(any(), any())).thenReturn(Future.successful(Some(largeProducer)))

      val res = testController.validateSmallProducer("XXSDIL000112235")(FakeRequest())
      status(res) mustBe NOT_FOUND
      (contentAsJson(res) \ "errorCode").as[String] mustBe "NOT_SMALL_PRODUCER"
    }

    "return Not Found and a NOT_SMALL_PRODUCER error code if the subscription is not a producer" in {
      lazy val nonProducer = subscription.copy(
        activity = RetrievedActivity(isProducer = false, isLarge = false, isContractPacker = false, isImporter = true)
      )

      when(desConnector.retrieveSubscriptionDetails(
        matching("sdil"),
        matching("XXSDIL000112236")
      )(any(), any())).thenReturn(Future.successful(Some(nonProducer)))

      val res = testController.validateSmallProducer("XXSDIL000112236")(FakeRequest())
      status(res) mustBe NOT_FOUND
      (contentAsJson(res) \ "errorCode").as[String] mustBe "NOT_SMALL_PRODUCER"
    }

    "return Ok if the subscription is an active small producer" in {
      lazy val smallProducer = subscription.copy(
        activity = RetrievedActivity(isProducer = true, isLarge = false, isContractPacker = false, isImporter = true)
      )

      when(desConnector.retrieveSubscriptionDetails(
        matching("sdil"),
        matching("XXSDIL000112237")
      )(any(), any())).thenReturn(Future.successful(Some(smallProducer)))

      val res = testController.validateSmallProducer("XXSDIL000112237")(FakeRequest())
      status(res) mustBe OK
    }
  }

  lazy val subscription = Subscription(
    utr = "9876543210",
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

  lazy val authConnector: AuthConnector = {
    val m = mock[AuthConnector]
    when(m.authorise[Unit](any(), any())(any(), any())).thenReturn(Future.successful(()))
    m
  }

  lazy val desConnector = mock[DesConnector]

  lazy val testController = wire[ReturnsController]
}
