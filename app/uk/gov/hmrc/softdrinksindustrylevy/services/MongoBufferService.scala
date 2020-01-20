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

import java.time.Instant

import play.api.libs.json._
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDateTime, BSONDocument, BSONString}
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.mongo.{MongoConnector, ReactiveRepository}
import uk.gov.hmrc.softdrinksindustrylevy.models.Subscription
import uk.gov.hmrc.softdrinksindustrylevy.models.json.internal._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class MongoBufferService(implicit mc: MongoConnector)
    extends ReactiveRepository[SubscriptionWrapper, String](
      "sdil-subscription",
      mc.db,
      SubscriptionWrapper.format,
      implicitly) {

  def updateStatus(id: String, newStatus: String)(implicit ec: ExecutionContext): Future[Unit] =
    collection.findAndUpdate(
      BSONDocument("_id"  -> BSONString(id)),
      BSONDocument("$set" -> BSONDocument("status" -> BSONString(newStatus)))
    ) map { _ =>
      ()
    }

  def findOverdue(createdBefore: Instant)(implicit ec: ExecutionContext): Future[Seq[SubscriptionWrapper]] = {
    val bsonDt = BSONDateTime(createdBefore.toEpochMilli)
    find("status" -> "PENDING", "timestamp" -> Json.obj("$lt" -> bsonDt))
  }

  override def indexes: Seq[Index] = Seq(
    Index(
      key = Seq("timestamp" -> IndexType.Ascending),
      name = Some("ttl"),
      options = BSONDocument("expireAfterSeconds" -> (30 days).toSeconds)
    )
  )
}

case class SubscriptionWrapper(
  _id: String,
  subscription: Subscription,
  formBundleNumber: String,
  timestamp: Instant = Instant.now,
  status: String = "PENDING")

object SubscriptionWrapper {
  implicit val subFormat: Format[Subscription] = Format(subReads, subWrites)

  implicit val instantFormat: Format[Instant] = new Format[Instant] {
    override def writes(o: Instant): JsValue =
      Json.toJson(BSONDateTime(o.toEpochMilli))

    override def reads(json: JsValue): JsResult[Instant] =
      json.validate[BSONDateTime] map { dt =>
        Instant.ofEpochMilli(dt.value)
      }
  }

  val format: Format[SubscriptionWrapper] = Json.format[SubscriptionWrapper]
}
