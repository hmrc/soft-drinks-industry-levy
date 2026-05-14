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
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.softdrinksindustrylevy.models.{CreateSubscriptionResponse, DisplayDirectDebitResponse, ReturnsRequest, Subscription}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RoutingSdilConnector @Inject() (
  desConnector: DesConnector,
  hipSubscriptionConnector: HipSubscriptionConnector,
  hipReturnsConnector: HipReturnsConnector,
  servicesConfig: ServicesConfig
)(implicit ec: ExecutionContext)
    extends SdilConnector {

  private val useHipSubscription =
    servicesConfig.getBoolean("features.hip.subscription")

  private val useHipReturns =
    servicesConfig.getBoolean("features.hip.returns")

  override def createSubscription(request: Subscription, idType: String, idNumber: String)(implicit
    hc: HeaderCarrier
  ): Future[CreateSubscriptionResponse] =
    if (useHipSubscription)
      hipSubscriptionConnector.createSubscription(request, idType, idNumber)
    else
      desConnector.createSubscription(request, idType, idNumber)

  override def retrieveSubscriptionDetails(idType: String, idNumber: String)(implicit
    hc: HeaderCarrier
  ): Future[Option[Subscription]] =
    if (useHipSubscription)
      hipSubscriptionConnector.retrieveSubscriptionDetails(idType, idNumber)
    else
      desConnector.retrieveSubscriptionDetails(idType, idNumber)

  override def submitReturn(sdilRef: String, returnsRequest: ReturnsRequest)(implicit
    hc: HeaderCarrier,
    period: ReturnPeriod
  ): Future[HttpResponse] =
    if (useHipReturns)
      hipReturnsConnector.submitReturn(sdilRef, returnsRequest)
    else
      desConnector.submitReturn(sdilRef, returnsRequest)

  override def retrieveFinancialData(sdilRef: String, year: Option[Int])(implicit
    hc: HeaderCarrier
  ): Future[Option[FinancialTransactionResponse]] =
    desConnector.retrieveFinancialData(sdilRef, year)

  override def displayDirectDebit(sdilRef: String)(implicit hc: HeaderCarrier): Future[DisplayDirectDebitResponse] =
    desConnector.displayDirectDebit(sdilRef)
}
