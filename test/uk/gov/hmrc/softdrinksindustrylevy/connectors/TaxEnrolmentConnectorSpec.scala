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

import org.mockito.Mockito.when
import org.scalatest.RecoverMethods.recoverToExceptionIf
import play.api.Mode
import play.api.libs.json.Json
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpResponse, NotFoundException, UnauthorizedException}
import uk.gov.hmrc.softdrinksindustrylevy.models.TaxEnrolments.TaxEnrolmentsSubscription

import scala.concurrent.{ExecutionContext, Future}

class TaxEnrolmentConnectorSpec extends HttpClientV2Helper {

  val mode = mock[Mode]

  val connector = new TaxEnrolmentConnector(mockHttpClient, mode, mockServicesConfig)

  val req = TaxEnrolmentsSubscription(None, "etmp1", "active", None)
  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit lazy val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  "should get successful response back" in {

    when(requestBuilderExecute[HttpResponse])
      .thenReturn(Future.successful(HttpResponse(200, Json.toJson(req).toString())))

    connector.getSubscription("1234567890").map { response =>
      response mustBe req
    }
  }

  "should subscribe successfully" in {

    when(requestBuilderExecute[HttpResponse])
      .thenReturn(Future.successful(HttpResponse(200, Json.toJson(req).toString())))

    connector.subscribe("safe1", "1234").map { res =>
      res.status mustBe 200
    }
  }

  "should handle errors for create subscribtion" in {
    when(requestBuilderExecute[HttpResponse])
      .thenReturn(Future.successful(HttpResponse(400, "400")))

    connector.subscribe("safe1", "1234").map { res =>
      res.status mustBe 400
    }
  }
  "should handle unauthorized error for subscription" in {
    when(requestBuilderExecute[HttpResponse])
      .thenReturn(Future.failed(new UnauthorizedException("Unauthorized")))

    recoverToExceptionIf[UnauthorizedException] {
      connector.subscribe("safe1", "1234")
    }.map { ex =>
      ex.getMessage mustBe "Unauthorized"
    }
  }
  "should handle bad request error for subscription" in {
    when(requestBuilderExecute[HttpResponse])
      .thenReturn(Future.failed(new BadRequestException("Bad Request")))

    recoverToExceptionIf[BadRequestException] {
      connector.subscribe("safe1", "1234")
    }.map { ex =>
      ex.getMessage mustBe "Bad Request"
    }
  }

  "should handle errors for get subscription" in {
    when(requestBuilderExecute[TaxEnrolmentsSubscription])
      .thenReturn(Future.failed(new NotFoundException("Not Found")))

    recoverToExceptionIf[NotFoundException] {
      connector.getSubscription("1234567890")
    }.map { ex =>
      ex.getMessage mustBe "Not Found"
    }
  }
}
