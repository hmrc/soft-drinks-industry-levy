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

import org.apache.pekko.actor.{ActorSystem, Scheduler}
import org.apache.pekko.pattern
import play.api.Configuration

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class RetryService @Inject() (
  configuration: Configuration,
  actorSystem: ActorSystem
)(implicit ec: ExecutionContext) {

  private implicit val scheduler: Scheduler = actorSystem.scheduler

  private val defaultDelay: FiniteDuration =
    configuration.get[FiniteDuration]("retry.delay")

  private val defaultMaxAttempts: Int =
    configuration.get[Int]("retry.max-attempts")

  def retry[A](
    f: => Future[A],
    delay: FiniteDuration = defaultDelay,
    maxAttempts: Int = defaultMaxAttempts,
    retriable: Throwable => Boolean = NonFatal.apply
  ): Future[A] =
    f.recoverWith {
      case t if retriable(t) =>
        pattern.retry(() => f, maxAttempts - 2, delay)
    }
}
