package uk.gov.hmrc.play.frontend.auth

import play.api.mvc._
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

class ProtectedAction(authRequirements: AuthRequirements, handleNotAuthenticated: => Result, authConnectorProvided: AuthConnector) extends AuthenticatedAction {
  override def apply(body: (AuthContext) => (Request[AnyContent]) => Result): Action[AnyContent] = {

    Action.async {
      implicit request =>
        val sd = UserSessionData(request.session)
        implicit val hc = HeaderCarrier.fromHeadersAndSession(request.headers, Some(request.session))
        authContextService.currentAuthContext(sd) map {
          case Some(authContext) if authContext.principal.accounts.contains(authRequirements.account)=>
            body(authContext)(request)
          case _ =>
            handleNotAuthenticated
        }
    }
  }

  override def async(body: (AuthContext) => (Request[AnyContent]) => Future[Result]): Action[AnyContent] = ???

  def authContextService: AuthContextService = new AuthContextService with DelegationDisabled {
    override protected def authConnector: AuthConnector = authConnectorProvided
  }

}

class ProtectedSubAction(authRequirements: AuthRequirements) extends AuthenticatedAction {
  override def apply(body: (AuthContext) => (Request[AnyContent]) => Result): Action[AnyContent] = ???

  override def async(body: (AuthContext) => (Request[AnyContent]) => Future[Result]): Action[AnyContent] = ???
}

trait ProtectedActionHelper {
  def ProtectedAction(authRequirements: AuthRequirements, handleNotAuthenticated: => Result, authConnectorProvided: AuthConnector) = new ProtectedAction(authRequirements, handleNotAuthenticated, authConnectorProvided)
}