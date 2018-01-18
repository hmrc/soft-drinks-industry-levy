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

package uk.gov.hmrc.softdrinksindustrylevy.services

import java.time.LocalDateTime
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

class OverdueSubmissionsChecker @Inject()(val config: Configuration,
                                          val mongoConnector: MongoConnector,
                                          val actorSystem: ActorSystem,
                                          mongoBufferService: MongoBufferService,
                                          contactFrontend: ContactFrontendConnector)
                                         (implicit val ec: ExecutionContext) extends LockedJobScheduler {

  override val jobName: String = "overdueSubmissions"

  override val jobEnabled: Boolean = {
    config.getBoolean(s"$jobName.enabled").getOrElse(throw new RuntimeException("Missing config setting overdueSubmissions.enabled"))
  }

  override val jobStartDelay: FiniteDuration = {
    config.getMilliseconds("overdueSubmissions.startDelay") match {
      case Some(delay) => FiniteDuration(delay, MILLISECONDS)
      case None => throw new RuntimeException("Missing config setting overdueSubmissions.startDelay")
    }
  }

  val overduePeriod: Duration = {
    config.getMilliseconds("overdueSubmissions.overduePeriod") match {
      case Some(millis) => Duration.millis(millis)
      case None => throw new RuntimeException("Missing config setting overdueSubmissions.overduePeriod")
    }
  }

  override val jobInterval: Duration = {
    config.getMilliseconds("overdueSubmissions.jobInterval") match {
      case Some(millis) => Duration.millis(millis)
      case None => throw new RuntimeException("Missing config setting overdueSubmissions.jobInterval")
    }
  }

  override protected def runJob()(implicit ec: ExecutionContext): Future[Unit] = {
    for {
      subs <- mongoBufferService.findOverdue(LocalDateTime.now.minusHours(overduePeriod.getStandardHours))
      _ <- handleOverdueSubmissions(subs)
    } yield Logger.info(s"job $jobName complete; rerunning in ${jobInterval.getStandardMinutes} minutes")
  }

  private def handleOverdueSubmissions(submissions: Seq[SubscriptionWrapper])(implicit ec: ExecutionContext) = {
    Future.sequence(
      submissions map { sub =>
        for {
          _ <- mongoBufferService.updateStatus(sub._id, "OVERDUE")
          _ = Logger.warn(s"Overdue submission (safe id ${sub._id}; submitted at ${sub.timestamp})")
          _ <- contactFrontend.raiseTicket(sub.subscription, sub.formBundleNumber, sub.timestamp)(HeaderCarrier(), ec)
        } yield ()
      }
    )
  }

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

  private def run()(implicit ec: ExecutionContext): Future[Option[Unit]] = lock.tryToAcquireOrRenewLock {
    Logger.info(s"Running job $jobName")
    runJob() recoverWith {
      case e: Exception =>
        Logger.error(s"Job $jobName failed", e)
        Future.failed(e)
    }
  }

  def start()(implicit ec: ExecutionContext): Cancellable = {
    actorSystem.scheduler.schedule(
      jobStartDelay,
      FiniteDuration(jobInterval.getStandardMinutes, MINUTES)
    )(run())
  }
}
