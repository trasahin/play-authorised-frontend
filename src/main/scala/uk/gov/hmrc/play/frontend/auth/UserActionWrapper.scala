package uk.gov.hmrc.play.frontend.auth

import play.api.Logger
import play.api.mvc.{Result, _}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.auth.frontend.connectors.AuthConnector
import uk.gov.hmrc.play.auth.frontend.connectors.domain.Authority
import uk.gov.hmrc.play.http.SessionKeys
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent._

trait UserActionWrapper extends Results {

  protected implicit def authConnector: AuthConnector

  private[auth] def WithUserAuthorisedBy(authenticationProvider: AuthenticationProvider,
                                            taxRegime: Option[TaxRegime],
                                            redirectToOrigin: Boolean)
                                           (userAction: User => Action[AnyContent]): Action[AnyContent] =
    Action.async {
      implicit request =>
        implicit val hc = HeaderCarrier.fromSessionAndHeaders(request.session, request.headers)
        Logger.info(s"WithUserAuthorisedBy using auth provider ${authenticationProvider.id}")
        val handle =
          authenticationProvider.handleNotAuthenticated(redirectToOrigin) orElse
            authenticationProvider.handleAuthenticated orElse
            handleAuthenticated(taxRegime, authenticationProvider)

        handle(UserCredentials(request.session)).flatMap {
          case Left(successfullyFoundUser) => userAction(successfullyFoundUser)(request)
          case Right(resultOfFailure) => Action(resultOfFailure)(request)
        }
    }

  private def handleAuthenticated(taxRegime: Option[TaxRegime], authenticationProvider: AuthenticationProvider)
                                 (implicit request: Request[AnyContent]):
  PartialFunction[UserCredentials, Future[Either[User, Result]]] = {
    case UserCredentials(Some(userId), tokenOption) =>
      implicit val hc = HeaderCarrier.fromSessionAndHeaders(request.session, request.headers)
      val authority = authConnector.currentAuthority
      Logger.debug(s"Received user authority: $authority")

      authority.flatMap {
        case Some(ua) => taxRegime match {
          case Some(regime) if !regime.isAuthorised(ua.accounts) =>
            Logger.info("user not authorised for " + regime.getClass)
            regime.unauthorisedLandingPage match {
              case Some(redirectUrl) => Future.successful(Right(Redirect(redirectUrl)))
              case None => authenticationProvider.redirectToLogin(redirectToOrigin = false).map(Right(_))
            }
          case _ =>
            Future.successful(Left(constructAuthContext(userId, ua, tokenOption)))
        }
        case _ =>
          Logger.warn(s"No authority found for user id '$userId' from '${request.remoteAddress}'")
          authenticationProvider.redirectToLogin(redirectToOrigin = false).map {
            result =>
              Right(result.withNewSession)
          }
      }
  }

  private def constructAuthContext(userId: String, authority: Authority, governmentGatewayToken: Option[String])(implicit request: Request[AnyContent]): User = {
    new User(
      userId = userId,
      userAuthority = authority,
      nameFromGovernmentGateway = request.session.get(SessionKeys.name),
      decryptedToken = governmentGatewayToken)
  }
}