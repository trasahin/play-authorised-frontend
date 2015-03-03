package uk.gov.hmrc.play.frontend.auth

import uk.gov.hmrc.play.auth.frontend.connectors.domain.{Accounts, Authority}

abstract class TaxRegime {

  def isAuthorised(accounts: Accounts): Boolean

  def authenticationType: AuthenticationProvider

  def unauthorisedLandingPage: Option[String] = None
}

case class User(userId: String,
                userAuthority: Authority,
                nameFromGovernmentGateway: Option[String] = None,
                decryptedToken: Option[String] = None,
                actingAsAttorneyFor:Option[ActingAsAttorneyFor]=None) {

  def oid = userId.substring(userId.lastIndexOf("/") + 1)

  def displayName : Option[String] = nameFromGovernmentGateway
}






