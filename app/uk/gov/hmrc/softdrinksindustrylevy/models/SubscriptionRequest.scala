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

case class Site(
  address: Address,
  ref: String = java.util.UUID.randomUUID.toString
) 

case class Contact(
  name: Option[String],
  positionInCompany: Option[String],
  phoneNumber: String,
  email: String
)

case class Subscription (
                          utr: String,
                          orgName: String,
                          address: Address,
                          activity: Activity,
                          liabilityDate: Date,
                          productionSites: List[Site],
                          warehouseSites: List[Site],
                          contact: Contact
)
//{
//
//  lazy val (lowerLitres,upperLitres) =
//    activity match {
//      case a:InternalActivity => a.sumOfLiableLitreRates
//      case _ => (0L, 0L)
//    }
//
//  val lowerRate: Long = 6
//  val upperRate: Long = 8
//
//  lazy val taxEstimatePence: Long =
//    lowerLitres * lowerRate + upperLitres * upperRate
//
//  lazy val taxEstimatePounds: BigDecimal =
//    BigDecimal(taxEstimatePence.toString) / 100
//
//}

/* Probably overkill */
case class CreateSubscriptionResponse(
  processingDate: LocalDateTime,
  formBundleNumber: String
)

