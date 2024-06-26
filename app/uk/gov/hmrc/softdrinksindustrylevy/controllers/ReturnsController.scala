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

package uk.gov.hmrc.softdrinksindustrylevy.controllers

import play.api.Logger
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import sdil.models.{ReturnPeriod, SdilReturn}
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.credentials
import uk.gov.hmrc.auth.core.{AuthConnector, AuthProviders, AuthorisedFunctions}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.softdrinksindustrylevy.connectors.DesConnector
import uk.gov.hmrc.softdrinksindustrylevy.models._
import uk.gov.hmrc.softdrinksindustrylevy.models.json.des.returns._
import uk.gov.hmrc.softdrinksindustrylevy.services.{ReturnsPersistence, SdilMongoPersistence}

import java.time._
import scala.concurrent.{ExecutionContext, Future}
import com.google.inject.{Inject, Singleton}

@Singleton
class ReturnsController @Inject() (
  val authConnector: AuthConnector,
  desConnector: DesConnector,
  val persistence: SdilMongoPersistence,
  val returns: ReturnsPersistence,
  auditing: AuditConnector,
  val cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc) with AuthorisedFunctions {

  lazy val logger = Logger(this.getClass)

  def checkSmallProducerStatus(
    idType: String,
    idNumber: String,
    year: Int,
    quarter: Int
  ): Action[AnyContent] = Action.async { implicit request =>
    authorised(AuthProviders(GovernmentGateway)) {
      val period = ReturnPeriod(year, quarter)
      for {
        sub  <- desConnector.retrieveSubscriptionDetails(idType, idNumber)
        subs <- sub.fold(Future(List.empty[Subscription]))(s => persistence.list(s.utr))
        byRef = sub.fold(subs)(x => subs.filter(_.sdilRef == x.sdilRef))
        isSmallProd = byRef.nonEmpty && byRef.forall(b =>
                        b.deregDate.fold(b.activity.isSmallProducer && b.liabilityDate.isBefore(period.end))(y =>
                          y.isAfter(period.end) && b.activity.isSmallProducer
                        )
                      )
      } yield Ok(Json.toJson(isSmallProd))
    }
  }

  def buildReturnAuditDetail(
    sdilReturn: SdilReturn,
    returnsRequest: ReturnsRequest,
    providerId: String,
    period: ReturnPeriod,
    subscription: Option[Subscription],
    utr: String,
    outcome: String
  ): JsValue = {
    val sdilNo: String = subscription
      .flatMap {
        _.sdilRef
      }
      .fold("unknown") {
        identity
      }
    Json.obj(
      "sdilNumber" -> sdilNo,
      "orgName" -> subscription.fold("unknown") {
        _.orgName
      },
      "utr"              -> utr,
      "outcome"          -> outcome,
      "authProviderType" -> "GovernmentGateway",
      "authProviderId"   -> providerId,
      "return"           -> Json.toJson(returnsRequest)(writesForAuditing(period, sdilReturn)).as[JsObject]
    )
  }

  def post(utr: String, year: Int, quarter: Int): Action[JsValue] =
    Action.async(parse.json) { implicit request =>
      authorised(AuthProviders(GovernmentGateway)).retrieve(credentials) { creds =>
        withJsonBody[SdilReturn] { sdilReturn =>
          logger.info("SDIL return submission sent to DES")
          implicit val period: ReturnPeriod = ReturnPeriod(year, quarter)

          val returnsReq = ReturnsRequest(sdilReturn)
          (for {
            subscription <- desConnector.retrieveSubscriptionDetails("utr", utr)
            ref = subscription.get.sdilRef.get
            _ <- desConnector.submitReturn(ref, returnsReq)
            _ <- auditing.sendExtendedEvent(
                   new SdilReturnEvent(
                     request.uri,
                     buildReturnAuditDetail(
                       sdilReturn,
                       returnsReq,
                       creds.get.providerId,
                       period,
                       subscription,
                       utr,
                       "SUCCESS"
                     )
                   )
                 )
            _ <- returns(utr, period) = sdilReturn
          } yield Ok(Json.toJson(returnsReq))).recoverWith { case e =>
            auditing.sendExtendedEvent(
              new SdilReturnEvent(
                request.uri,
                buildReturnAuditDetail(sdilReturn, returnsReq, creds.get.providerId, period, None, utr, "ERROR")
              )
            ) map {
              throw e
            }
          }
        }
      }
    }

  def get(utr: String, year: Int, quarter: Int): Action[AnyContent] =
    Action.async {
      returns.get(utr, ReturnPeriod(year, quarter)).map {
        case Some(record) =>
          Ok(Json.toJson(record))
        case None => NotFound
      }
    }

  implicit class RichLong(i: Long) {
    def asMilliseconds: LocalDateTime =
      Instant
        .ofEpochMilli(i)
        .atZone(ZoneId.systemDefault)
        .toLocalDateTime
  }

  def pending(utr: String): Action[AnyContent] =
    Action.async { implicit request =>
      desConnector.retrieveSubscriptionDetails("utr", utr).flatMap { subscription =>
        val start = subscription match {
          case Some(x) => x.liabilityDate
          case None =>
            logger.error(s"Error occurred while retrieving subscriptionDetails for utr =  $utr")
            throw new NoSuchElementException(s"No subscription details found for the user utr= $utr")
        }

        val all = {
          ReturnPeriod(start).count to ReturnPeriod(LocalDate.now()).count
        }.map {
          ReturnPeriod.apply
        }.filter {
          _.end.isBefore(LocalDate.now())
        }
        returns.list(utr).map { posted: Map[ReturnPeriod, SdilReturn] =>
          Ok(Json.toJson(all.toList diff posted.keys.toList))
        }
      }
    }

  def variable(utr: String): Action[AnyContent] =
    Action.async {
      returns.listVariable(utr).map { posted =>
        Ok(Json.toJson(posted.keys.toList))
      }
    }
}
