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

package uk.gov.hmrc.softdrinksindustrylevy.services

import java.time.temporal.{ChronoUnit, TemporalUnit}
import java.time.{Instant, LocalDateTime}
import javax.inject.Inject

import akka.actor.{ActorSystem, Cancellable}
import org.joda.time.Duration
import play.api.{Configuration, Logger}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lock.{ExclusiveTimePeriodLock, LockMongoRepository, LockRepository}
import uk.gov.hmrc.mongo.MongoConnector
import uk.gov.hmrc.softdrinksindustrylevy.connectors.ContactFrontendConnector

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class OverdueSubmissionsChecker(
  val config: Configuration,
  val mongoConnector: MongoConnector,
  val actorSystem: ActorSystem,
  mongoBufferService: MongoBufferService,
  contactFrontend: ContactFrontendConnector)(implicit val ec: ExecutionContext)
    extends LockedJobScheduler {

  override val jobName: String = "overdueSubmissions"

  override val jobEnabled: Boolean = {
    config.getBoolean(s"$jobName.enabled").getOrElse(throw MissingConfiguration(s"$jobName.enabled"))
  }

  override val jobStartDelay: FiniteDuration = {
    config.getLong(s"$jobName.startDelayMinutes") match {
      case Some(d) => FiniteDuration(d, MINUTES)
      case None    => throw MissingConfiguration(s"$jobName.startDelayMinutes")
    }
  }

  val overduePeriod: Duration = {
    config.getLong(s"$jobName.overduePeriodMinutes") match {
      case Some(d) => Duration.standardMinutes(d)
      case None    => throw MissingConfiguration(s"$jobName.overduePeriodMinutes")
    }
  }

  override val jobInterval: Duration = {
    config.getLong(s"$jobName.jobIntervalMinutes") match {
      case Some(d) => Duration.standardMinutes(d)
      case None    => throw MissingConfiguration(s"$jobName.jobIntervalMinutes")
    }
  }

  override protected def runJob()(implicit ec: ExecutionContext): Future[Unit] =
    for {
      subs <- mongoBufferService.findOverdue(Instant.now.minus(overduePeriod.getStandardMinutes, ChronoUnit.MINUTES))
      _    <- handleOverdueSubmissions(subs)
    } yield Logger.info(s"job $jobName complete; rerunning in ${jobInterval.getStandardMinutes} minutes")

  private def handleOverdueSubmissions(submissions: Seq[SubscriptionWrapper])(implicit ec: ExecutionContext) =
    Future.sequence(
      submissions map { sub =>
        for {
          _ <- mongoBufferService.updateStatus(sub._id, "OVERDUE")
          _ = Logger.warn(s"Overdue submission (safe id ${sub._id}; submitted at ${sub.timestamp})")
          _ <- contactFrontend.raiseTicket(sub.subscription, sub.formBundleNumber, sub.timestamp)(HeaderCarrier(), ec)
        } yield ()
      }
    )

  if (jobEnabled) {
    Logger.info(s"scheduling $jobName to start running in ${jobStartDelay.toMinutes} minutes")
    start()
  } else {
    Logger.info(s"Job $jobName disabled")
  }
}

trait LockedJobScheduler {
  val config: Configuration
  val mongoConnector: MongoConnector
  val actorSystem: ActorSystem
  val jobName: String
  val jobInterval: Duration
  val jobStartDelay: FiniteDuration
  val jobEnabled: Boolean

  val lock: ExclusiveTimePeriodLock = new ExclusiveTimePeriodLock {
    override def holdLockFor: Duration = jobInterval

    override def repo: LockRepository = LockMongoRepository(mongoConnector.db)

    override def lockId: String = jobName
  }

  protected def runJob()(implicit ec: ExecutionContext): Future[Unit]

  private def run()(implicit ec: ExecutionContext): Future[Unit] = {
    Logger.info(s"Running job $jobName")

    lock.tryToAcquireOrRenewLock {
      runJob() recoverWith {
        case e: Exception =>
          Logger.error(s"Job $jobName failed", e)
          Future.failed(e)
      }
    } map {
      _.getOrElse(Logger.info(s"Unable to run job $jobName"))
    }

  }

  def start()(implicit ec: ExecutionContext): Cancellable =
    actorSystem.scheduler.schedule(
      jobStartDelay,
      FiniteDuration(jobInterval.getStandardMinutes, MINUTES)
    )(run())
}

case class MissingConfiguration(key: String) extends RuntimeException(s"Missing configuration value $key")
