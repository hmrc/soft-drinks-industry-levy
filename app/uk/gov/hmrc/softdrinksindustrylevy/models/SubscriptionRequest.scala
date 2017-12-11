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

import java.time.{LocalDateTime, LocalDate => Date}

import play.api.libs.json.Format

case class Site(
  address: Address,
  ref: String = java.util.UUID.randomUUID.toString
) 

object ActivityType extends Enumeration {
  val ProducedOwnBrand, Imported, CopackerAll, CopackerSmall, Copackee = Value
}


sealed trait BetterActivity {
  def isProducer: Boolean
  def isLarge: Boolean
  def isContractPacker: Boolean
  def isImporter: Boolean
}

case class RetrievedActivity(isProducer: Boolean, isLarge: Boolean, isContractPacker: Boolean, isImporter: Boolean) extends BetterActivity

case class InternalActivity (activity: Map[ActivityType.Value, LitreBands]) extends BetterActivity {
  import ActivityType._
  // TODO check the logic then refactor the code as it's messy
  def isProducer: Boolean = {
    List(ProducedOwnBrand, Copackee).
      foldLeft(false){ case (acc, t) =>
        acc || activity.contains(t)
      }
  }
  private val excludedActivityTypes = List(Imported, CopackerSmall, CopackerAll)
  private val add = activity.filter(x => !excludedActivityTypes.contains(x._1)).values.foldLeft((0L,0L)){
    case ((aL,aH), (pL,pH)) => (aL+pL, aH+pH)
  }
  private val subtract = activity.get(CopackerSmall)
  def isLarge = (add._1 + add._2) - (subtract.get._1 + subtract.get._2) >= 1000000

  def isContractPacker = activity.keySet.contains(CopackerAll)
  def isImporter = activity.keySet.contains(Imported)
}

case class Contact(
  name: Option[String],
  positionInCompany: Option[String], // could be made optional
  phoneNumber: String,
  email: String
)

case class Subscription (
  utr: String,
  orgName: String,
  address: Address,
  activity: BetterActivity,
  liabilityDate: Date,
  productionSites: List[Site],
  warehouseSites: List[Site],
  contact: Contact
) {
  lazy val (lowerLitres,upperLitres) =
    activity match {
      case InternalActivity(a) => a.values.foldLeft((0L,0L)){
        case ((aL,aH), (pL,pH)) => (aL+pL, aH+pH) // TODO get Adam to tell luke he's wrong as this adds all the activityTypes LitreBands
      }
      case _ => (0L, 0L) // this info isn't in the retrieved data so we won't be able to give them an estimate
    }

  val lowerRate: Long = 6
  val upperRate: Long = 8

  lazy val taxEstimatePence: Long =
    lowerLitres * lowerRate + upperLitres * upperRate

  lazy val taxEstimatePounds: BigDecimal =
    BigDecimal(taxEstimatePence.toString) / 100
  
}

/* Probably overkill */
case class CreateSubscriptionResponse(
  processingDate: LocalDateTime,
  formBundleNumber: String
)

