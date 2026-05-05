/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.softdrinksindustrylevy

import play.api.Logger
import play.api.libs.json.Reads
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.softdrinksindustrylevy.models.{Address, ForeignAddress, UkAddress}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

package object utils {

  def outboundHeaderCarrier(hc: HeaderCarrier): HeaderCarrier =
    HeaderCarrier(
      requestId = hc.requestId,
      sessionId = hc.sessionId
    )

  def parseResponse[A: Reads](response: HttpResponse): A =
    response.json.as[A]

  def upstreamError(system: String, operation: String, status: Int): UpstreamErrorResponse =
    UpstreamErrorResponse(
      s"Received $status from $system during $operation",
      status,
      status
    )

  def recover[A](system: String, operation: String, startTime: Long)(implicit
    ec: ExecutionContext,
    logger: Logger
  ): PartialFunction[Throwable, Future[A]] = {
    case e @ UpstreamErrorResponse(_, status, _, _) =>
      logger.error(
        s"$system failure ${loggingContext(operation, Some(status), Some(startTime), Some(e.getClass.getSimpleName))}"
      )
      Future.failed(e)

    case NonFatal(e) =>
      logger.error(
        s"$system failure ${loggingContext(operation, startTime = Some(startTime), errorClass = Some(e.getClass.getSimpleName))}",
        e
      )
      Future.failed(e)
  }

  def endpointUrl(apiBaseUrl: String, path: String): String = s"$apiBaseUrl$path"

  class RawHttpReads extends HttpReads[HttpResponse] {
    override def read(method: String, url: String, response: HttpResponse): HttpResponse = response
  }

  def loggingContext(
    operation: String,
    status: Option[Int] = None,
    startTime: Option[Long] = None,
    errorClass: Option[String] = None
  ): String =
    Seq(
      Some(s"operation=$operation"),
      status.map(st => s"status=$st"),
      startTime.map(st => s"durationMs=${System.currentTimeMillis() - st}"),
      errorClass.map(name => s"errorClass=$name")
    ).flatten.mkString(" ")

  def formatAddress(address: Address): Address = {
    import uk.gov.hmrc.softdrinksindustrylevy.models.RosmResponseAddress.*
    address match {
      case a: UkAddress      => a.copy(lines = address.lines.map(_.clean))
      case b: ForeignAddress => b.copy(lines = address.lines.map(_.clean))
      case _                 => throw new Exception("Cannot format address with params supplied")
    }
  }

}
