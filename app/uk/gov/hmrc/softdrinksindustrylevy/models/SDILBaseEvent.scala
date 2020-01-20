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

import play.api.libs.json.JsValue
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

abstract class SDILBaseEvent(auditType: String, transactionName: String, path: String, detailJson: JsValue)(
  implicit hc: HeaderCarrier)
    extends ExtendedDataEvent(
      auditSource = "soft-drinks-industry-levy",
      auditType = auditType,
      detail = detailJson,
      tags = hc.toAuditTags(transactionName, path)
    )

class SdilSubscriptionEvent(path: String, detailJson: JsValue)(implicit hc: HeaderCarrier)
    extends SDILBaseEvent(
      auditType = "SDILRegistrationSubmitted",
      transactionName = "Soft Drinks Industry Levy subscription submitted",
      path = path,
      detailJson = detailJson)

class TaxEnrolmentEvent(enrolmentStatus: String, path: String, detailJson: JsValue)(implicit hc: HeaderCarrier)
    extends SDILBaseEvent(
      auditType = "SDILEnrolment",
      transactionName = s"Soft Drinks Industry Levy subscription ${enrolmentStatus.toLowerCase}",
      path = path,
      detailJson = detailJson)

class SdilReturnEvent(path: String, detailJson: JsValue)(implicit hc: HeaderCarrier)
    extends SDILBaseEvent(
      auditType = "SDILReturnSubmitted",
      transactionName = "Soft Drinks Industry Levy return submitted",
      path = path,
      detailJson = detailJson)

class BalanceQueryEvent(path: String, detail: JsValue)(implicit headerCarrier: HeaderCarrier)
    extends SDILBaseEvent(
      auditType = "SDILBalanceQuery",
      transactionName = "Soft Drinks Industry Levy balance requested",
      path = path,
      detailJson = detail)
