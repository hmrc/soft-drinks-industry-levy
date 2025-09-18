/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.libs.json._
import scala.language.implicitConversions

/** Utility class for creating json formatters for enumerations.
  */
object EnumUtils {
  def enumReads[E <: Enumeration](`enum`: E): Reads[`enum`.Value] = Reads[`enum`.Value] {
    case JsString(s) =>
      `enum`.values.find(_.toString == s) match {
        case Some(value) => JsSuccess(value)
        case None =>
          JsError(
            s"Enumeration expected of type: '${`enum`.getClass}', but it does not appear to contain the value: '$s'"
          )
      }
    case _ => JsError("String value expected")
  }

  implicit def enumWrites[E <: Enumeration](`enum`: E): Writes[`enum`.Value] = new Writes[`enum`.Value] {
    def writes(v: `enum`.Value): JsValue = JsString(v.toString)
  }

  implicit def enumFormat[E <: Enumeration](`enum`: E): Format[`enum`.Value] =
    Format(enumReads(`enum`), enumWrites(`enum`))

}
