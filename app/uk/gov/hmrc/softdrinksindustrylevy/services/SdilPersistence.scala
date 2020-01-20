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

import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.functional.syntax.unlift
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONArray, BSONDateTime, BSONDocument, BSONObjectID, BSONString}
import reactivemongo.play.json.ImplicitBSONHandlers._
import sdil.models.{ReturnPeriod, SdilReturn, SmallProducer}
import uk.gov.hmrc.mongo.{MongoConnector, ReactiveRepository}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext => EC, _}
import uk.gov.hmrc.softdrinksindustrylevy.models._
import java.time._

import cats.implicits._

trait SdilPersistence {

  protected trait DAO[U, K, V] {
    def update(user: U, key: K, value: V)(implicit ec: EC): Future[Unit]
    def get(user: U, key: K)(implicit ec: EC): Future[Option[(V, Option[BSONObjectID])]]
    def apply(user: U, key: K)(implicit ec: EC): Future[V] =
      get(user, key).map { _.get._1 }
    def list(user: U)(implicit ec: EC): Future[Map[K, V]]
    def listVariable(user: U)(implicit ec: EC): Future[Map[K, V]]
    def dropCollection(implicit ec: EC): Future[Boolean]
  }

  def returns: DAO[String, ReturnPeriod, SdilReturn]

  protected trait SubsDAO[K, V] {
    def insert(key: K, value: V)(implicit ec: EC): Future[Unit]
    def list(key: K)(implicit ec: EC): Future[List[V]]
  }

  def subscriptions: SubsDAO[String, Subscription]
}

class SdilMongoPersistence(mc: MongoConnector) extends SdilPersistence {

  val subscriptions = new SubsDAO[String, Subscription] {

    protected case class Wrapper(
      utr: String,
      subscription: Subscription,
      retrievalTime: LocalDateTime = LocalDateTime.now(),
      _id: Option[BSONObjectID] = None
    )

    import json.internal._
    implicit val formatWrapper = Json.format[Wrapper]

    override def insert(utr: String, value: Subscription)(implicit ec: EC): Future[Unit] =
      subscriptionsMongo.insert(Wrapper(utr, value)).map(_ => ())

    override def list(utr: String)(implicit ec: EC): Future[List[Subscription]] =
      subscriptionsMongo
        .find(
          "utr" -> utr
        )
        .map(_.map(_.subscription))

    def dropDb(implicit ec: EC) = subscriptionsMongo.drop

    val subscriptionsMongo =
      new ReactiveRepository[Wrapper, BSONObjectID]("sdilsubscriptions", mc.db, formatWrapper, implicitly) {
        override def indexes: Seq[Index] = Seq(
          Index(
            key = Seq(
              "utr" -> IndexType.Ascending
            ),
            unique = false
          )
        )
      }
  }

  val returns = new DAO[String, ReturnPeriod, SdilReturn] {

    protected case class Wrapper(
      utr: String,
      period: ReturnPeriod,
      sdilReturn: SdilReturn,
      _id: Option[BSONObjectID] = None)

    implicit val formatWrapper = Json.format[Wrapper]

    val returnsMongo = new ReactiveRepository[Wrapper, BSONObjectID]("sdilreturns", mc.db, formatWrapper, implicitly) {

      override def indexes: Seq[Index] = Seq(
        Index(
          key = Seq(
            "utr"            -> IndexType.Ascending,
            "period.year"    -> IndexType.Descending,
            "period.quarter" -> IndexType.Descending
          ),
          unique = true
        )
      )
    }

    def dropCollection(implicit ec: EC) = returnsMongo.drop

    def update(utr: String, period: ReturnPeriod, value: SdilReturn)(implicit ec: EC): Future[Unit] = {
      import returnsMongo._

      val data = Wrapper(utr, period, value)

      domainFormatImplicit.writes(data) match {
        case d @ JsObject(_) =>
          val selector = Json.obj("utr" -> utr, "period.year" -> period.year, "period.quarter" -> period.quarter)
          collection.update(ordered = false).one(selector, data, upsert = true)
        case _ =>
          Future.failed[WriteResult](new Exception("cannot write object"))
      }
    }.map { _ =>
      ()
    }

    def get(utr: String, period: ReturnPeriod)(implicit ec: EC): Future[Option[(SdilReturn, Option[BSONObjectID])]] =
      returnsMongo
        .find(
          "utr"            -> utr,
          "period.year"    -> period.year,
          "period.quarter" -> period.quarter
        )
        .map {
          _.headOption.map { x =>
            (x.sdilReturn, x._id)
          }
        }

    def list(utr: String)(implicit ec: EC): Future[Map[ReturnPeriod, SdilReturn]] =
      returnsMongo
        .find(
          "utr" -> utr
        )
        .map {
          _.map { x =>
            (x.period, x.sdilReturn)
          }.toMap
        }

    def listVariable(utr: String)(implicit ec: EC): Future[Map[ReturnPeriod, SdilReturn]] = {
      val since = LocalDate.now.minusYears(4)

      returnsMongo
        .find(
          "utr" -> utr,
          "$or" -> BSONArray(
            BSONDocument("period.year" -> BSONDocument("$gt" -> since.getYear)),
            BSONDocument(
              "$and" -> BSONArray(
                BSONDocument("period.year"    -> BSONDocument("$eq"  -> since.getYear)),
                BSONDocument("period.quarter" -> BSONDocument("$gte" -> (since.getMonthValue - 1) / 3))
              ))
          )
        )
        .map {
          _.map { x =>
            (x.period, x.sdilReturn)
          }.toMap
        }
    }

  }

}
