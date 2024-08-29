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
import org.mockito.Mockito.when
import play.api.Mode
import play.api.libs.json.JsValue
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.softdrinksindustrylevy.util.FakeApplicationSpec

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class EmailConnectorSpec extends HttpClientV2Helper {

  val mode = mock[Mode]

  val connector = app.injector.instanceOf[EmailConnector]

  implicit val hc: HeaderCarrier = HeaderCarrier()

  implicit lazy val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  "attempted email should succeed" in {
    when(requestBuilderExecute[HttpResponse])
      .thenReturn(Future.successful(HttpResponse(200, "")))

    connector.sendConfirmationEmail("test", "test", "dfg") onComplete {

      case Success(_) =>
      case Failure(_) => fail()

    }
  }

  "attempted email should fail when the response from email service is a failure" in {
    when(requestBuilderExecute[HttpResponse])
      .thenReturn(Future.failed(new Exception("")))
    connector.sendConfirmationEmail("test", "test", "dfg") onComplete {
      case Success(_) => fail()
      case Failure(_) => // do nothing
    }
  }

  "attempted submission email should succeed" in {
    when(requestBuilderExecute[HttpResponse])
      .thenReturn(Future.successful(HttpResponse(200, "")))
    connector.sendSubmissionReceivedEmail("test", "test") onComplete {
      case Success(_) =>
      case Failure(_) => fail()
    }
  }

  "attempted submission email should fail if email service fails" in {
    when(requestBuilderExecute[HttpResponse])
      .thenReturn(Future.failed(new Exception("")))
    connector.sendSubmissionReceivedEmail("test", "test") onComplete {
      case Success(_) => fail()
      case Failure(_) => // do nothing
    }
  }

}
