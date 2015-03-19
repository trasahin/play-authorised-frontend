package uk.gov.hmrc.play.frontend.auth

import play.api.mvc.Session
import uk.gov.hmrc.play.http.SessionKeys

case class UserSessionData(userId: Option[String], governmentGatewayToken: Option[String], name: Option[String], delegationState: DelegationState) {
  val userCredentials = UserCredentials(userId, governmentGatewayToken)
}

object UserSessionData {

  private val DelegationStateSessionKey = "delegationState"

  def apply(session: Session): UserSessionData = UserSessionData(
    userId = session.get(SessionKeys.userId),
    governmentGatewayToken = session.get(SessionKeys.token),
    name = session.get(SessionKeys.name),
    delegationState = DelegationState(session.get(DelegationStateSessionKey))
  )
}



