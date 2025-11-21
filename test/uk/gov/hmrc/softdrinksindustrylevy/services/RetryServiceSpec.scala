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

package uk.gov.hmrc.softdrinksindustrylevy.services

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.inject.guice.GuiceApplicationBuilder

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RetryServiceSpec extends AnyWordSpec with Matchers with ScalaFutures {
  private val app = GuiceApplicationBuilder()
    .configure(
      "retry.delay"                         -> "10 milliseconds",
      "retry.max-attempts"                  -> 3,
      "create-internal-auth-token-on-start" -> false
    )
    .build()

  private val service = app.injector.instanceOf[RetryService]

  "retry" should {
    "must return successfully when the future is successful" in {
      service.retry(Future.successful("foobar")).futureValue mustEqual "foobar"
    }

    "must return successfully when the future returns after an initial failure" in {
      var times = 0

      def future: Future[String] = Future {
        times += 1
        if (times < 2) {
          throw new RuntimeException()
        } else {
          "foobar"
        }
      }

      service.retry(future).futureValue mustEqual "foobar"
      times mustBe 2
    }

    "must fail when the future fails consistently" in {
      var times = 0

      def future: Future[String] = Future {
        times += 1
        throw new RuntimeException()
      }

      service.retry(future).failed.futureValue
      times mustBe 3
    }
  }
}
