package uk.gov.hmrc.play.frontend.auth

trait Authoriser {
  def AuthorisedFor(regime: TaxRegime,
                    redirectToOrigin: Boolean = false,
                    pageVisibility: PageVisibilityPredicate = DefaultPageVisibilityPredicate): AuthenticatedAction

  def AuthenticatedBy(authenticationProvider: AuthenticationProvider,
                      redirectToOrigin: Boolean = false,
                      pageVisibility: PageVisibilityPredicate = DefaultPageVisibilityPredicate): AuthenticatedAction
}
