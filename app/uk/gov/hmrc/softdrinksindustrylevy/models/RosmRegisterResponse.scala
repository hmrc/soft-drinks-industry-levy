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

import play.api.libs.json.Json
import java.text.Normalizer

case class OrganisationResponse(
  organisationName: String
)

object OrganisationResponse {
  implicit val organisationResponseFormat = Json.format[OrganisationResponse]
}

case class IndividualResponse(firstName: String, lastName: String)

object IndividualResponse {

  import play.api.libs.json._

  implicit val format: Format[IndividualResponse] = Json.format[IndividualResponse]
}

abstract case class RosmResponseAddress private[models] (
  addressLine1: String,
  addressLine2: Option[String],
  addressLine3: Option[String],
  addressLine4: Option[String],
  countryCode: String,
  postalCode: String
)

object RosmResponseAddress {
  implicit val rosmResponseAddressFormat = Json.format[RosmResponseAddress]

  /** ROSM schemas permit any characters, but the SDIL schemas are more
    * restrictive. Convert all non-standard latin characters to their standard
    * equivalent (e.g. 'é' becomes 'e') and remove any non-permitted symbols
    * such as parenthesis */
  implicit class RichStr(i: String) {
    def clean =
      Normalizer
        .normalize(i, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .replaceAll("[^A-Za-z0-9 \\-,.&'\\/]", "")
  }

  def apply(
    addressLine1: String,
    addressLine2: Option[String],
    addressLine3: Option[String],
    addressLine4: Option[String],
    countryCode: String,
    postalCode: String
  ): RosmResponseAddress =
    new RosmResponseAddress(
      addressLine1.clean,
      addressLine2.map(_.clean),
      addressLine3.map(_.clean),
      addressLine4.map(_.clean),
      countryCode,
      postalCode
    ) {}
}

case class RosmResponseContactDetails(
  primaryPhoneNumber: Option[String],
  secondaryPhoneNumber: Option[String],
  faxNumber: Option[String],
  emailAddress: Option[String]
)

object RosmResponseContactDetails {
  implicit val rosmResponseContactDetailsFormat = Json.format[RosmResponseContactDetails]
}

case class RosmRegisterResponse(
  safeId: String,
  agentReferenceNumber: Option[String],
  isEditable: Boolean,
  isAnAgent: Boolean,
  isAnIndividual: Boolean,
  organisation: Option[OrganisationResponse],
  individual: Option[IndividualResponse],
  address: RosmResponseAddress,
  contactDetails: RosmResponseContactDetails
)

object RosmRegisterResponse {
  implicit val rosmRegisterResponseFormat = Json.format[RosmRegisterResponse]
}
