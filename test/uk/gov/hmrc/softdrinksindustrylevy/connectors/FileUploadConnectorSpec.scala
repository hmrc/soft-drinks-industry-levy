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
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.{HttpClient, HttpResponse}
import uk.gov.hmrc.softdrinksindustrylevy.util.FakeApplicationSpec

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class FileUploadConnectorSpec extends FakeApplicationSpec with MockitoSugar with BeforeAndAfterEach {

  val connector = app.injector.instanceOf[FileUploadConnector]

  val mockHttpClient = mock[HttpClient]

  implicit lazy val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  "attempted get of file should succeed if the file is returned" in {

    when(mockHttpClient.GET[HttpResponse](any(), any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(HttpResponse(200, """{ "directDebitMandateFound" : true }""")))

    connector.getFile("1234", "testfile") onComplete {
      case Success(_) =>
      case Failure(_) => fail()
    }
  }
}
