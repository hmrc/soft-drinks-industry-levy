/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.softdrinksindustrylevy.config

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.stubbing.Scenario
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.{Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status.{CREATED, INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers.{AUTHORIZATION, running}
import uk.gov.hmrc.http.test.WireMockSupport

class InternalAuthInitialiserSpec
    extends AnyWordSpec with Matchers with ScalaFutures with IntegrationPatience with WireMockSupport {
  "when configured to run" when {
    "must initialise the internal-auth token if it is not already initialised" in {
      val authToken = "authToken"
      val appName = "appName"

      val expectedRequest = Json.obj(
        "token"     -> authToken,
        "principal" -> appName,
        "permissions" -> Seq(
          Json.obj(
            "resourceType"     -> "dms-submission",
            "resourceLocation" -> "submit",
            "actions"          -> List("WRITE")
          )
        )
      )

      stubFor(
        get(urlMatching("/test-only/token"))
          .willReturn(aResponse().withStatus(NOT_FOUND))
      )

      stubFor(
        post(urlMatching("/test-only/token"))
          .willReturn(aResponse().withStatus(CREATED))
      )

      val app = GuiceApplicationBuilder()
        .configure(
          "microservice.services.internal-auth.port" -> wireMockPort,
          "appName"                                  -> appName,
          "create-internal-auth-token-on-start"      -> true,
          "internal-auth.token"                      -> authToken
        )
        .build()

      running(app) {
        eventually(Timeout(Span(30, Seconds))) {
          verify(
            1,
            getRequestedFor(urlMatching("/test-only/token"))
              .withHeader(AUTHORIZATION, equalTo(authToken))
          )
          verify(
            1,
            postRequestedFor(urlMatching("/test-only/token"))
              .withRequestBody(equalToJson(Json.stringify(Json.toJson(expectedRequest))))
          )
        }
      }
    }

    "must not initialise the internal-auth token if it is already initialised" in {
      val authToken = "authToken"
      val appName = "appName"

      stubFor(
        get(urlMatching("/test-only/token"))
          .willReturn(aResponse().withStatus(OK))
      )

      stubFor(
        post(urlMatching("/test-only/token"))
          .willReturn(aResponse().withStatus(CREATED))
      )

      val app = GuiceApplicationBuilder()
        .configure(
          "microservice.services.internal-auth.port" -> wireMockPort,
          "appName"                                  -> appName,
          "create-internal-auth-token-on-start"      -> true,
          "internal-auth.token"                      -> authToken
        )
        .build()

      running(app) {
        app.injector.instanceOf[InternalAuthTokenInitialiser].initialised.futureValue

        verify(
          1,
          getRequestedFor(urlMatching("/test-only/token"))
            .withHeader(AUTHORIZATION, equalTo(authToken))
        )
        verify(0, postRequestedFor(urlMatching("/test-only/token")))
      }
    }

    "must retry if anything fails" in {
      val authToken = "authToken"
      val appName = "appName"

      stubFor(
        get(urlMatching("/test-only/token"))
          .inScenario("Retry")
          .whenScenarioStateIs(Scenario.STARTED)
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR))
          .willSetStateTo("Failing connection made")
      )

      stubFor(
        get(urlMatching("/test-only/token"))
          .inScenario("Retry")
          .whenScenarioStateIs("Failing connection made")
          .willReturn(aResponse().withStatus(NOT_FOUND))
      )

      stubFor(
        post(urlMatching("/test-only/token"))
          .willReturn(aResponse().withStatus(CREATED))
      )

      val app = GuiceApplicationBuilder()
        .configure(
          "microservice.services.internal-auth.port" -> wireMockPort,
          "appName"                                  -> appName,
          "create-internal-auth-token-on-start"      -> true,
          "internal-auth.token"                      -> authToken
        )
        .build()

      running(app) {
        eventually(Timeout(Span(30, Seconds))) {
          verify(
            2,
            getRequestedFor(urlMatching("/test-only/token"))
              .withHeader(AUTHORIZATION, equalTo(authToken))
          )
          verify(1, postRequestedFor(urlMatching("/test-only/token")))
        }
      }
    }
  }

  "when not configured to run" when {
    "must not make the relevant calls to internal-auth" in {
      val authToken = "authToken"
      val appName = "appName"

      stubFor(
        get(urlMatching("/test-only/token"))
          .willReturn(aResponse().withStatus(OK))
      )

      stubFor(
        post(urlMatching("/test-only/token"))
          .willReturn(aResponse().withStatus(CREATED))
      )

      val app = GuiceApplicationBuilder()
        .configure(
          "microservice.services.internal-auth.port" -> wireMockPort,
          "appName"                                  -> appName,
          "create-internal-auth-token-on-start"      -> false,
          "internal-auth.token"                      -> authToken
        )
        .build()

      app.injector.instanceOf[InternalAuthTokenInitialiser].initialised.futureValue

      verify(0, getRequestedFor(urlMatching("/test-only/token")))
      verify(0, postRequestedFor(urlMatching("/test-only/token")))
    }
  }
}
