package uk.gov.hmrc.play.frontend.auth

import play.api.mvc._
import uk.gov.hmrc.play.frontend.auth.connectors.domain.LevelOfAssurance.LOA_2

import scala.concurrent.Future

trait Actions extends UserActions with DelegationDisabled
trait DelegationAwareActions extends UserActions with DelegationEnabled

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

  implicit def makeAction(body: PlayUserRequest): UserAction = (authContext: AuthContext) => Action(body(authContext))

  implicit def makeFutureAction(body: AsyncPlayUserRequest): UserAction = (authAction: AuthContext) => Action.async(body(authAction))

  class AuthenticatedBy(authenticationProvider: AuthenticationProvider,
                        taxRegime: Option[TaxRegime],
                        redirectToOrigin: Boolean,
                        pageVisibility: PageVisibilityPredicate) extends AuthenticatedAction {
    def apply(body: PlayUserRequest): Action[AnyContent] = authorised(authenticationProvider, taxRegime, redirectToOrigin, pageVisibility, body)

    def async(body: AsyncPlayUserRequest): Action[AnyContent] = authorised(authenticationProvider, taxRegime, redirectToOrigin, pageVisibility, body)
  }


  def AuthorisedFor(taxRegime: TaxRegime,
                    redirectToOrigin: Boolean = false,
                    pageVisibility: PageVisibilityPredicate = LoaPredicate(LOA_2))
  = new AuthenticatedBy(taxRegime.authenticationType, Some(taxRegime), redirectToOrigin, pageVisibility)

  def AuthenticatedBy(authenticationProvider: AuthenticationProvider,
                      redirectToOrigin: Boolean = false,
                      pageVisibility: PageVisibilityPredicate = LoaPredicate(LOA_2))
  = new AuthenticatedBy(authenticationProvider, None, redirectToOrigin, pageVisibility)

  private def authorised(authenticationProvider: AuthenticationProvider,
                         taxRegime: Option[TaxRegime],
                         redirectToOrigin: Boolean,
                         pageVisibility: PageVisibilityPredicate,
                         body: UserAction) =
      WithSessionTimeoutValidation(authenticationProvider) {
        WithUserAuthenticatedBy(authenticationProvider, taxRegime, redirectToOrigin) {
          authContext =>
            WithPageVisibility(pageVisibility, authContext) {
              implicit authContext => body(authContext)
            }
        }
      }
}


