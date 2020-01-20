/*
 * Copyright 2020 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, stubFor, urlPathEqualTo}

import scala.util.{Failure, Success, Try}

class FileUploadConnectorSpec extends WiremockSpec {

  object TestConnector extends FileUploadConnector(wsClient, environment.mode, servicesConfig) {
    override val url: String = mockServerUrl
  }

  "attempted get of file should succeed if the file is returned" in {
    stubFor(
      get(urlPathEqualTo("/file-upload/envelopes/1234/files/testfile/content"))
        .willReturn(aResponse().withStatus(200).withBody("some data")))

    Try(TestConnector.getFile("1234", "testfile").futureValue) match {
      case Success(_) =>
      case Failure(_) => fail
    }
  }
}
