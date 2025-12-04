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

package uk.gov.hmrc.softdrinksindustrylevy.util

import com.typesafe.config.ConfigFactory
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.MessagesApi
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.mvc.ControllerComponents
import play.api.{Application, Configuration, inject}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.test.WireMockSupport
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.softdrinksindustrylevy.services.{ReturnsPersistence, SdilMongoPersistence}

trait FakeApplicationSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with WireMockSupport {

  private val config = Configuration(
    ConfigFactory.parseString(
      s"""
         |create-internal-auth-token-on-start = false
         |microservice {
         |  services {
         |      tax-enrolments {
         |      protocol = http
         |      host     = $wireMockHost
         |      port     = $wireMockPort
         |    }
         |    des {
         |      protocol = http
         |      host = $wireMockHost
         |      port = $wireMockPort
         |      token = token
         |      environment = environment
         |    }
         |    email {
         |      protocol = http
         |      host     = $wireMockHost
         |      port     = $wireMockPort
         |    }
         |    contact-frontend {
         |      protocol = http
         |      host     = $wireMockHost
         |      port     = $wireMockPort
         |    }
         |    des-direct-debit {
         |      protocol = http
         |      host = $wireMockHost
         |      port = $wireMockPort
         |    }
         |    file-upload {
         |      protocol = http
         |      host     = $wireMockHost
         |      port     = $wireMockPort
         |    }
         |
         |  }
         |}
         |""".stripMargin
    )
  )
  val mockAuditConnector: AuditConnector = mock[AuditConnector]

  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .overrides(inject.bind[AuditConnector].toInstance(mockAuditConnector))
    .configure(config)
    .build()

  lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  lazy val httpClient: HttpClientV2 = app.injector.instanceOf[HttpClientV2]
  lazy val components: ControllerComponents = app.injector.instanceOf[ControllerComponents]

  val returns: ReturnsPersistence = mock[ReturnsPersistence]

  val subscriptions: SdilMongoPersistence = mock[SdilMongoPersistence]
}
