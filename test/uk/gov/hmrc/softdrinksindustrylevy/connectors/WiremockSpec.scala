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

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import org.scalatest._
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.play.it.Port
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.softdrinksindustrylevy.util.FakeApplicationSpec

import scala.concurrent.ExecutionContext

trait WiremockSpec extends FakeApplicationSpec with BeforeAndAfterEach with BeforeAndAfterAll with ScalaFutures {
  val port = WireMockSupport.port
  val mockServer = new WireMockServer(port)

  implicit lazy val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  val mockServerUrl = s"http://localhost:$port"

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    mockServer.start()
    WireMock.configureFor("localhost", WireMockSupport.port)
  }

  protected val baseUrl = s"http://localhost:${WireMockSupport.port}"

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    WireMock.reset()
  }

  object WireMockSupport {
    val port = 11111
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    mockServer.stop()
  }
}
