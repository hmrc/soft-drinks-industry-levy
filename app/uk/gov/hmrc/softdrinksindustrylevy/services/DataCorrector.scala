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

import javax.inject.{Inject, Named}
import akka.actor._
import play.api.Logger
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import play.api.libs.concurrent.Akka
import play.api.Application
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.softdrinksindustrylevy.config.SdilConfig
import uk.gov.hmrc.softdrinksindustrylevy.connectors.DesConnector
import sdil.models._
import cats.implicits._

/** Actor which creates/updates missing/erroneous SdilReturns in mongo
  *
  * When passed a [[DataCorrector.ReturnsCorrection]] object will look up the UTR (if not
  * provided) from the SDIL reference and then create the [[sdil.models.SdilReturn]] in
  * mongo. Called from [[DataCorrector.DataCorrectorSupervisor]].
  */
class ReturnsCorrectorWorker(
  connector: DesConnector,
  persistence: SdilPersistence
)(implicit ec: ExecutionContext)
    extends Actor {
  import DataCorrector._
  implicit val hc = new HeaderCarrier()
  val logger = Logger("DataCorrector")

  def getUtrFromSdil(sdilRef: String): Future[String] =
    connector.retrieveSubscriptionDetails("sdil", sdilRef).map {
      case Some(x) =>
        logger.info(s"found UTR of ${x.utr} for SDIL ref $sdilRef")
        x.utr
      case None =>
        throw new NoSuchElementException(s"Cannot find subscription with SDIL ref $sdilRef")
    }

  override def receive = {
    case ReturnsCorrection(sdilRefO, utrO, period, data) =>
      logger.info(s"attempting to process $utrO/$sdilRefO")
      val job: Future[Unit] = for {
        utr <- utrO.fold(getUtrFromSdil(sdilRefO.get))(_.pure[Future])
      } yield {
        persistence.returns(utr, period) = data
      }
      job.onSuccess {
        case _ => logger.info(s"done processing $utrO/$sdilRefO")
      }

    case e => logger.warn(s"Don't understand $e")
  }
}

/** Corrects local (MDTP) mongo data with records read from configuration
  *
  * Reads in any pending correction records from the system configuration and
  * schedules corrective action.
  */
class DataCorrector(
  system: ActorSystem,
  persistence: SdilPersistence,
  allConfig: SdilConfig,
  connector: DesConnector
)(implicit ec: ExecutionContext) {
  import DataCorrector._

  val config = allConfig.dataCorrector
  val logger = Logger("DataCorrector")

  logger.info("DataCorrector startup")
  logger.info(s"pending returns corrections: ${config.returns.size}")

  class DataCorrectorSupervisor(_pending: List[ReturnsCorrection]) extends Actor {
    var pending = _pending

    // worker threads
    val worker = context.actorOf(
      Props(
        new ReturnsCorrectorWorker(connector, persistence)
      ),
      name = "worker")

    override def receive = {
      case "next" =>
        pending match {
          case (h :: hs) =>
            pending = hs
            worker ! h
            if (pending.isEmpty)
              logger.info(s"no more records to process")
          case Nil =>
        }
      case e => logger.warn(s"Don't understand $e")
    }
  }

  val supervisor = system.actorOf(Props(new DataCorrectorSupervisor(config.returns)), name = "datacorrector")

  system.scheduler.schedule(
    initialDelay = config.initialDelay,
    interval = config.interval,
    receiver = supervisor,
    message = "next"
  )

}

object DataCorrector {

  case class Config(
    /** Rate at which records are processed */
    interval: FiniteDuration = 30 seconds,
    /** Delay between startup and the first record being processed */
    initialDelay: FiniteDuration = 10 seconds,
    /** Individual records to be upserted into mongo */
    returns: List[ReturnsCorrection] = Nil
  )

  /** Details of the record to be created/altered
    *
    * Returs are keyed on  the UTR, if no UTR is provided  but an SDIL Reference
    * is then a HoD call will be made to determine the UTR
    */
  case class ReturnsCorrection(
    sdilRef: Option[String] = None,
    utr: Option[String] = None,
    period: ReturnPeriod,
    data: SdilReturn
  ) {
    require(sdilRef.isDefined || utr.isDefined, "Either sdilRef or utr must be defined")
  }

}
