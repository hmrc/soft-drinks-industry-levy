/*
 * Copyright 2026 HM Revenue & Customs
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

import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.softdrinksindustrylevy.*

import java.time.LocalDate

final case class HipRetrieveSubscriptionDetailsResponse(success: HipSubscription)

final case class HipSubscription(utr: String,
                                 subscriptionDetails: HipSubscriptionDetails,
                                 businessAddress: HipAddress,
                                 sites: List[HipSite] = Nil)

final case class HipSubscriptionDetails(sdilRegistrationNumber: String,
                                        taxObligationStartDate: LocalDate,
                                        taxObligationEndDate: Option[LocalDate],
                                        tradingName: String,
                                        deregistrationDate: Option[LocalDate],
                                        voluntaryRegistration: Boolean,
                                        smallProducer: Boolean,
                                        largeProducer: Boolean,
                                        contractPacker: Boolean,
                                        importer: Boolean,
                                        primaryContactName: Option[String],
                                        primaryPositionInCompany: Option[String],
                                        primaryTelephone: String,
                                        primaryEmail: String)

final case class HipAddress(line1: String,
                            line2: Option[String],
                            line3: Option[String],
                            line4: Option[String],
                            postCode: Option[String],
                            country: Option[String])

final case class HipSite(siteReference: Option[String],
                         tradingName: Option[String],
                         siteAddress: HipAddress,
                         closureDate: Option[LocalDate],
                         siteType: String)

object HipRetrieveSubscriptionDetailsResponse {
  implicit val format: Format[HipRetrieveSubscriptionDetailsResponse] = Json.format
}

object HipSubscription {
  implicit val format: Format[HipSubscription] = Json.format
}

object HipSubscriptionDetails {
  implicit val format: Format[HipSubscriptionDetails] = Json.format
}

object HipAddress {
  implicit val format: Format[HipAddress] = Json.format
}

object HipSite {
  implicit val format: Format[HipSite] = Json.format
}
