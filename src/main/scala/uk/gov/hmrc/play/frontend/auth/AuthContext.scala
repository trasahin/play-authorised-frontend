package uk.gov.hmrc.play.frontend.auth

import org.joda.time.DateTime
import uk.gov.hmrc.play.frontend.auth.connectors.domain.LevelOfAssurance.LevelOfAssurance
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{Accounts, Authority}

case class AuthContext(user: LoggedInUser, principal: Principal, attorney: Option[Attorney]) {
  lazy val isDelegating: Boolean = attorney.isDefined
}

object AuthContext {

  def apply(authority: Authority, governmentGatewayToken: Option[String] = None,
            nameFromSession: Option[String] = None,
            delegationData: Option[DelegationData] = None): AuthContext = {

    val (principalName: Option[String], accounts: Accounts, attorney: Option[Attorney]) = delegationData match {
      case Some(delegation) => (Some(delegation.principalName), delegation.accounts, Some(delegation.attorney))
      case None => (nameFromSession, authority.accounts, None)
    }

    AuthContext(
      user = LoggedInUser(
        userId = authority.uri,
        loggedInAt = authority.loggedInAt,
        previouslyLoggedInAt = authority.previouslyLoggedInAt,
        governmentGatewayToken = governmentGatewayToken,
        levelOfAssurance = authority.levelOfAssurance
      ),
      principal = Principal(
        name = principalName,
        accounts = accounts
      ),
      attorney = attorney
    )
  }
}

case class LoggedInUser(userId: String,loggedInAt: Option[DateTime],
                        previouslyLoggedInAt: Option[DateTime],
                        governmentGatewayToken: Option[String],
                        levelOfAssurance: LevelOfAssurance) {
  lazy val oid: String = OidExtractor.userIdToOid(userId)
}

case class Principal(name: Option[String], accounts: Accounts)

case class Attorney(name: String, returnLink: Link, supplementaryData: Map[String, String] = Map.empty)

case class Link(url: String, text: String)

object OidExtractor {
  def userIdToOid(userId: String): String = userId.substring(userId.lastIndexOf("/") + 1)
}
