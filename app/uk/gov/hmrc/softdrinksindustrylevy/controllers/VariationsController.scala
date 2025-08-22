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

import com.google.inject.{Inject, Singleton}
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import sdil.models.ReturnVariationData
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.softdrinksindustrylevy.connectors.GformConnector
import uk.gov.hmrc.softdrinksindustrylevy.models.{ReturnsVariationRequest, VariationsRequest, VariationsSubmissionEvent, formatReturnVariationData}
import uk.gov.hmrc.softdrinksindustrylevy.services.{ReturnsAdjustmentSubmissionService, ReturnsVariationSubmissionService, VariationSubmissionService}

import scala.concurrent.ExecutionContext

@Singleton
class VariationsController @Inject() (
  override val messagesApi: MessagesApi,
  gforms: GformConnector,
  auditing: AuditConnector,
  variationSubmissions: VariationSubmissionService,
  returnSubmission: ReturnsVariationSubmissionService,
  returnsAdjustmentSubmissionService: ReturnsAdjustmentSubmissionService,
  val cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc) with I18nSupport {

  lazy val logger = Logger(this.getClass)

  def generateVariations(sdilNumber: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[VariationsRequest] { data =>
      val page = views.html.variations_pdf(data, sdilNumber).toString
      (for {
        _ <- gforms.submitToDms(page, sdilNumber)
        _ <- variationSubmissions.save(data, sdilNumber)
        _ <- auditing.sendExtendedEvent(
               new VariationsSubmissionEvent(
                 request.uri,
                 buildVariationsAudit(data, sdilNumber, "SUCCESS")
               )
             )
      } yield NoContent).recoverWith { case e =>
        auditing
          .sendExtendedEvent(
            new VariationsSubmissionEvent(
              request.uri,
              buildVariationsAudit(data, sdilNumber, "ERROR", Some(e.getMessage))
            )
          )
          .map(_ => throw e)
      }
    }
  }

  def returnsVariation(sdilNumber: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[ReturnsVariationRequest] { data =>
      val page = views.html.returns_variation_pdf(data, sdilNumber).toString
      for {
        _ <- gforms.submitToDms(page, sdilNumber)
        _ <- returnSubmission.save(data, sdilNumber)
      } yield NoContent
    }
  }

  def varyReturn(sdilRef: String): Action[JsValue] =
    Action.async(parse.json) { implicit request =>
      withJsonBody[ReturnVariationData] { data =>
        logger.info("SDIL return variation sent to DMS queue")
        implicit val returnPeriod = data.period
        val page = views.html.return_variation_pdf(data, sdilRef).toString
        for {
          _ <- gforms.submitToDms(page, sdilRef)
          _ <- returnsAdjustmentSubmissionService.save(data, sdilRef)
        } yield NoContent

      }
    }

  private def buildVariationsAudit(
    data: VariationsRequest,
    sdilNumber: String,
    outcome: String,
    error: Option[String] = None
  )(implicit hc: HeaderCarrier): JsValue = {

    val core: JsObject = Json.obj(
      "sdilNumber"       -> sdilNumber,
      "outcome"          -> outcome,
      "authProviderType" -> "GovernmentGateway",
      "deviceId"         -> hc.deviceID,
      "formTemplateId"   -> "SDIL-VAR-1"
    )

    val payload: JsObject = Json.obj(
      "tradingName"           -> data.tradingName,
      "orgName"               -> data.displayOrgName,
      "address"               -> data.ppobAddress,
      "businessContact"       -> data.businessContact,
      "correspondenceContact" -> data.correspondenceContact,
      "primaryPersonContact"  -> data.primaryPersonContact,
      "litreageActivity"      -> data.sdilActivity,
      "deregistration" -> Json.obj(
        "date"   -> data.deregistrationDate,
        "reason" -> data.deregistrationText
      ),
      "sites" -> Json.obj(
        "new"   -> data.newSites,
        "amend" -> data.amendSites,
        "close" -> data.closeSites
      )
    )

    core ++ payload ++ error.fold(Json.obj())(e => Json.obj("error" -> e))
  }

}
