package uk.gov.hmrc.play.frontend.auth

import play.api.Logger
import play.api.mvc.{Result, _}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent._

trait UserActionWrapper extends Results {

  self: AuthContextService =>

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

        handle(UserSessionData(request.session).userCredentials).flatMap {
          case Left(successfullyFoundUser) => userAction(successfullyFoundUser)(request)
          case Right(resultOfFailure) => Action(resultOfFailure)(request)
        }
    }

  private def handleAuthenticated(taxRegime: Option[TaxRegime], authenticationProvider: AuthenticationProvider)
                                 (implicit request: Request[AnyContent]):
  PartialFunction[UserCredentials, Future[Either[User, Result]]] = {
    case UserCredentials(Some(userId), tokenOption) =>
      implicit val hc = HeaderCarrier.fromSessionAndHeaders(request.session, request.headers)

      currentAuthContext(UserSessionData(request.session)).flatMap {

        case Some(authContext) => taxRegime match {
          case Some(regime) if !regime.isAuthorised(authContext.principal.accounts) =>
            Logger.info("user not authorised for " + regime.getClass)
            regime.unauthorisedLandingPage match {
              case Some(redirectUrl) => Future.successful(Right(Redirect(redirectUrl)))
              case None => authenticationProvider.redirectToLogin(redirectToOrigin = false).map(Right(_))
            }
          case _ => Future.successful(Left(authContext))
        }
        case None =>
          Logger.warn(s"No authority found for user id '$userId' from '${request.remoteAddress}'")
          authenticationProvider.redirectToLogin(redirectToOrigin = false).map {
            result =>
              Right(result.withNewSession)
          }
      }
  }
}