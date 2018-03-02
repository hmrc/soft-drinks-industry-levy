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

import java.time.LocalDate
import uk.gov.hmrc.softdrinksindustrylevy.models.json.internal._

import play.api.libs.json._

object VariationsRequest {
  implicit val reads: Reads[VariationsRequest] = Json.reads[VariationsRequest]
}

case class VariationsRequest(tradingName: Option[String] = None,
                             businessContact: Option[VariationsContact] = None,
                             correspondenceContact: Option[VariationsContact] = None,
                             primaryPersonContact: Option[VariationsPersonalDetails] = None,
                             sdilActivity: Option[SdilActivity] = None)

object VariationsContact {
  implicit val reads: Reads[VariationsContact] = Json.reads[VariationsContact]
}

case class VariationsContact(addressLine1: Option[String] = None,
                             addressLine2: Option[String] = None,
                             addressLine3: Option[String] = None,
                             addressLine4: Option[String] = None,
                             postCode: Option[String] = None,
                             telephoneNumber: Option[String] = None,
                             emailAddress: Option[String] = None)

object VariationsPersonalDetails {
  implicit val reads: Reads[VariationsPersonalDetails] = Json.reads[VariationsPersonalDetails]
}

case class VariationsPersonalDetails(
                                      name: Option[String],
                                      position: Option[String],
                                      telephoneNumber: Option[String],
                                      emailAddress: Option[String]
                                    )

object SdilActivity {

  implicit val reads: Reads[SdilActivity] = Json.reads[SdilActivity]
}

case class SdilActivity(
                         activity: Activity,
                         produceLessThanOneMillionLitres: Option[Boolean],
                         smallProducerExemption: Option[Boolean], //If true then the user does not have to file returns
                         usesContractPacker: Option[Boolean],
                         voluntarilyRegistered: Option[Boolean],
                         reasonForAmendment: Option[String],
                         estimatedTaxAmount: Option[BigDecimal],
                         taxObligationStartDate: Option[LocalDate]
                       )