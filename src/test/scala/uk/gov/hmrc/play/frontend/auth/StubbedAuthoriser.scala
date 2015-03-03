package uk.gov.hmrc.play.frontend.auth

import play.api.mvc.{Action, AnyContent, Request, Result}

import scala.concurrent.Future

trait StubbedAuthoriser extends Authoriser {
  val user: User

  override def AuthorisedFor(regime: TaxRegime,
                             redirectToOrigin: Boolean = false,
                             pageVisibility: PageVisibilityPredicate = DefaultPageVisibilityPredicate) = {
    new StubbedAuthenticationAction
  }


  override def AuthenticatedBy(authenticationProvider: AuthenticationProvider,
                               redirectToOrigin: Boolean,
                               pageVisibility: PageVisibilityPredicate): AuthenticatedAction = {
    new StubbedAuthenticationAction
  }

  class StubbedAuthenticationAction extends AuthenticatedAction {
    override def apply(body: (User) => (Request[AnyContent]) => Result): Action[AnyContent] = Action {
      body(user)
    }

    override def async(body: (User) => (Request[AnyContent]) => Future[Result]): Action[AnyContent] = Action.async {
      body(user)
    }
  }
}
