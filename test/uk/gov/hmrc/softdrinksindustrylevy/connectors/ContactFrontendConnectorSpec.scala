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

package uk.gov.hmrc.softdrinksindustrylevy.connectors

import java.time.Instant
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.libs.json.JsValue
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import uk.gov.hmrc.softdrinksindustrylevy.models.connectors._
import uk.gov.hmrc.softdrinksindustrylevy.util.FakeApplicationSpec

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class ContactFrontendConnectorSpec extends FakeApplicationSpec {

  val connector = app.injector.instanceOf[ContactFrontendConnector]

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val mockHttpClient = mock[HttpClient]

  implicit lazy val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  "attempted contact form should fail if contact service is not available" in {
    when(mockHttpClient.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
      .thenReturn(Future.failed(new Exception("")))

    connector.raiseTicket(sub, "safeid1", Instant.now()) onComplete {
      case Success(_) => fail
      case Failure(_) =>
    }
  }

  "attempted contact form should succeed if contact service is available" in {
    when(mockHttpClient.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
      .thenReturn(Future.successful(HttpResponse(200, "")))

    connector.raiseTicket(sub, "test1", Instant.now()) onComplete {
      case Success(_) =>
      case Failure(_) => fail
    }
  }

}
