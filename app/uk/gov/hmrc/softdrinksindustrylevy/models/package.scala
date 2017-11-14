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

package uk.gov.hmrc.softdrinksindustrylevy

import play.api.libs.json._

package object models {

  // SDIL create and retrieve subscription formatters
  implicit val addressFormat: OFormat[Address] = Json.format[Address]
  implicit val contactDetailsFormat: OFormat[ContactDetails] = Json.format[ContactDetails]
  implicit val businessContactFormat: OFormat[BusinessContact] = Json.format[BusinessContact]
  implicit val correspondenceContactFormat: OFormat[CorrespondenceContact] = Json.format[CorrespondenceContact]
  implicit val primaryContactFormat: OFormat[PrimaryPersonContact] = Json.format[PrimaryPersonContact]
  implicit val litresProducedFormat: OFormat[LitresProduced] = Json.format[LitresProduced]
  implicit val producerDetailsFormat: OFormat[ProducerDetails] = Json.format[ProducerDetails]
  implicit val detailsFormat: OFormat[Details] = Json.format[Details]
  implicit val siteFormat: OFormat[Site] = Json.format[Site]
  implicit val registrationFormat: OFormat[SubscriptionRequest] = Json.format[SubscriptionRequest]
  implicit val entityActionFormat: OFormat[EntityAction] = Json.format[EntityAction]
  implicit val createSubscriptionRequestFormat: OFormat[CreateSubscriptionRequest] = Json.format[CreateSubscriptionRequest]
  implicit val createSubscriptionResponseFormat: OFormat[CreateSubscriptionResponse] = Json.format[CreateSubscriptionResponse]

}
