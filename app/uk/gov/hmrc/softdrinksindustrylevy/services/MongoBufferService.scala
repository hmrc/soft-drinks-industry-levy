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

import java.time.{Instant, LocalDateTime}
import play.api.libs.json._
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.softdrinksindustrylevy.models.Subscription
import uk.gov.hmrc.softdrinksindustrylevy.models.json.internal._
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats.instantFormat
import uk.gov.hmrc.softdrinksindustrylevy.services.ReturnsWrapper.returnsWrapperFormat
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import com.google.inject.{Inject, Singleton}
import org.mongodb.scala.model.{Filters, IndexModel, IndexOptions, Indexes, Updates}
import org.mongodb.scala.result.DeleteResult
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats.{Implicits, instantFormat, instantReads, instantWrites, localDateTimeReads, localDateTimeWrites}
@Singleton
class MongoBufferService @Inject()(
  mongoComponent: MongoComponent
)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[SubscriptionWrapper](
      collectionName = "sdil-subscription",
      mongoComponent = mongoComponent,
      domainFormat = SubscriptionWrapper.format,
      indexes = Seq(
        IndexModel(
          Indexes.ascending("timestamp"),
          IndexOptions().name("ttl").expireAfter((30 days).toSeconds, SECONDS)),
        IndexModel(Indexes.ascending("status")),
        IndexModel(Indexes.ascending("utr"))
      )
    ) {
  // queries and updates can now be implemented with the available `collection: org.mongodb.scala.MongoCollection`
  def updateStatus(id: String, newStatus: String)(implicit ec: ExecutionContext): Future[Unit] =
    collection
      .findOneAndUpdate(
        filter = Filters.equal("_id", id),
        update = Updates.set("status", newStatus)
      )
      .toFuture() map { _ =>
      ()
    }

  def findOverdue(createdBefore: Instant)(implicit ec: ExecutionContext): Future[Seq[SubscriptionWrapper]] =
    collection
      .find(
        Filters.and(
          Filters.equal("status", "PENDING"),
          Filters.lt("timestamp", createdBefore)
        ))
      .toFuture()

  def insert(sub: SubscriptionWrapper)(implicit ec: ExecutionContext): Future[Unit] =
    collection.insertOne(sub).toFuture() map (_ => ())

  def findByUtr(utr: String)(implicit ec: ExecutionContext): Future[Seq[SubscriptionWrapper]] =
    collection.find(Filters.equal("utr", utr)).toFuture()

  def remove(utr: String)(implicit ec: ExecutionContext): Future[DeleteResult] =
    collection.deleteOne(Filters.equal("utr", utr)).toFuture()

  def findById(id: String)(implicit ec: ExecutionContext): Future[SubscriptionWrapper] =
    collection.find(Filters.equal("_id", id)).toFuture() map (_.head)

  def removeById(id: String)(implicit ec: ExecutionContext): Future[DeleteResult] =
    collection.deleteOne(Filters.equal("_id", id)).toFuture()

}

case class SubscriptionWrapper(
  _id: String,
  subscription: Subscription,
  formBundleNumber: String,
  timestamp: Instant = Instant.now,
  status: String = "PENDING")

object SubscriptionWrapper {
  implicit val subFormat: Format[Subscription] = Format(subReads, subWrites)
  implicit val inf = instantFormat

  val format: Format[SubscriptionWrapper] = Json.format[SubscriptionWrapper]
}
