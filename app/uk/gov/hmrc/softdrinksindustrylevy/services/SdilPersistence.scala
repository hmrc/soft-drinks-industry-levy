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

package uk.gov.hmrc.softdrinksindustrylevy.services

import play.api.libs.json._
import sdil.models.{ReturnPeriod, SdilReturn}
import uk.gov.hmrc.softdrinksindustrylevy.models._
import com.google.inject.{Inject, Singleton}
import org.mongodb.scala.model.{Filters, FindOneAndReplaceOptions, IndexModel, IndexOptions, Indexes, ReturnDocument, Updates}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.softdrinksindustrylevy.services.SubscriptionWrap._

import java.time._
import scala.concurrent.{ExecutionContext => EC, _}
import uk.gov.hmrc.softdrinksindustrylevy.models.json.internal._
import play.api.libs.functional.syntax._
import uk.gov.hmrc.softdrinksindustrylevy.services.ReturnsWrapper.returnsWrapperFormat

case class SubscriptionWrap(
  utr: String,
  subscription: Subscription,
  retrievalTime: LocalDateTime = LocalDateTime.now()
)
object SubscriptionWrap {

  val subsWrapperFormat = Json.format[SubscriptionWrap]
}

case class ReturnsWrapper(
  utr: String,
  period: ReturnPeriod,
  sdilReturn: SdilReturn
)

object ReturnsWrapper {
  val returnsWrapperFormat = Json.format[ReturnsWrapper]
}

@Singleton
class SdilMongoPersistence @Inject()(
  mongoComponent: MongoComponent
)(implicit ec: EC)
    extends PlayMongoRepository[SubscriptionWrap](
      collectionName = "sdilsubscriptions",
      mongoComponent = mongoComponent,
      domainFormat = subsWrapperFormat,
      indexes = Seq(
        IndexModel(Indexes.ascending("utr"), IndexOptions().name("utrIdx").unique(false))
      )
    ) {

  // queries and updates can now be implemented with the available `collection: org.mongodb.scala.MongoCollection`
  def findAll(): Future[Seq[SubscriptionWrap]] = collection.find().toFuture()
  def insert(utr: String, value: Subscription)(implicit ec: EC): Future[Unit] =
    collection.insertOne(SubscriptionWrap(utr, value)).toFuture().map(_ => ())
  def list(utr: String)(implicit ec: EC): Future[List[Subscription]] =
    collection
      .find(Filters.equal("utr", utr))
      .collect()
      .toFuture()
      .map(_.map(_.subscription).toList)

}

@Singleton
class ReturnsPersistence @Inject()(
  mongoComponent: MongoComponent
)(implicit ec: EC)
    extends PlayMongoRepository[ReturnsWrapper](
      collectionName = "sdilreturns",
      mongoComponent = mongoComponent,
      domainFormat = returnsWrapperFormat,
      indexes = Seq(
        IndexModel(Indexes.ascending("utr"), IndexOptions().name("utrIdx")),
        IndexModel(Indexes.descending("period.year"), IndexOptions().name("periodYearIdx")),
        IndexModel(Indexes.descending("period.quarter"), IndexOptions().name("periodQuarterIdx"))
      )
    ) {
  // queries and updates can now be implemented with the available `collection: org.mongodb.scala.MongoCollection`
  def dropCollection(implicit ec: EC) = collection.drop.toFuture() map (_ => ())
  def update(utr: String, period: ReturnPeriod, value: SdilReturn)(implicit ec: EC): Future[Unit] = {
    val data = ReturnsWrapper(utr, period, value)
    domainFormat.writes(data) match {
      case j @ JsObject(_) =>
        val selector = Json.obj("utr" -> utr, "period.year" -> period.year, "period.quarter" -> period.quarter)
        collection
          .findOneAndReplace(
            filter = Filters.and(
              Filters.equal("utr", utr),
              Filters.equal("period.year", period.year),
              Filters.equal("period.quarter", period.quarter)
            ),
            replacement = data, //How to convert data to Bson
            options = FindOneAndReplaceOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
          )
          .toFuture()
          .map(_ => ())
      case _ =>
        Future.failed(new Exception("cannot write object"))
    }

  }

  def get(utr: String, period: ReturnPeriod)(implicit ec: EC): Future[Option[SdilReturn]] =
    collection
      .find(
        Filters.and(
          Filters.equal("utr", utr),
          Filters.equal("period.year", period.year),
          Filters.equal("period.quarter", period.quarter)
        ))
      .toFuture()
      .map {
        _.headOption.map { x =>
          x.sdilReturn
        }
      }

  def list(utr: String)(implicit ec: EC): Future[Map[ReturnPeriod, SdilReturn]] =
    collection
      .find(
        Filters.equal("utr", utr)
      )
      .toFuture()
      .map {
        _.map { x =>
          (x.period, x.sdilReturn)
        }.toMap
      }

  def listVariable(utr: String)(implicit ec: EC): Future[Map[ReturnPeriod, SdilReturn]] = {
    val since = LocalDate.now.minusYears(4)

    collection
      .find(
        Filters.and(
          Filters.equal("utr", utr),
          Filters.or(
            Filters.gt("period.year", since.getYear),
            Filters.and(
              Filters.equal("period.year", since.getYear),
              Filters.gte("period.quarter", ((since.getMonthValue - 1) / 3)),
            )
          )
        )
      )
      .toFuture()
      .map {
        _.map { x =>
          (x.period, x.sdilReturn)
        }.toMap
      }

  }

}
