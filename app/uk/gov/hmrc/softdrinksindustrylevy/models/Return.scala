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

package sdil.models

import cats.Order
import cats.implicits._
import java.time.LocalDateTime
import java.time.LocalDate

import play.api.libs.json.{Format, Json, OFormat}
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.softdrinksindustrylevy.models.{litreBandsMonoid => _, _}

case class ReturnVariationData(
  original: SdilReturn,
  revised: SdilReturn,
  period: ReturnPeriod,
  orgName: String,
  address: UkAddress,
  reason: String,
  repaymentMethod: Option[String]
) {

  def changedLitreages: Map[String, (Long, Long)] = original.compare(revised)
  def removedSmallProducers: List[SmallProducer] = original.packSmall.filterNot(revised.packSmall.toSet)
  def addedSmallProducers: List[SmallProducer] = revised.packSmall.filterNot(original.packSmall.toSet)
}
object ReturnVariationData {
  implicit val format: Format[ReturnVariationData] = Json.format[ReturnVariationData]
}

case class SdilReturn(
  ownBrand: (Long, Long) = (0, 0),
  packLarge: (Long, Long) = (0, 0),
  packSmall: List[SmallProducer] = Nil, // zero charge
  importSmall: (Long, Long) = (0, 0), // zero charge
  importLarge: (Long, Long) = (0, 0),
  export: (Long, Long) = (0, 0), // negative charge
  wastage: (Long, Long) = (0, 0), // negative charge
  submittedOn: Option[LocalDateTime]
) {
  private def toLongs: List[(Long, Long)] = List(ownBrand, packLarge, importSmall, importLarge, export, wastage)
  private val keys = List("ownBrand", "packLarge", "importSmall", "importLarge", "export", "wastage")
  def compare(other: SdilReturn): Map[String, (Long, Long)] = {
    val y = this.toLongs
    other.toLongs.zipWithIndex
      .filter { x =>
        x._1 != y(x._2)
      }
      .map(x => keys(x._2) -> x._1)
      .toMap
  }
  private def sumLitres(l: List[(Long, Long)]) = l.map(x => LitreOps(x).dueLevy).sum
  def total: BigDecimal =
    sumLitres(List(ownBrand, packLarge, importLarge)) - sumLitres(List(export, wastage))
}
object SdilReturn {
  // TODO extract to config
  val costLower = BigDecimal("0.18")
  val costHigher = BigDecimal("0.24")
}

case class ReturnPeriod(year: Int, quarter: Int) {
  require(quarter <= 3 && quarter >= 0)
  require(year >= 2018)
  def start: LocalDate = LocalDate.of(year, quarter * 3 + 1, if (count == 0) 5 else 1)
  def end: LocalDate = next.start.minusDays(1)
  def deadline: LocalDate = end.plusDays(30)
  def next: ReturnPeriod = ReturnPeriod(count + 1)
  def previous: ReturnPeriod = ReturnPeriod(count - 1)
  def count: Int = year * 4 + quarter - 2018 * 4 - 1

  def desPeriodKey: String = s"${year % 100}C${quarter + 1}"

  def pretty: String = {
    def q: String = quarter match {
      case 0 => s"January to March"
      case 1 => s"April to June"
      case 2 => s"July to September"
      case 3 => s"October to December"
    }
    s"$q $year (${year % 100}C${quarter + 1})"
  }

}

object ReturnPeriod {
  def apply(o: Int): ReturnPeriod = {
    val i = o + 1
    ReturnPeriod(2018 + i / 4, i % 4)
  }
  def apply(date: LocalDate): ReturnPeriod = ReturnPeriod(date.getYear, quarter(date))
  def quarter(date: LocalDate): Int = { date.getMonthValue - 1 } / 3

  def fromPeriodKey(in: String): ReturnPeriod = {
    val (y :: q :: _) = in.split("C").toList
    ReturnPeriod(2000 + y.toInt, q.toInt - 1)
  }

}
