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

import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.play.PlaySpec
import play.api.i18n.MessagesApi
import play.api.libs.ws.WSClient
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.softdrinksindustrylevy.services.{ReturnsPersistence, SdilMongoPersistence}

trait FakeApplicationSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar {

  lazy val messagesApi = app.injector.instanceOf[MessagesApi]
  lazy val wsClient = app.injector.instanceOf[WSClient]
  lazy val httpClient: HttpClientV2 = app.injector.instanceOf[HttpClientV2]
  lazy val components: ControllerComponents = app.injector.instanceOf[ControllerComponents]

  val returns = mock[ReturnsPersistence]

  val subscriptions: SdilMongoPersistence = mock[SdilMongoPersistence]

}
