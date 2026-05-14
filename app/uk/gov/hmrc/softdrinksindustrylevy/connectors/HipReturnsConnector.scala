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

package uk.gov.hmrc.softdrinksindustrylevy.connectors

import com.google.inject.Singleton
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import play.api.{Logger, Mode}
import sdil.models.ReturnPeriod
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReadsInstances, HttpResponse, StringContextOps}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.softdrinksindustrylevy.models.ReturnsRequest
import uk.gov.hmrc.softdrinksindustrylevy.models.json.des.returns.*
import uk.gov.hmrc.softdrinksindustrylevy.services.SdilMongoPersistence

import java.time.Clock
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HipReturnsConnector @Inject(
  http: HttpClientV2,
  mode: Mode,
  servicesConfig: ServicesConfig,
  persistence: SdilMongoPersistence,
  auditing: AuditConnector,
  clock: Clock
) (implicit executionContext: ExecutionContext)
    extends ConnectorHelpers(servicesConfig, clock) {

  implicit private val logger: Logger = Logger(this.getClass)

  def submitReturn(sdilRef: String, returnsRequest: ReturnsRequest)(implicit
    hc: HeaderCarrier,
    period: ReturnPeriod
  ): Future[HttpResponse] = {

    val operation = "submitReturn"
    val path = s"/$softDrinksApiRoot/$sdilRef/return"
    val startTime = System.currentTimeMillis()

    logger.info(s"HIP request ${loggingContext(operation)}")

    http
      .post(url"${endpointUrl(hipBaseURL, path)}")(using outboundHeaderCarrier(hc))
      .transform(_.addHttpHeaders(hipHeaders*))
      .withBody(Json.toJson(returnsRequest))
      .execute[HttpResponse](using HttpReadsInstances.readRaw, executionContext)
      .map { response =>
        logger.info(s"HIP response ${loggingContext(operation, Some(response.status), Some(startTime))}")
        response
      }
      .recoverWith(recover(operation, startTime))
  }
}
