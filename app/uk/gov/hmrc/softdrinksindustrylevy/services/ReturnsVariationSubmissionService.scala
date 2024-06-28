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

import com.google.inject.{Inject, Singleton}
import org.mongodb.scala.model.{Filters, IndexModel, IndexOptions, Indexes}
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats.instantFormat
import uk.gov.hmrc.softdrinksindustrylevy.models.ReturnsVariationRequest

import java.time.Instant
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

@Singleton
class ReturnsVariationSubmissionService @Inject() (mongo: MongoComponent)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[ReturnsVariationWrapper](
      collectionName = "returns-variations",
      mongoComponent = mongo,
      domainFormat = ReturnsVariationWrapper.format,
      indexes = Seq(
        IndexModel(
          Indexes.ascending("timestamp"),
          IndexOptions().name("ttl").expireAfter((90 days).toSeconds, SECONDS)
        ),
        IndexModel(Indexes.ascending("sdilRef"))
      )
    ) {

  def save(v: ReturnsVariationRequest, sdilRef: String): Future[Unit] =
    collection.insertOne(ReturnsVariationWrapper(v, sdilRef)).toFuture() map { _ =>
      ()
    }

  def get(sdilRef: String): Future[Option[ReturnsVariationRequest]] =
    collection
      .find(Filters.equal("sdilRef", sdilRef))
      .toFuture()
      .map(_.sortWith(_.timestamp isAfter _.timestamp).headOption.map(_.submission))

}

case class ReturnsVariationWrapper(
  submission: ReturnsVariationRequest,
  sdilRef: String,
  timestamp: Instant = Instant.now
)

object ReturnsVariationWrapper {
  implicit val inf: Format[Instant] = instantFormat
  val format: Format[ReturnsVariationWrapper] = Json.format[ReturnsVariationWrapper]
}
