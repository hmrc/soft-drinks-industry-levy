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

import java.time.{LocalDate, LocalDateTime}

import play.api.libs.json.Json

case class Site(address: Address, ref: Option[String], tradingName: Option[String], closureDate: Option[LocalDate])

case class Contact(
  name: Option[String],
  positionInCompany: Option[String],
  phoneNumber: String,
  email: String
)

case class Subscription(
  utr: String,
  sdilRef: Option[String],
  orgName: String,
  orgType: Option[String],
  address: Address,
  activity: Activity,
  liabilityDate: LocalDate,
  productionSites: List[Site],
  warehouseSites: List[Site],
  contact: Contact,
  endDate: Option[LocalDate],
  deregDate: Option[LocalDate] = None) {

  def isDeregistered: Boolean = deregDate.fold(false) { x =>
    x.isBefore(LocalDate.now) || x.isEqual(LocalDate.now)
  }
}

/* Probably overkill */
case class CreateSubscriptionResponse(
  processingDate: LocalDateTime,
  formBundleNumber: String
)
