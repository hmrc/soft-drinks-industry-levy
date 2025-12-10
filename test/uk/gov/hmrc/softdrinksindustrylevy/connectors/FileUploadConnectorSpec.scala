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

import org.scalatest.matchers.should.Matchers.shouldBe
import play.api.http.Status
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.softdrinksindustrylevy.util.WireMockMethods

import scala.concurrent.ExecutionContext

class FileUploadConnectorSpec extends HttpClientV2Helper with WireMockMethods {

  val connector: FileUploadConnector = app.injector.instanceOf[FileUploadConnector]

  implicit lazy val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  "attempted get of file should succeed if the file is returned" in {

    when(GET, "/file-upload/envelopes/1234/files/testfile/content")
      .thenReturn(Status.OK, """{ "directDebitMandateFound" : true }""")

    await(connector.getFile("1234", "testfile")).utf8String shouldBe "{ \"directDebitMandateFound\" : true }"

  }
}
