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

package uk.gov.hmrc.softdrinksindustrylevy

import cats.kernel.Monoid
import cats.implicits._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.functional.syntax.unlift
import reactivemongo.bson.BSONObjectID
import sdil.models._

package object models {

  type Litres = Long
  type LitreBands = (Litres, Litres)

  // TODO remove this and use cats instead
  implicit val litreBandsMonoid: Monoid[(LitreBands)] = new Monoid[(LitreBands)] {
    override def empty: (Litres, Litres) = (0, 0)

    override def combine(x: (Litres, Litres), y: (Litres, Litres)): (Litres, Litres) = (x._1 + y._1, x._2 + y._2)
  }

  implicit class LitreOps(litreBands: LitreBands) {
    lazy val lowLevy: BigDecimal = litreBands._1 * BigDecimal("0.18")
    lazy val highLevy: BigDecimal = litreBands._2 * BigDecimal("0.24")
    lazy val dueLevy: BigDecimal = lowLevy + highLevy
  }

  // cos coupling is bad, mkay
  implicit val longTupleFormatter: Format[(Long, Long)] = (
    (JsPath \ "lower").format[Long] and
      (JsPath \ "higher").format[Long]
  )((a: Long, b: Long) => (a, b), unlift({ x: (Long, Long) =>
    Tuple2.unapply(x)
  }))

  implicit val ukAddressFormat: OFormat[UkAddress] = Json.format[UkAddress]
  implicit val formatSP: OFormat[SmallProducer] = Json.format[SmallProducer]
  implicit val formatReturn: OFormat[SdilReturn] = Json.format[SdilReturn]
  implicit val formatPeriod: OFormat[ReturnPeriod] = Json.format[ReturnPeriod]
  implicit val formatReturnVariationData: OFormat[ReturnVariationData] = Json.format[ReturnVariationData]

  implicit def optFormatter[A](implicit innerFormatter: Format[A]): Format[Option[A]] =
    new Format[Option[A]] {
      def reads(json: JsValue): JsResult[Option[A]] = json match {
        case JsNull => JsSuccess(none[A])
        case a      => innerFormatter.reads(a).map { _.some }
      }
      def writes(o: Option[A]): JsValue =
        o.map { innerFormatter.writes }.getOrElse(JsNull)
    }

}
