/*
 * Copyright 2018 HM Revenue & Customs
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

import org.scalatestplus.play.{BaseOneAppPerSuite, FakeApplicationFactory, PlaySpec}
import play.api.libs.ws.{WSAPI, WSClient}
import play.api.{Application, ApplicationLoader}
import play.core.DefaultWebCommands
import uk.gov.hmrc.play.bootstrap.http.{DefaultHttpClient, HttpClient}
import uk.gov.hmrc.softdrinksindustrylevy.config.SdilApplicationLoader

trait FakeApplicationSpec extends PlaySpec with BaseOneAppPerSuite with FakeApplicationFactory with TestWiring {
  override def fakeApplication(): Application = {
    val context = ApplicationLoader.Context(
      environment,
      sourceMapper = None,
      new DefaultWebCommands,
      configuration
    )

    new SdilApplicationLoader().load(context)
  }

  private lazy val wsClient = app.injector.instanceOf[WSAPI].client
  lazy val httpClient: HttpClient = new DefaultHttpClient(configuration, auditConnector, wsClient)
}
