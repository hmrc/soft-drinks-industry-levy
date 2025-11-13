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

package uk.gov.hmrc.softdrinksindustrylevy.connectors.dms

import com.google.inject.{Inject, Singleton}
import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import play.api.http.HeaderNames.AUTHORIZATION
import play.api.libs.ws.WSBodyWritables.bodyWritableOf_Multipart
import play.api.mvc.MultipartFormData
import play.api.{Logging, Mode}
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.softdrinksindustrylevy.models.DmsSubmission
import uk.gov.hmrc.softdrinksindustrylevy.models.dms.DmsSubmissionPayload
import uk.gov.hmrc.softdrinksindustrylevy.services.dms.PdfGenerationService

import java.time.format.DateTimeFormatter
import java.time.{Clock, LocalDateTime}
import java.util.Base64
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DmsConnector @Inject() (
  httpClient: HttpClientV2,
  val mode: Mode,
  servicesConfig: ServicesConfig,
  pdfGeneratorService: PdfGenerationService,
  dmsSubmissionPayload: DmsSubmissionPayload,
  clock: Clock
) extends Logging {

  private val sdilUrl = servicesConfig.baseUrl("soft-drinks-industry-levy")
  private val url = s"${servicesConfig.baseUrl("dms")}/dms-submission/submit"
  val dmsSubmission = new DmsSubmission

  def submitToDms(html: String, sdilNumber: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {

    val dataParts: Seq[MultipartFormData.DataPart] = Seq(
      MultipartFormData.DataPart("callbackUrl", s"$sdilUrl/soft-drinks-industry-levy/dms/callback"),
      MultipartFormData.DataPart("metadata.source", "sidl"),
      MultipartFormData
        .DataPart("metadata.timeOfReceipt", DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now(clock))),
      MultipartFormData.DataPart("metadata.formId", "SDIL-VAR-1"),
      MultipartFormData.DataPart("metadata.customerId", sdilNumber),
      MultipartFormData.DataPart("metadata.classificationType", "BT-NRU-SDIL"),
      MultipartFormData.DataPart("metadata.businessArea", "BI")
    )

    val pdfBytes = pdfGeneratorService.generatePDFBytes(decode(html))

    val fileParts: Seq[MultipartFormData.FilePart[Source[ByteString, ?]]] =
      Seq(
        MultipartFormData.FilePart(
          key = "form",
          filename = "form.pdf",
          contentType = Some("application/octet-stream"),
          ref = Source.single(ByteString(pdfBytes))
        )
      )

    val attachmentParts: Seq[MultipartFormData.FilePart[Source[ByteString, ?]]] = dmsSubmissionPayload.attachments.map {
      attachment =>
        MultipartFormData.FilePart(
          key = "attachment",
          filename = attachment.filename,
          contentType = Some("application/octet-stream"),
          ref = Source.apply(attachment.data)
        )
    }

    val source: Source[MultipartFormData.Part[Source[ByteString, ?]], NotUsed] = Source(
      dataParts ++ fileParts ++ attachmentParts
    )

    logger.info("Sending payload to dms service...")
    httpClient
      .post(url"$url")
      .setHeader(AUTHORIZATION -> servicesConfig.getString("internal-auth.token"))
      .withBody(source)
      .execute[HttpResponse]
      .map { _ =>
        ()
      }
  }

  private val decode = (s: String) => new String(Base64.getDecoder.decode(s))
}
