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

package uk.gov.hmrc.softdrinksindustrylevy.controllers

import play.api.libs.json._
import play.api.mvc._
import scala.concurrent._
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.softdrinksindustrylevy.connectors.DesConnector
import sdil.models.FinancialLineItem
import java.time._

class BalanceController(
  desConnector: DesConnector
)(implicit ec: ExecutionContext) extends BaseController {

  val lineItems = List (
    FinancialLineItem(LocalDate.of(2018,8,8), "Payment received", 1645),
    FinancialLineItem(LocalDate.of(2018,8,6), "1% Interest", -12),
    FinancialLineItem(LocalDate.of(2018,8,4), "1% Interest", -11),
    FinancialLineItem(LocalDate.of(2018,8,2), "1% Interest", -10),
    FinancialLineItem(LocalDate.of(2018,7,31), "Late payment fee", -100),
    FinancialLineItem(LocalDate.of(2018,7,1), "Return Apr to Jun 2018", -1512),
    FinancialLineItem(LocalDate.of(2018,4,1), "Return Oct to Dec 2017", 0)
  )

  def balance(sdilRef: String) = Action {
    Ok(JsNumber(lineItems.map{_.amount}.sum))
  }

  implicit protected val format = Json.format[FinancialLineItem]

  def balanceHistory(sdilRef: String) = Action {
    Ok(JsArray(lineItems.map{Json.toJson(_)}))
  }

}
