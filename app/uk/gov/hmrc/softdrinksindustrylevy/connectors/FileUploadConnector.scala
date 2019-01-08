/*
 * Copyright 2019 HM Revenue & Customs
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

import akka.util.ByteString
import play.api.Configuration
import play.api.Mode.Mode
import play.api.libs.ws.WSClient
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

class FileUploadConnector(ws: WSClient,
                          val mode: Mode,
                          val runModeConfiguration: Configuration)
                         (implicit ec: ExecutionContext) extends ServicesConfig {

  val url: String = baseUrl("file-upload")

  def getFile(envelopeId: String, fileName: String): Future[ByteString] = {
    ws.url(s"$url/file-upload/envelopes/$envelopeId/files/$fileName/content").get().map(_.bodyAsBytes)
  }
}
