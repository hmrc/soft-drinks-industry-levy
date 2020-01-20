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

package uk.gov.hmrc.softdrinksindustrylevy.models

import cats.kernel.Monoid
import cats.syntax.semigroup._

object ActivityType extends Enumeration {
  val ProducedOwnBrand, Imported, CopackerAll, Copackee, Exporting, Wastage = Value
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
  override def taxEstimation: BigDecimal =
    0 // lost in translation - we should either hide or say something like unknown but it is
  // not optional
}

case class InternalActivity(activity: Map[ActivityType.Value, LitreBands], isLarge: Boolean) extends Activity {

  import ActivityType._

  def totalLiableLitres: LitreBands =
    Monoid.combineAll(
      Seq(
        activity.get(ProducedOwnBrand),
        activity.get(Imported),
        activity.get(CopackerAll)
      ).flatten)

  lazy val totalProduced: Option[LitreBands] = {
    (activity.get(ProducedOwnBrand), activity.get(Copackee)) match {
      case (Some(p), Some(c)) => Some(p |+| c)
      case (Some(pob), None)  => Some(pob)
      case (None, Some(c))    => Some(c)
      case (None, None)       => None
    }
  }

  def isProducer: Boolean = activity.contains(ProducedOwnBrand) || activity.contains(Copackee) || isLarge

  def isContractPacker: Boolean = activity.keySet.contains(CopackerAll)

  def isImporter: Boolean = activity.keySet.contains(Imported)

  override def taxEstimation: BigDecimal =
    if (isSmallProducer && !isContractPacker && !isImporter) 0
    else {
      val biggestNumberThatETMPCanHandle = BigDecimal("99999999999.99")
      totalLiableLitres.dueLevy.min(biggestNumberThatETMPCanHandle)
    }
}
