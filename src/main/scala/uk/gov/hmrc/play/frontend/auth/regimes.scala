package uk.gov.hmrc.play.frontend.auth

import uk.gov.hmrc.play.auth.frontend.connectors.domain.{Accounts, Authority}

abstract class TaxRegime {

  def isAuthorised(accounts: Accounts): Boolean

  def authenticationType: AuthenticationProvider

  def unauthorisedLandingPage: Option[String] = None
}
