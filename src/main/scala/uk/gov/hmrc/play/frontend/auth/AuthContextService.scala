package uk.gov.hmrc.play.frontend.auth

import play.api.Logger
import play.api.mvc.{AnyContent, Request, Result}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.auth.frontend.connectors.AuthConnector
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

trait AuthContextService {

  protected def authConnector: AuthConnector
//  protected def delegationConnector: DelegationConnector

  private[auth] def currentAuthContext(userId: String, governmentGatewayToken: Option[String], nameFromSession: Option[String])(implicit hc: HeaderCarrier): Future[Option[User]] = {

    authConnector.currentAuthority.map {
      case Some(authority) if authority.uri == userId => Some(AuthContext(authority, governmentGatewayToken, nameFromSession))
      case Some(authority) if authority.uri != userId =>
        Logger.warn(s"Current Authority uri does not match session userId '$userId', ending session.  Authority found was: $authority")
        None
      case None => None
    }
  }

  def beginDelegation(result: Result, delegationData: DelegationData)(implicit request: Request[AnyContent]): Future[Result] = {
//    result.addingToSession()
    ???
  }

  def endDelegation(result: Result)(implicit request: Request[AnyContent]): Future[Result] = {
//        result.removingFromSession()
    ???
  }
}
