/*
 * Copyright 2022 HM Revenue & Customs
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

import com.mongodb.{ReadConcern, ReadPreference, WriteConcern}
import com.mongodb.client.model.{CreateCollectionOptions, CreateViewOptions}
import org.mongodb.scala.{MongoClient, MongoDatabase}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.play.{BaseOneAppPerSuite, FakeApplicationFactory, PlaySpec}
import play.api.i18n.MessagesApi
import play.api.inject.DefaultApplicationLifecycle
import play.api.libs.ws.WSClient
import play.api.mvc.ControllerComponents
import play.api.{Application, ApplicationLoader}
import play.core.DefaultWebCommands
import sdil.models.{ReturnPeriod, SdilReturn}
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.softdrinksindustrylevy.models.Subscription
import uk.gov.hmrc.softdrinksindustrylevy.services.{ReturnsPersistence, SdilMongoPersistence}
import com.mongodb.reactivestreams.client.{AggregatePublisher, ChangeStreamPublisher, ClientSession, ListCollectionsPublisher, MongoCollection, MongoDatabase => JMongoDatabase}
import org.bson.Document
import org.bson.codecs.configuration.CodecRegistry
import org.bson.conversions.Bson
import org.reactivestreams.Publisher

import java.util
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, ExecutionContext => EC}

trait FakeApplicationSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with MongoConnectorCustom {

  lazy val messagesApi = app.injector.instanceOf[MessagesApi]
  lazy val wsClient = app.injector.instanceOf[WSClient]
  lazy val httpClient: HttpClient = app.injector.instanceOf[HttpClient]
  lazy val components: ControllerComponents = app.injector.instanceOf[ControllerComponents]

  implicit val mongoComponent: MongoComponent = new MongoComponent {
    override def client: MongoClient = mongoClient

    override def database: MongoDatabase = mock[MongoDatabase]
  }

  val returns = mock[ReturnsPersistence]

  val subscriptions: SdilMongoPersistence = mock[SdilMongoPersistence]

}
