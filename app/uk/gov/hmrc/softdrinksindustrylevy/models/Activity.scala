/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.softdrinksindustrylevy.models

object ActivityType extends Enumeration {
  val ProducedOwnBrand, Imported, CopackerAll, Copackee, Exported, Wastage = Value
}

sealed trait Activity {
  def isProducer: Boolean
  def isLarge: Boolean
  def isContractPacker: Boolean
  def isImporter: Boolean
  def isVoluntaryRegistration: Boolean = isProducer && !isLarge && !isImporter && !isContractPacker
  def isSmallProducer: Boolean = isProducer && !isLarge
  def taxEstimation: BigDecimal
}

case class RetrievedActivity(isProducer: Boolean, isLarge: Boolean, isContractPacker: Boolean, isImporter: Boolean)
  extends Activity {
  override def taxEstimation: BigDecimal = 0 // lost in translation - we should either hide or say something like unknown but it is
  // not optional
}

case class InternalActivity(activity: Map[ActivityType.Value, LitreBands], isLarge: Boolean) extends Activity {

  import ActivityType._

  private lazy val lowerRate: BigDecimal = BigDecimal("0.18")
  private lazy val upperRate: BigDecimal = BigDecimal("0.24")

  def totalLiableLitres: LitreBands = {
    val zero: LitreBands = (0, 0)

    Seq(
      totalProduced.getOrElse(zero),
      activity.getOrElse(Imported, zero),
      activity.getOrElse(CopackerAll, zero)
    ).foldLeft(zero) { case ((accLow, accHigh), (low, high)) => (accLow + low, accHigh + high) }
  }

  lazy val totalProduced: Option[LitreBands] = {
    (activity.get(ProducedOwnBrand), activity.get(Copackee)) match {
      case (Some(pob), Some(c)) => Some((pob._1 + c._1, pob._2 + c._2))
      case (Some(pob), None) => Some(pob)
      case (None, Some(c)) => Some(c)
      case (None, None) => None
    }
  }

  def isProducer: Boolean = activity.contains(ProducedOwnBrand) || activity.contains(Copackee)

  def isContractPacker: Boolean = activity.keySet.contains(CopackerAll)

  def isImporter: Boolean = activity.keySet.contains(Imported)

  override def taxEstimation: BigDecimal = {
    val estimate = totalLiableLitres._1 * lowerRate + totalLiableLitres._2 * upperRate
    val biggestNumberThatETMPCanHandle = BigDecimal("99999999999.99")

    if (estimate > biggestNumberThatETMPCanHandle) {
      biggestNumberThatETMPCanHandle
    } else {
      estimate
    }
  }
}

