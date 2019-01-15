/*
 * Copyright 2019 HM Revenue & Customs
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

import org.scalatestplus.play.{BaseOneAppPerSuite, FakeApplicationFactory, PlaySpec}
import play.api.libs.ws.{WSAPI, WSClient}
import play.api.{Application, ApplicationLoader, Play}
import play.core.DefaultWebCommands
import reactivemongo.bson.BSONObjectID
import sdil.models.{ReturnPeriod, SdilReturn}
import uk.gov.hmrc.play.bootstrap.http.{DefaultHttpClient, HttpClient}
import uk.gov.hmrc.softdrinksindustrylevy.config.SdilApplicationLoader
import uk.gov.hmrc.softdrinksindustrylevy.models.Subscription
import uk.gov.hmrc.softdrinksindustrylevy.services.SdilPersistence

import scala.collection.mutable
import scala.concurrent.{Future, ExecutionContext => EC}


trait FakeApplicationSpec extends PlaySpec with BaseOneAppPerSuite with FakeApplicationFactory with TestWiring {
  override def fakeApplication(): Application = {
    val context = ApplicationLoader.Context(
      environment,
      sourceMapper = None,
      new DefaultWebCommands,
      configuration
    )

    new SdilApplicationLoader().load(context)
  }

  lazy val actorSystem = Play.current.actorSystem

  private lazy val wsClient = app.injector.instanceOf[WSAPI].client
  lazy val httpClient: HttpClient = new DefaultHttpClient(configuration, auditConnector, wsClient,actorSystem)


  lazy val testPersistence: SdilPersistence = new SdilPersistence {

    val returns: DAO[String, ReturnPeriod, SdilReturn] = new DAO[String, ReturnPeriod, SdilReturn] {
      private var data: Map[(String, ReturnPeriod), SdilReturn] = Map.empty
      private var getData: Map[(String, ReturnPeriod), (SdilReturn, Option[BSONObjectID])] = Map.empty
      def update(user: String, period: ReturnPeriod, value: SdilReturn)(implicit ec: EC): Future[Unit] = {
        data = data + { (user, period) -> value }
        Future.successful(())
      }

      def get(
        user: String,
        key: ReturnPeriod
      )(implicit ec: EC): Future[Option[(SdilReturn, Option[BSONObjectID])]] =
        Future.successful(getData.get((user, key)))

      def list(user: String)(implicit ec: EC): Future[Map[ReturnPeriod, SdilReturn]] =
        Future.successful{
          data.toList.collect{ case ((`user`, period), ret) => (period, ret) }.toMap
        }
      def listVariable(user: String)(implicit ec: EC): Future[Map[ReturnPeriod, SdilReturn]] =
        Future.successful{
          data.toList.collect{ case ((`user`, period), ret) => (period, ret) }.toMap
        }
    }

    val subscriptions: SubsDAO[String, Subscription] = new SubsDAO[String, Subscription] {
      private var data: scala.collection.mutable.Map[String, List[Subscription]] = mutable.Map.empty

      override def insert(key: String, value: Subscription)(implicit ec: EC): Future[Unit] = {
        data += data.get(key).fold(key -> List(value))(x => key -> (value +: x))
        Future.successful(())
      }

      override def list(key: String)(implicit ec: EC): Future[List[Subscription]] =
        Future(data.getOrElse(key, List.empty[Subscription]))

    }
  }

}

