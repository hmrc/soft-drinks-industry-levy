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

import play.api.libs.json.{Format, JsResult, JsValue, Json}
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDateTime, BSONDocument, BSONObjectID}
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.mongo.{MongoConnector, ReactiveRepository}
import uk.gov.hmrc.softdrinksindustrylevy.models.{ReturnsVariationRequest, VariationsRequest}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.language.postfixOps

class VariationSubmissionService(implicit mc: MongoConnector, ec: ExecutionContext)
    extends ReactiveRepository[VariationWrapper, String]("variations", mc.db, VariationWrapper.format, implicitly) {

  def save(variation: VariationsRequest, sdilRef: String): Future[Unit] =
    insert(VariationWrapper(variation, sdilRef)) map { _ =>
      ()
    }

  def get(sdilRef: String): Future[Option[VariationsRequest]] =
    find("sdilRef" -> sdilRef).map(_.sortWith(_.timestamp isAfter _.timestamp).headOption.map(_.submission))

  override def indexes: Seq[Index] = Seq(
    Index(
      key = Seq("timestamp" -> IndexType.Ascending),
      name = Some("ttl"),
      options = BSONDocument("expireAfterSeconds" -> (90 days).toSeconds)
    )
  )
}

case class VariationWrapper(
  submission: VariationsRequest,
  sdilRef: String,
  _id: BSONObjectID = BSONObjectID.generate(),
  timestamp: Instant = Instant.now)

object VariationWrapper {
  implicit val instantFormat: Format[Instant] = new Format[Instant] {
    override def writes(o: Instant): JsValue =
      Json.toJson(BSONDateTime(o.toEpochMilli))

    override def reads(json: JsValue): JsResult[Instant] =
      json.validate[BSONDateTime] map { dt =>
        Instant.ofEpochMilli(dt.value)
      }
  }

  val format: Format[VariationWrapper] = Json.format[VariationWrapper]
}
