package uk.gov.hmrc.play.frontend.auth

import play.api.Logger
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.http.SessionKeys
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext.fromLoggingDetails

import scala.concurrent._

case class UserCredentials(userId: Option[String], token: Option[String])

object UserCredentials {
  @deprecated("Use UserSessionData.apply(...).userCredentials", since = "March 2015")
  def apply(session: Session): UserCredentials = UserCredentials(session.get(SessionKeys.userId), session.get(SessionKeys.token))
}

object AuthenticationProviderIds {
  val GovernmentGatewayId = "GGW"
  val AnyAuthenticationProviderId = "IDAorGGW"
}

trait AuthenticationProvider {
  type FailureResult = Result

  def id: String

  def redirectToLogin(redirectToOrigin: Boolean = false)(implicit request: Request[AnyContent]): Future[Result]

  def handleSessionTimeout(implicit request: Request[AnyContent]): Future[Result] = redirectToLogin(redirectToOrigin = false)

  def handleNotAuthenticated(redirectToOrigin: Boolean)(implicit request: Request[AnyContent]): PartialFunction[UserCredentials, Future[Either[AuthContext, FailureResult]]]

  def handleAuthenticated(implicit request: Request[AnyContent]): PartialFunction[UserCredentials, Future[Either[AuthContext, Result]]] = PartialFunction.empty

  implicit def hc(implicit request: Request[_]) = HeaderCarrier.fromSessionAndHeaders(request.session, request.headers)
}

trait GovernmentGateway extends AuthenticationProvider {

  override val id = AuthenticationProviderIds.GovernmentGatewayId

  def login: String

  def redirectToLogin(redirectToOrigin: Boolean)(implicit request: Request[AnyContent]) = Future.successful(Redirect(login))

  def handleNotAuthenticated(redirectToOrigin: Boolean)(implicit request: Request[AnyContent]): PartialFunction[UserCredentials, Future[Either[AuthContext, FailureResult]]] = {
    case UserCredentials(None, token@_) =>
      Logger.info(s"No userId found - redirecting to login. user: None token : $token")
      redirectToLogin(redirectToOrigin).map(Right(_))
    case UserCredentials(Some(userId), None) =>
      Logger.info(s"No gateway token - redirecting to login. user : $userId token : None")
      redirectToLogin(redirectToOrigin).map(Right(_))
  }
}

trait AnyAuthenticationProvider extends AuthenticationProvider {

  val id = AuthenticationProviderIds.AnyAuthenticationProviderId

  def ggwAuthenticationProvider: GovernmentGateway

  def login: String

  def redirectToLogin(redirectToOrigin: Boolean)(implicit request: Request[AnyContent]): Future[Result] = {
    Logger.info("In AnyAuthenticationProvider - redirecting to login page")
    request.session.get(SessionKeys.authProvider) match {
      case _ => Future.successful(Redirect(login))
    }
  }

  def handleNotAuthenticated(redirectToOrigin: Boolean)(implicit request: Request[AnyContent]) = {
    request.session.get(SessionKeys.authProvider) match {
      case Some(AuthenticationProviderIds.GovernmentGatewayId) => ggwAuthenticationProvider.handleNotAuthenticated(redirectToOrigin)
      case _ => {
        case _ => Future.successful(Right(Redirect(login).withNewSession))
      }
    }
  }

}


