/*
 * Copyright 2017 HM Revenue & Customs
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
  val ProducedOwnBrand, Imported, CopackerAll, CopackerSmall, Copackee = Value
}

sealed trait Activity {
  def isProducer: Boolean
  def isLarge: Boolean
  def isContractPacker: Boolean
  def isImporter: Boolean
  def isVoluntaryRegistration: Boolean = isProducer && !isLarge && !isImporter && !isContractPacker // TODO check logic
  def isSmallProducer: Boolean = isProducer && !isLarge // TODO check logic
  def taxEstimation: String
}

case class RetrievedActivity(isProducer: Boolean, isLarge: Boolean, isContractPacker: Boolean, isImporter: Boolean)
  extends Activity {
  override def taxEstimation = "unknown"
}

case class InternalActivity (activity: Map[ActivityType.Value, LitreBands]) extends Activity {
  import ActivityType._

  val lowerRate: Long = 6
  val upperRate: Long = 8

  val add: (Litres, Litres) = activity
    .filter(x => List(ProducedOwnBrand,Copackee).contains(x._1)) // TODO check logic
    .values.foldLeft((0L,0L)){
      case ((aL,aH), (pL,pH)) => (aL+pL, aH+pH)
    }

  def sumOfLiableLitreRates: LitreBands = {
    activity.get(CopackerSmall).fold(add) {
      subtract => (add._1 - subtract._1, add._2 - subtract._2)
    }
  }

  def isProducer: Boolean = {
    List(ProducedOwnBrand, Copackee).
      foldLeft(false){ case (acc, t) =>
        acc || activity.contains(t)
      }
  }

  def isLarge: Boolean = sumOfLiableLitreRates._1 + sumOfLiableLitreRates._2 >= 1000000
  def isContractPacker: Boolean = activity.keySet.contains(CopackerAll)
  def isImporter: Boolean = activity.keySet.contains(Imported)

  override def taxEstimation: String = {
    BigDecimal(sumOfLiableLitreRates._1 * upperRate + sumOfLiableLitreRates._2 * lowerRate) / 100 toString()
  }
}

