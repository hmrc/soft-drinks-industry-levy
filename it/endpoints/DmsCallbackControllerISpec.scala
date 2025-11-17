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

package endpoints

import play.api.libs.json.Json
import play.api.libs.ws.WSRequest
import support.IntegrationBaseSpec

import scala.util.Random
import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue

class DmsCallbackControllerISpec extends IntegrationBaseSpec {
  trait Test {
    protected val callbackRouteUri: String = "/soft-drinks-industry-levy/dms/callback"

    private def request(uri: String): WSRequest = buildRequest(uri)

    protected def callbackRequest: WSRequest = request(callbackRouteUri)
  }

  "/dms/callback" should {
    "return 200 OK response" in new Test {
      private val dmsSubmissionResult = Json.obj(
        "id"     -> Random.alphanumeric.take(12).toString(),
        "status" -> "Processed"
      )
      private val result = await(callbackRequest.post(dmsSubmissionResult))
      result.status shouldBe OK
    }
  }
}
