package uk.gov.hmrc.softdrinksindustrylevy.controllers

import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.controller.BackendController
import uk.gov.hmrc.softdrinksindustrylevy.connectors.DesConnector

class DirectDebitsController(desConnector: DesConnector,
                             val cc: ControllerComponents
                            )
  extends BackendController(cc) {

  def checkDdStatus(sdilRef:String):Action[AnyContent] = Action.async { implicit request =>
    for {
      response <- desConnector.displayDirectDebit(sdilRef)
    }yield if(response.directDebitMandateFound)NoContent else NotFound
  }
}
