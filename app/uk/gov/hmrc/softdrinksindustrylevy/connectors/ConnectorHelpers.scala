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

import play.api.Logger
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.softdrinksindustrylevy.models.{Address, ForeignAddress, UkAddress}

import java.time.format.DateTimeFormatter
import java.time.{Clock, Instant}
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

abstract class ConnectorHelpers(servicesConfig: ServicesConfig, clock: Clock) {

  protected val hipBaseURL: String = servicesConfig.baseUrl("hip")
  protected val softDrinksApiRoot: String = "soft-drinks"

  protected def hipHeaders: Seq[(String, String)] =
    Seq(
      "correlationid"         -> UUID.randomUUID().toString,
      "X-Originating-System"  -> "SDIL",
      "X-Receipt-Date"        -> DateTimeFormatter.ISO_INSTANT.format(Instant.now(clock)),
      "X-Transmitting-System" -> "HIP"
    )

  protected def outboundHeaderCarrier(hc: HeaderCarrier): HeaderCarrier =
    HeaderCarrier(
      requestId = hc.requestId,
      sessionId = hc.sessionId
    )

  /*protected def upstreamError(message: String, operation: String, status: Int): UpstreamErrorResponse =
    UpstreamErrorResponse(message, status)
   */
  protected def upstreamError(
    system: String,
    operation: String,
    status: Int,
    responseBody: Option[String] = None
  ): UpstreamErrorResponse =
    UpstreamErrorResponse(
      s"Received $status from $system during $operation" +
        responseBody.fold("")(body => s" with response body: $body"),
      status
    )

  protected def recover[A](operation: String, startTime: Long)(implicit
    ec: ExecutionContext,
    logger: Logger
  ): PartialFunction[Throwable, Future[A]] = {
    case e @ UpstreamErrorResponse(message, status, _, _) =>
      logger.error(
        s"$message ${loggingContext(
            operation = operation,
            status = Some(status),
            startTime = Some(startTime),
            errorClass = Some(e.getClass.getSimpleName)
          )}"
      )
      Future.failed(e)

    case NonFatal(e) =>
      logger.error(
        s"${e.getMessage} ${loggingContext(
            operation = operation,
            startTime = Some(startTime),
            errorClass = Some(e.getClass.getSimpleName)
          )}",
        e
      )
      Future.failed(e)
  }

  protected def endpointUrl(apiBaseUrl: String, path: String): String = s"$apiBaseUrl$path"

  protected def loggingContext(
    operation: String,
    status: Option[Int] = None,
    startTime: Option[Long] = None,
    errorClass: Option[String] = None,
    responseBody: Option[String] = None
  ): String =
    Seq(
      Some(s"operation=$operation"),
      status.map(st => s"status=$st"),
      startTime.map(st => s"durationMs=${System.currentTimeMillis() - st}"),
      errorClass.map(name => s"errorClass=$name"),
      responseBody.map(body => s"responseBody=$body")
    ).flatten.mkString(" ")

  protected def formatAddress(address: Address): Address = {
    import uk.gov.hmrc.softdrinksindustrylevy.models.RosmResponseAddress.*
    address match {
      case a: UkAddress      => a.copy(lines = address.lines.map(_.clean))
      case b: ForeignAddress => b.copy(lines = address.lines.map(_.clean))
      case _                 => throw new Exception("Cannot format address with params supplied")
    }
  }
}
