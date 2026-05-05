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

package uk.gov.hmrc.softdrinksindustrylevy.connectors

import sdil.models.ReturnPeriod
import sdil.models.des.FinancialTransactionResponse
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.softdrinksindustrylevy.models.{CreateSubscriptionResponse, DisplayDirectDebitResponse, ReturnsRequest, Subscription}

import scala.concurrent.Future

trait SdilConnector {

  def createSubscription(request: Subscription, idType: String, idNumber: String)(implicit
    hc: HeaderCarrier
  ): Future[CreateSubscriptionResponse]

  def retrieveSubscriptionDetails(idType: String, idNumber: String)(implicit
    hc: HeaderCarrier
  ): Future[Option[Subscription]]

  def submitReturn(sdilRef: String, returnsRequest: ReturnsRequest)(implicit
    hc: HeaderCarrier,
    period: ReturnPeriod
  ): Future[HttpResponse]

  def retrieveFinancialData(sdilRef: String, year: Option[Int])(implicit
    hc: HeaderCarrier
  ): Future[Option[FinancialTransactionResponse]]

  def displayDirectDebit(sdilRef: String)(implicit hc: HeaderCarrier): Future[DisplayDirectDebitResponse]

}
