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

import play.api.libs.json.*
import sdil.models.{ReturnPeriod, SdilReturn}
import uk.gov.hmrc.softdrinksindustrylevy.models.*
import com.google.inject.{Inject, Singleton}
import com.mongodb.ErrorCategory
import org.bson.BsonType
import org.mongodb.scala.{MongoCollection, MongoWriteException}
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.model.{Filters, FindOneAndReplaceOptions, IndexModel, IndexOptions, Indexes, ReturnDocument}
import play.api.Logging
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import uk.gov.hmrc.softdrinksindustrylevy.services.SubscriptionWrap.*

import java.time.*
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.softdrinksindustrylevy.models.json.internal.*
import uk.gov.hmrc.softdrinksindustrylevy.services.ReturnsWrapper.returnsWrapperFormat

import scala.concurrent.duration.SECONDS

case class SubscriptionWrap(
  utr: String,
  subscription: Subscription,
  retrievalTime: Instant = Instant.now()
)
object SubscriptionWrap {
  implicit val subsWrapperFormat: OFormat[SubscriptionWrap] = {
    implicit val instantFmt: Format[Instant] = MongoJavatimeFormats.instantFormat
    Json.format[SubscriptionWrap]
  }
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
class SdilMongoPersistence @Inject() (
  mongoComponent: MongoComponent
)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[SubscriptionWrap](
      collectionName = "sdilsubscriptions",
      mongoComponent = mongoComponent,
      domainFormat = subsWrapperFormat,
      indexes = Seq(
        IndexModel(
          Indexes.ascending("retrievalTime"),
          IndexOptions()
            .name("sdil-subscription-cache-expiry")
            .expireAfter((30 * 24 * 60 * 60).toLong, SECONDS)
        ),
        IndexModel(
          Indexes.ascending("utr")
        )
      ),
      replaceIndexes = true
    ) with Logging {

  private val lockCollection: MongoCollection[Document] =
    mongoComponent.database.getCollection("locks")

  private val lockId = "sdil-subscription-migration-lock"

  private def ensureLockTtlIndex(): Future[Unit] =
    lockCollection
      .createIndex(
        Indexes.ascending("createdAt"),
        IndexOptions()
          .name("lock-ttl-index")
          .expireAfter(3600, SECONDS)
          .background(true)
      )
      .toFuture()
      .map(_ => ())
      .recover { case ex =>
        logger.warn("[SDIL] Failed to create TTL index on locks collection", ex)
      }

  private def acquireLock(): Future[Boolean] = {
    val lockDoc = Document("_id" -> lockId, "createdAt" -> java.util.Date.from(Instant.now()))

    lockCollection
      .insertOne(lockDoc)
      .toFuture()
      .map(_ => true)
      .recover {
        case ex: MongoWriteException if ex.getError.getCategory == ErrorCategory.DUPLICATE_KEY =>
          logger.warn("[SDIL] Migration lock already acquired. Skipping.")
          false
        case ex =>
          logger.error("[SDIL] Error acquiring migration lock", ex)
          false
      }
  }

  private def releaseLock(): Future[Unit] =
    lockCollection
      .deleteOne(Filters.equal("_id", lockId))
      .toFuture()
      .map(_ => logger.warn("[SDIL] Lock released"))
      .recover { case ex =>
        logger.warn("[SDIL] Failed to release lock", ex)
      }

  private def migrateOldRecords(): Future[Unit] =
    ensureLockTtlIndex().flatMap { _ =>
      acquireLock().flatMap {
        case true =>
          logger.warn("[SDIL] Lock acquired. Starting migration.")

          val filterOldFormat = Filters.`type`("retrievalTime", BsonType.STRING)

          val updatePipeline = List(
            Document(
              "$set" -> Document(
                "retrievalTime" -> Document("$toDate" -> "$retrievalTime")
              )
            )
          )

          collection
            .updateMany(filterOldFormat, updatePipeline)
            .toFuture()
            .map { result =>
              logger.warn(
                s"[SDIL] Migration completed: ${result.getModifiedCount} documents updated."
              )
            }
            .flatMap(_ => releaseLock())
            .recoverWith { case ex =>
              logger.error("[SDIL] Migration failed", ex)
              releaseLock().flatMap(_ => Future.failed(ex))
            }

        case false =>
          logger.warn("[SDIL] Migration already completed or in progress — skipping.")
          Future.unit
      }
    }

  private val _ = migrateOldRecords()

  // queries and updates can now be implemented with the available `collection: org.mongodb.scala.MongoCollection`
  def findAll(): Future[Seq[SubscriptionWrap]] = collection.find().toFuture()
  def insert(utr: String, value: Subscription)(implicit ec: ExecutionContext): Future[Unit] =
    collection.insertOne(SubscriptionWrap(utr, value)).toFuture().map(_ => ())
  def list(utr: String)(implicit ec: ExecutionContext): Future[List[Subscription]] =
    collection
      .find(Filters.equal("utr", utr))
      .collect()
      .toFuture()
      .map(_.map(_.subscription).toList)

}

@Singleton
class ReturnsPersistence @Inject() (
  mongoComponent: MongoComponent
)(implicit ec: ExecutionContext)
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

  override lazy val requiresTtlIndex: Boolean = false

  // queries and updates can now be implemented with the available `collection: org.mongodb.scala.MongoCollection`
  def dropCollection(implicit ec: ExecutionContext) = collection.drop().toFuture() map (_ => ())
  def update(utr: String, period: ReturnPeriod, value: SdilReturn)(implicit ec: ExecutionContext): Future[Unit] = {
    val data = ReturnsWrapper(utr, period, value)
    domainFormat.writes(data) match {
      case _ @JsObject(_) =>
        collection
          .findOneAndReplace(
            filter = Filters.and(
              Filters.equal("utr", utr),
              Filters.equal("period.year", period.year),
              Filters.equal("period.quarter", period.quarter)
            ),
            replacement = data, // How to convert data to Bson
            options = FindOneAndReplaceOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
          )
          .toFuture()
          .map(_ => ())
      case _ =>
        Future.failed(new Exception("cannot write object"))
    }

  }

  def get(utr: String, period: ReturnPeriod)(implicit ec: ExecutionContext): Future[Option[SdilReturn]] =
    collection
      .find(
        Filters.and(
          Filters.equal("utr", utr),
          Filters.equal("period.year", period.year),
          Filters.equal("period.quarter", period.quarter)
        )
      )
      .toFuture()
      .map {
        _.headOption.map { x =>
          x.sdilReturn
        }
      }

  def list(utr: String)(implicit ec: ExecutionContext): Future[Map[ReturnPeriod, SdilReturn]] =
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

  def listVariable(utr: String)(implicit ec: ExecutionContext): Future[Map[ReturnPeriod, SdilReturn]] = {
    val since = LocalDate.now.minusYears(4)

    collection
      .find(
        Filters.and(
          Filters.equal("utr", utr),
          Filters.or(
            Filters.gt("period.year", since.getYear),
            Filters.and(
              Filters.equal("period.year", since.getYear),
              Filters.gte("period.quarter", (since.getMonthValue - 1) / 3)
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
