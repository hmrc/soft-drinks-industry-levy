package uk.gov.hmrc.softdrinksindustrylevy.models

import play.api.libs.json.{Format, Json}

case class DisplayDirectDebitResponse(directDebitMandateFound: Boolean)

object DisplayDirectDebitResponse {
  implicit val format: Format[DisplayDirectDebitResponse] = Json.format[DisplayDirectDebitResponse]
}