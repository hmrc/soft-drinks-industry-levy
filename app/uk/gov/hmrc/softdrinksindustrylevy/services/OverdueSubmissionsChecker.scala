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

package uk.gov.hmrc.softdrinksindustrylevy.services

import org.apache.pekko.actor.{ActorSystem, Cancellable}
import com.google.inject.{Inject, Singleton}
import org.joda.time.Duration
import play.api.{Configuration, Logger}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.lock.{LockRepository, MongoLockRepository, TimePeriodLockService}
import uk.gov.hmrc.softdrinksindustrylevy.connectors.ContactFrontendConnector

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.duration.{FiniteDuration, MINUTES}
import scala.concurrent.{ExecutionContext, Future, duration}

@Singleton
class OverdueSubmissionsChecker @Inject()(
  val mongoLockRepository: MongoLockRepository,
  mongoBufferService: MongoBufferService,
  val config: Configuration,
  val actorSystem: ActorSystem,
  contactFrontend: ContactFrontendConnector)(implicit ec: ExecutionContext) {
  val logger = Logger(this.getClass)

  val jobName: String = "overdueSubmissions"

  val jobEnabled: Boolean = {
    config.getOptional[Boolean](s"$jobName.enabled") match {
      case Some(value) => value
      case None        => throw MissingConfiguration(s"$jobName.enabled")
    }
  }

  val jobStartDelay: FiniteDuration = {
    config.getOptional[Long](s"$jobName.startDelayMinutes") match {
      case Some(d) => FiniteDuration(d, MINUTES)
      case None    => throw MissingConfiguration(s"$jobName.startDelayMinutes")
    }
  }

  val overduePeriod: Duration = {
    config.getOptional[Long](s"$jobName.overduePeriodMinutes") match {
      case Some(d) => Duration.standardMinutes(d)
      case None    => throw MissingConfiguration(s"$jobName.overduePeriodMinutes")
    }
  }

  val jobInterval: Duration = {
    config.getOptional[Long](s"$jobName.jobIntervalMinutes") match {
      case Some(d) => Duration.standardMinutes(d)
      case None    => throw MissingConfiguration(s"$jobName.jobIntervalMinutes")
    }
  }

  protected def runJob()(implicit ec: ExecutionContext): Future[Unit] =
    for {
      subs <- mongoBufferService.findOverdue(Instant.now.minus(overduePeriod.getStandardMinutes, ChronoUnit.MINUTES))
      _    <- handleOverdueSubmissions(subs)
    } yield logger.info(s"job $jobName complete; rerunning in ${jobInterval.getStandardMinutes} minutes")

  private def handleOverdueSubmissions(submissions: Seq[SubscriptionWrapper])(implicit ec: ExecutionContext) =
    Future.sequence(
      submissions map { sub =>
        for {
          _ <- mongoBufferService.updateStatus(sub._id, "OVERDUE")
          _ = logger.warn(s"Overdue submission (safe id ${sub._id}; submitted at ${sub.timestamp})")
          _ <- contactFrontend.raiseTicket(sub.subscription, sub.formBundleNumber)(HeaderCarrier(), ec)
        } yield ()
      }
    )

  if (jobEnabled) {
    logger.info(s"scheduling $jobName to start running in ${jobStartDelay.toMinutes} minutes")
    start()
  } else {
    logger.info(s"Job $jobName disabled")
  }

  val lock: TimePeriodLockService = new TimePeriodLockService {
    override val lockRepository: LockRepository = mongoLockRepository
    override val lockId: String = jobName
    override val ttl: duration.Duration = FiniteDuration(jobInterval.getStandardMinutes, MINUTES)
  }

  private def run()(implicit ec: ExecutionContext): Future[Unit] = {
    logger.info(s"Running job $jobName")

    lock.withRenewedLock {
      runJob() recoverWith {
        case e: Exception =>
          logger.error(s"Job $jobName failed", e)
          Future.failed(e)
      }
    } map {
      _.getOrElse(logger.info(s"Unable to run job $jobName"))
    }

  }

  def start()(implicit ec: ExecutionContext): Cancellable =
    actorSystem.scheduler.schedule(
      jobStartDelay,
      FiniteDuration(jobInterval.getStandardMinutes, MINUTES)
    )(run())
}

case class MissingConfiguration(key: String) extends RuntimeException(s"Missing configuration value $key")
