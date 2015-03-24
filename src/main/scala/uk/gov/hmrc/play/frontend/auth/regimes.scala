package uk.gov.hmrc.play.frontend.auth

import uk.gov.hmrc.play.auth.frontend.connectors.domain.{Accounts, Authority}

abstract class TaxRegime {

  def isAuthorised(accounts: Accounts): Boolean

  def authenticationType: AuthenticationProvider

  def unauthorisedLandingPage: Option[String] = None
}

case class User(@deprecated("Use user.userId - see AuthContext", since = "2015-03-12") userId: String,
                @deprecated("Use principal and/or user - see AuthContext", since = "2015-03-12") userAuthority: Authority,
                @deprecated("Use principal.name and/or attorney.name - see AuthContext", since = "2015-03-12") nameFromGovernmentGateway: Option[String] = None,
                @deprecated("Use user.governmentGatewayToken - see AuthContext", since = "2015-03-12") decryptedToken: Option[String] = None,
                @deprecated("Use principal and attorney fields - see AuthContext", since = "2015-03-12") actingAsAttorneyFor:Option[ActingAsAttorneyFor] = None,
                attorney: Option[Attorney] = None)
  extends AuthContext {

  override val user = LoggedInUser(
    userId = userId,
    loggedInAt = userAuthority.loggedInAt,
    previouslyLoggedInAt = userAuthority.previouslyLoggedInAt,
    governmentGatewayToken = decryptedToken
  )

  override val principal = Principal(
    name = nameFromGovernmentGateway,
    accounts = userAuthority.accounts
  )

  @deprecated("Use principal.name and/or attorney.name - see AuthContext", since = "2015-03-12")
  val displayName: Option[String] = principal.name


  @deprecated("Use user.oid - see AuthContext", since = "2015-03-12")
  lazy val oid = user.oid
}
