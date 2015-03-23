package uk.gov.hmrc.play.frontend.auth

import org.joda.time.DateTime
import uk.gov.hmrc.play.auth.frontend.connectors.domain.{Authority, Accounts}

trait AuthContext {

  def user: LoggedInUser

  def principal: Principal

  def attorney: Option[Attorney]

  lazy val displayName: Option[String] = (attorney, principal.name) match {
    case (Some(att), Some(principalName)) => Some(att.name + " on behalf of " + principalName)
    case (Some(att), None) => Some(att.name)
    case (_, optionalPrincipalName) => optionalPrincipalName
  }
}

case class LoggedInUser(userId: String,loggedInAt: Option[DateTime], previouslyLoggedInAt: Option[DateTime], governmentGatewayToken: Option[String]) {
  lazy val oid: String = OidExtractor.userIdToOid(userId)
}

case class Principal(name: Option[String], accounts: Accounts)

case class Attorney(name: String, returnLink: Link)

case class Link(url: String, text: String)

private[auth] class AuthenticationContext(override val user: LoggedInUser, override val principal: Principal, override val attorney: Option[Attorney])
  extends User(
    userId = user.userId,
    userAuthority = Authority(user.userId, principal.accounts, user.loggedInAt, user.previouslyLoggedInAt),
    nameFromGovernmentGateway = principal.name,
    decryptedToken = user.governmentGatewayToken,
    actingAsAttorneyFor = None)
  with AuthContext


object AuthContext {

  def apply(user: LoggedInUser, principal: Principal, attorney: Option[Attorney]): User = {
    new AuthenticationContext(user, principal, attorney)
  }

  def apply(authority: Authority, governmentGatewayToken: Option[String], nameFromSession: Option[String], delegationData: Option[DelegationData] = None): User = {

    val (principalName: Option[String], accounts: Accounts, attorney: Option[Attorney]) = delegationData match {
      case Some(delegation) => (Some(delegation.principalName), delegation.accounts, Some(delegation.attorney))
      case None => (nameFromSession, authority.accounts, None)
    }

    AuthContext(
      user = LoggedInUser(
        userId = authority.uri,
        loggedInAt = authority.loggedInAt,
        previouslyLoggedInAt = authority.previouslyLoggedInAt,
        governmentGatewayToken = governmentGatewayToken
      ),
      principal = Principal(
        name = principalName,
        accounts = accounts
      ),
      attorney = attorney
    )
  }

  def unapply(authContext: AuthContext): Option[(LoggedInUser, Principal, Option[Attorney])] = {
    Option(authContext).map(auth => (auth.user, auth.principal, auth.attorney))
  }
}

object OidExtractor {
  def userIdToOid(userId: String): String = userId.substring(userId.lastIndexOf("/") + 1)
}
