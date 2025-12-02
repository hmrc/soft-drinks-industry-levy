/*
 * Copyright 2024 HM Revenue & Customs
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

package support

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, EitherValues}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.{HeaderNames, Status}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.{WSClient, WSRequest}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits, ResultExtractors}
import play.api.{Application, Environment, Mode}
import uk.gov.hmrc.http.test.WireMockSupport

trait IntegrationBaseSpec
    extends AnyWordSpecLike with EitherValues with Matchers with FutureAwaits with DefaultAwaitTimeout
    with WireMockSupport with GuiceOneServerPerSuite with BeforeAndAfterEach with BeforeAndAfterAll with Status
    with HeaderNames with ResultExtractors {

  lazy val client: WSClient = app.injector.instanceOf[WSClient]

  def servicesConfig: Map[String, Any] = Map(
    "microservice.services.returns.host"  -> wireMockHost,
    "microservice.services.returns.port"  -> wireMockPort,
    "microservice.services.auth.host"     -> wireMockHost,
    "microservice.services.auth.port"     -> wireMockPort,
    "create-internal-auth-token-on-start" -> false
  )

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .configure(servicesConfig)
    .build()

  def buildRequest(path: String): WSRequest = client.url(s"http://localhost:$port$path").withFollowRedirects(false)

}
