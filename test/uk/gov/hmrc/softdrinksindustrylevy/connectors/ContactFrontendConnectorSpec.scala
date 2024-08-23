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
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.softdrinksindustrylevy.models.connectors._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class ContactFrontendConnectorSpec extends HttpClientV2Helper {

  val connector = app.injector.instanceOf[ContactFrontendConnector]

  implicit val hc: HeaderCarrier = HeaderCarrier()

  implicit lazy val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  "attempted contact form should fail if contact service is not available" in {
    when(requestBuilderExecute[HttpResponse])
      .thenReturn(Future.failed(new Exception("")))

    connector.raiseTicket(sub, "safeid1") onComplete {
      case Success(_) => fail()
      case Failure(_) =>
    }
  }

  "attempted contact form should succeed if contact service is available" in {
    when(requestBuilderExecute[HttpResponse])
      .thenReturn(Future.successful(HttpResponse(200, "")))

    connector.raiseTicket(sub, "test1") onComplete {
      case Success(_) =>
      case Failure(_) => fail()
    }
  }

}
