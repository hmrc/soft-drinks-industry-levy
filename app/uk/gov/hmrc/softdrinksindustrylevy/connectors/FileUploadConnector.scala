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

import org.apache.pekko.util.ByteString
import play.api.Mode
import play.api.libs.ws.WSClient
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}
import com.google.inject.{Inject, Singleton}

@Singleton
class FileUploadConnector @Inject() (ws: WSClient, val mode: Mode, servicesConfig: ServicesConfig)(implicit
  ec: ExecutionContext
) {

  val url: String = servicesConfig.baseUrl("file-upload")

  def getFile(envelopeId: String, fileName: String): Future[ByteString] =
    ws.url(s"$url/file-upload/envelopes/$envelopeId/files/$fileName/content").get().map(_.bodyAsBytes)
}
