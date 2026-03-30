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

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

abstract class DesHelpers(servicesConfig: ServicesConfig) {

  val http: HttpClientV2

  val serviceKey: String = s"Bearer ${servicesConfig.getConfString("des.token", "")}"
  val serviceEnvironment: String = servicesConfig.getConfString("des.environment", "")
  private val desCorrelationIdHeaderName: String =
    servicesConfig.getConfString("des.correlationIdHeaderName", "CorrelationId")
  private val rosmCorrelationIdHeaderName: String =
    servicesConfig.getConfString("des.rosmCorrelationIdHeaderName", desCorrelationIdHeaderName)

  private def correlationIdHeader(hc: HeaderCarrier, headerName: String): Seq[(String, String)] =
    hc.requestId.map(requestId => headerName -> requestId.value).toSeq

  protected def desHeaders(hc: HeaderCarrier): Seq[(String, String)] =
    Seq("Environment" -> serviceEnvironment, "Authorization" -> serviceKey) ++ correlationIdHeader(
      hc,
      desCorrelationIdHeaderName
    )

  protected def rosmHeaders(hc: HeaderCarrier): Seq[(String, String)] =
    Seq("Environment" -> serviceEnvironment, "Authorization" -> serviceKey) ++ correlationIdHeader(
      hc,
      rosmCorrelationIdHeaderName
    )

}
