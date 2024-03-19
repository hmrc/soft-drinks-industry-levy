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

package uk.gov.hmrc.softdrinksindustrylevy.connectors

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.Mode
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.softdrinksindustrylevy.util.FakeApplicationSpec

import scala.concurrent.{ExecutionContext, Future}

class TaxEnrolmentConnectorSpec
    extends FakeApplicationSpec with MockitoSugar with BeforeAndAfterEach with ScalaFutures {

  val mockServicesConfig: ServicesConfig = mock[ServicesConfig]

  val mode = mock[Mode]

  val mockHttpClient = mock[HttpClient]

  val connector = new TaxEnrolmentConnector(mockHttpClient, mode, mockServicesConfig)

  val req = TaxEnrolmentsSubscription(None, "etmp1", "active", None)
  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit lazy val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  override def beforeEach(): Unit =
    reset(mockHttpClient)

  "should get successful response back" in {

    when(mockHttpClient.GET[HttpResponse](any(), any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(HttpResponse(200, Json.toJson(req).toString())))

    connector.getSubscription("1234567890").map { response =>
      response mustBe req
    }
  }

  "should subscribe successfully" in {

    when(mockHttpClient.PUT[JsObject, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
      .thenReturn(Future.successful(HttpResponse(200, Json.toJson(req).toString())))

    connector.subscribe("safe1", "1234").map { res =>
      res.status mustBe 200
    }
  }

  "should handle errors for create subscribtion" in {
    when(mockHttpClient.PUT[JsObject, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
      .thenReturn(Future.successful(HttpResponse(400, "400")))

    connector.subscribe("safe1", "1234").map { res =>
      res.status mustBe 400
    }
  }
}
