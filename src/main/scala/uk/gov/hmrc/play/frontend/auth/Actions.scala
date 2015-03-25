package uk.gov.hmrc.play.frontend.auth

import play.api.mvc._

import scala.concurrent.Future

trait Actions extends UserActions with DelegationDisabled
trait ActionsWithDelegationAllowed extends UserActions with DelegationEnabled

sealed trait UserActions
  extends SessionTimeoutWrapper
  with UserActionWrapper
  with AuthContextService
  with Authoriser
  with Results {

  self: DelegationDataProvider =>

  private type PlayRequest = (Request[AnyContent] => Result)
  private type AsyncPlayRequest = (Request[AnyContent] => Future[Result])
  private type PlayUserRequest = AuthContext => PlayRequest
  private type AsyncPlayUserRequest = AuthContext => AsyncPlayRequest

  type UserAction = AuthContext => Action[AnyContent]

  implicit def makeAction(body: PlayUserRequest): UserAction = (user: AuthContext) => Action(body(user))

  implicit def makeFutureAction(body: AsyncPlayUserRequest): UserAction = (user: AuthContext) => Action.async(body(user))

  class AuthenticatedBy(authenticationProvider: AuthenticationProvider,
                        account: Option[TaxRegime],
                        redirectToOrigin: Boolean,
                        pageVisibility: PageVisibilityPredicate) extends AuthenticatedAction {
    def apply(body: PlayUserRequest): Action[AnyContent] = authorised(authenticationProvider, account, redirectToOrigin, pageVisibility, body)

    def async(body: AsyncPlayUserRequest): Action[AnyContent] = authorised(authenticationProvider, account, redirectToOrigin, pageVisibility, body)
  }


  def AuthorisedFor(regime: TaxRegime,
                    redirectToOrigin: Boolean = false,
                    pageVisibility: PageVisibilityPredicate = DefaultPageVisibilityPredicate)
  = new AuthenticatedBy(regime.authenticationType, Some(regime), redirectToOrigin, pageVisibility)

  def AuthenticatedBy(authenticationProvider: AuthenticationProvider,
                      redirectToOrigin: Boolean = false,
                      pageVisibility: PageVisibilityPredicate = DefaultPageVisibilityPredicate)
  = new AuthenticatedBy(authenticationProvider, None, redirectToOrigin, pageVisibility)

  private def authorised(authenticationProvider: AuthenticationProvider,
                         account: Option[TaxRegime],
                         redirectToOrigin: Boolean,
                         pageVisibility: PageVisibilityPredicate,
                         body: UserAction) =
      WithSessionTimeoutValidation(authenticationProvider) {
        WithUserAuthorisedBy(authenticationProvider, account, redirectToOrigin) {
          user =>
            WithPageVisibility(pageVisibility, user) {
              implicit user => body(user)
            }
        }
      }
}


