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

import sdil.models.ReturnPeriod
import play.api.libs.functional.syntax._
import play.api.libs.json.{Format, JsError, JsSuccess, Json, OFormat, OWrites, Reads, __}

final case class LevyCalculationRequest(
  lowLitres: Long,
  highLitres: Long,
  returnPeriod: ReturnPeriod
)

object LevyCalculationRequest {
  private implicit val returnPeriodReads: Reads[ReturnPeriod] = Reads { json =>
    for {
      year    <- (json \ "year").validate[Int]
      quarter <- (json \ "quarter").validate[Int]
      rp <-
        if (quarter < 0 || quarter > 3) {
          JsError(__ \ "quarter", "Invalid returnPeriod - quarter must be between 0 and 3")
        } else if (year < 2018) {
          JsError(__ \ "year", "Invalid returnPeriod - year must be >= 2018")
        } else {
          JsSuccess(ReturnPeriod(year, quarter))
        }
    } yield rp
  }

  private implicit val returnPeriodWrites: OWrites[ReturnPeriod] = Json.writes[ReturnPeriod]

  implicit val reads: Reads[LevyCalculationRequest] = (
    (__ \ "lowLitres").read[Long] and
      (__ \ "highLitres").read[Long] and
      (__ \ "returnPeriod").read[ReturnPeriod]
  )(LevyCalculationRequest.apply)

  implicit val writes: OWrites[LevyCalculationRequest] = Json.writes[LevyCalculationRequest]

  implicit val format: OFormat[LevyCalculationRequest] = OFormat(reads, writes)
}

final case class LevyCalculationResponse(
  lowBandLevy: BigDecimal,
  highBandLevy: BigDecimal,
  totalLevy: BigDecimal,
  totalRoundedDown: BigDecimal
)

object LevyCalculationResponse {
  implicit val format: Format[LevyCalculationResponse] = Json.format[LevyCalculationResponse]
}
