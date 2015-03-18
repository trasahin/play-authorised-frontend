package uk.gov.hmrc.play.frontend.auth

import java.net.URI

import org.joda.time.DateTime
import uk.gov.hmrc.play.auth.frontend.connectors.domain.{Authority, Accounts}

trait AuthContext {

  def user: LoggedInUser

  def principal: Principal

  def attorney: Option[Attorney]
}

case class LoggedInUser(userId: String,loggedInAt: Option[DateTime], previouslyLoggedInAt: Option[DateTime], governmentGatewayToken: Option[String]) {
  lazy val oid: String = userId.substring(userId.lastIndexOf("/") + 1)
}

case class Principal(name: Option[String], accounts: Accounts)

case class Attorney(name: String, returnLink: Link)

case class Link(url: URI, text: String)



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

  def apply(authority: Authority, governmentGatewayToken: Option[String], nameFromSession: Option[String]): User = {

    AuthContext(
      user = LoggedInUser(
        userId = authority.uri,
        loggedInAt = authority.loggedInAt,
        previouslyLoggedInAt = authority.previouslyLoggedInAt,
        governmentGatewayToken = governmentGatewayToken
      ),
      principal = Principal(
        name = nameFromSession,
        accounts = authority.accounts
      ),
      attorney = None
    )
  }

  def unapply(authContext: AuthContext): Option[(LoggedInUser, Principal, Option[Attorney])] = {
    Option(authContext).map(auth => (auth.user, auth.principal, auth.attorney))
  }
}