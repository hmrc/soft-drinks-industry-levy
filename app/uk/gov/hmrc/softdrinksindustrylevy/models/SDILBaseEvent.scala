package uk.gov.hmrc.softdrinksindustrylevy.models

import play.api.libs.json.JsValue
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

abstract class SDILBaseEvent(auditType: String,
                             transactionName: String,
                             path: String,
                             detailJson: JsValue)(implicit hc: HeaderCarrier)
  extends ExtendedDataEvent(
    auditSource = "soft-drinks-industry-levy",
    auditType = auditType,
    detail = detailJson,
    tags = hc.toAuditTags(transactionName, path)
  )

class SdilSubscriptionEvent(path: String, detailJson: JsValue)(implicit hc: HeaderCarrier)
  extends SDILBaseEvent(
    auditType = "submitSDILSubscription",
    transactionName = "Soft Drinks Industry Levy subscription submitted",
    path = path,
    detailJson = detailJson)

class TaxEnrolmentEvent(enrolmentStatus: String, path: String, detailJson: JsValue)(implicit hc: HeaderCarrier)
  extends SDILBaseEvent(
    auditType = enrolmentStatus,
    transactionName = s"Soft Drinks Industry Levy subscription ${enrolmentStatus.toLowerCase}",
    path = path,
    detailJson = detailJson
  )