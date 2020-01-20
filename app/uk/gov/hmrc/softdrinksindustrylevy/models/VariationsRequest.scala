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

import java.time.LocalDate

import uk.gov.hmrc.softdrinksindustrylevy.models.json.internal.{activityMapFormat, siteFormat}
import play.api.libs.json._

object VariationsRequest {
  implicit val formatAddress: Format[UkAddress] = Json.format[UkAddress]
  implicit val format: Format[VariationsRequest] = Json.format[VariationsRequest]
}

case class ReturnsVariationRequest(
  orgName: String,
  ppobAddress: UkAddress,
  importer: (Boolean, (Long, Long)) = (false, (0, 0)),
  packer: (Boolean, (Long, Long)) = (false, (0, 0)),
  warehouses: List[Site] = Nil,
  packingSites: List[Site] = Nil,
  phoneNumber: String,
  email: String,
  taxEstimation: BigDecimal)
object ReturnsVariationRequest {
  implicit val ukAddressFormat: Format[UkAddress] = Json.format[UkAddress]
  implicit val bllFormat: Format[(Boolean, (Long, Long))] = Json.format[(Boolean, (Long, Long))]
  implicit val format: Format[ReturnsVariationRequest] = Json.format[ReturnsVariationRequest]
}

case class VariationsRequest(
  tradingName: Option[String] = None,
  displayOrgName: String,
  ppobAddress: UkAddress,
  businessContact: Option[VariationsContact] = None,
  correspondenceContact: Option[VariationsContact] = None,
  primaryPersonContact: Option[VariationsPersonalDetails] = None,
  sdilActivity: Option[SdilActivity] = None,
  deregistrationText: Option[String] = None,
  deregistrationDate: Option[LocalDate] = None,
  newSites: List[VariationsSite] = Nil,
  amendSites: List[VariationsSite] = Nil,
  closeSites: List[CloseSites] = Nil
)

object VariationsContact {
  implicit val format: Format[VariationsContact] = Json.format[VariationsContact]
}

case class VariationsContact(
  addressLine1: Option[String] = None,
  addressLine2: Option[String] = None,
  addressLine3: Option[String] = None,
  addressLine4: Option[String] = None,
  postCode: Option[String] = None,
  telephoneNumber: Option[String] = None,
  emailAddress: Option[String] = None)

object VariationsPersonalDetails {
  implicit val format: Format[VariationsPersonalDetails] = Json.format[VariationsPersonalDetails]
}

case class VariationsPersonalDetails(
  name: Option[String] = None,
  position: Option[String] = None,
  telephoneNumber: Option[String] = None,
  emailAddress: Option[String] = None
)

object SdilActivity {

  implicit val format: Format[SdilActivity] = Json.format[SdilActivity]
}

case class SdilActivity(
  activity: Option[Activity],
  produceLessThanOneMillionLitres: Option[Boolean] = None,
  smallProducerExemption: Option[Boolean] = None, //If true then the user does not have to file returns
  usesContractPacker: Option[Boolean] = None,
  voluntarilyRegistered: Option[Boolean] = None,
  reasonForAmendment: Option[String] = None,
  taxObligationStartDate: Option[LocalDate] = None
)

object VariationsSite {
  implicit val format: Format[VariationsSite] = Json.format[VariationsSite]
}

case class VariationsSite(
  tradingName: String,
  siteReference: String,
  variationsContact: VariationsContact,
  typeOfSite: String
)

object CloseSites {
  implicit val format: Format[CloseSites] = Json.format[CloseSites]
}

case class CloseSites(
  tradingName: String,
  siteReference: String,
  reasonOfClosure: String
)
