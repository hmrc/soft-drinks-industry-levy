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

case class VariationsRequest(
                              tradingName: Option[String] = None,
                              businessContact: Option[VariationsContact] = None,
                              correspondenceContact: Option[VariationsContact] = None,
                              primaryPersonContact: Option[VariationsPersonalDetails] = None,
                              sdilActivity: Option[SdilActivity] = None,
                              deregistrationText: Option[String] = None,
                              newSites: List[VariationsSite] = Nil,
                              amendSites: List[VariationsSite] = Nil,
                              closeSites: List[CloseSites] = Nil
                            )

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
                                      name: Option[String] = None,
                                      position: Option[String] = None,
                                      telephoneNumber: Option[String] = None,
                                      emailAddress: Option[String] = None
                                    )

object SdilActivity {

  implicit val reads: Reads[SdilActivity] = Json.reads[SdilActivity]
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
  implicit val reads: Reads[VariationsSite] = Json.reads[VariationsSite]
}

case class VariationsSite(
                           tradingName: String,
                           siteReference: String,
                           variationsContact: VariationsContact,
                           typeOfSite: String
                         )

object CloseSites {
  implicit val reads: Reads[CloseSites] = Json.reads[CloseSites]
}

case class CloseSites(
                       tradingName: String,
                       siteReference: String,
                       reasonOfClosure: String
                     )