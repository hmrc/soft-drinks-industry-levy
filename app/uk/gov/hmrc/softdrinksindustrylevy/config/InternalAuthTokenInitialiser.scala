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

package uk.gov.hmrc.softdrinksindustrylevy.config

import org.apache.pekko.Done
import play.api.Logging
import play.api.http.Status.{CREATED, NOT_FOUND, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.softdrinksindustrylevy.services.RetryService
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue

abstract class InternalAuthTokenInitialiser {
  val initialised: Future[Done]
}

@Singleton
class NoOpInternalAuthTokenInitialiser @Inject() extends InternalAuthTokenInitialiser {
  override val initialised: Future[Done] = Future.successful(Done)
}

@Singleton
class InternalAuthTokenInitialiserImpl @Inject() (
  configuration: ServicesConfig,
  httpClient: HttpClientV2,
  retryService: RetryService
)(implicit ec: ExecutionContext)
    extends InternalAuthTokenInitialiser with Logging {
  private val authToken: String =
    configuration.getString("internal-auth.token")

  private val appName: String =
    configuration.getString("appName")

  override val initialised: Future[Done] =
    retryService.retry(ensureAuthToken(), delay = 30.seconds, maxAttempts = 10)

  Await.result(initialised, 5.minutes)

  private def ensureAuthToken(): Future[Done] =
    authTokenIsValid.flatMap { isValid =>
      if (isValid) then {
        logger.info("Auth token is already valid")
        Future.successful(Done)
      }
      else {
        createClientAuthToken()
      }
    }

  private def createClientAuthToken(): Future[Done] = {
    logger.info("Initialising auth token")
    httpClient
      .post(url"${configuration.baseUrl("internal-auth")}/test-only/token")(using HeaderCarrier())
      .withBody(
        Json.obj(
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
      )
      .execute
      .flatMap { response =>
        if (response.status) == CREATED then {
          logger.info("Auth token initialised")
          Future.successful(Done)
        }
        else {
          Future.failed(new RuntimeException("Unable to initialise internal-auth token"))
        }
      }
  }

  private def authTokenIsValid: Future[Boolean] = {
    logger.info("Checking auth token")
    httpClient
      .get(url"${configuration.baseUrl("internal-auth")}/test-only/token")(using HeaderCarrier())
      .setHeader("Authorization" -> authToken)
      .execute
      .flatMap {
        _.status match {
          case OK        => Future.successful(true)
          case NOT_FOUND => Future.successful(false)
          case _         => Future.failed(new RuntimeException("Unexpected response"))
        }
      }
  }
}
