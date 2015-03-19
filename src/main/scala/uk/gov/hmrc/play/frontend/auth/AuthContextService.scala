package uk.gov.hmrc.play.frontend.auth

import play.api.Logger
import play.api.mvc.Result
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.auth.frontend.connectors.AuthConnector
import uk.gov.hmrc.play.auth.frontend.connectors.domain.Authority
import uk.gov.hmrc.play.frontend.auth.connectors.DelegationConnector
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

private[auth] trait AuthContextService {

  protected def authConnector: AuthConnector
  protected def delegationConnector: Option[DelegationConnector] = None

  private[auth] def currentAuthContext(sessionData: UserSessionData)(implicit hc: HeaderCarrier): Future[Option[User]] = {

    sessionData.userId match {
      case Some(userId) => loadAuthContext(userId, sessionData.governmentGatewayToken, sessionData.name, sessionData.delegationState)
      case None => Future.successful(None)
    }
  }

  private def loadAuthContext(userId: String,
                              governmentGatewayToken: Option[String],
                              nameFromSession: Option[String],
                              delegationState: DelegationState)
                             (implicit hc: HeaderCarrier): Future[Option[User]] = {

    val authorityResponse = loadAuthority(userId)
    val delegationDataResponse = loadDelegationData(userId, delegationState)

    authorityResponse.flatMap {
      case Some(authority) => delegationDataResponse.map { delegationData =>
        Some(AuthContext(authority, governmentGatewayToken, nameFromSession, delegationData))
      }
      case None => Future.successful(None)
    }
  }

  private def loadAuthority(userId: String)(implicit hc: HeaderCarrier): Future[Option[Authority]] = {

    authConnector.currentAuthority.map {
      case Some(authority) if authority.uri == userId => Some(authority)
      case Some(authority) if authority.uri != userId =>
        Logger.warn(s"Current Authority uri does not match session userId '$userId', ending session.  Authority found was: $authority")
        None
      case None => None
    }
  }

  private def loadDelegationData(userId: String, delegationState: DelegationState)(implicit hc: HeaderCarrier): Future[Option[DelegationData]] = {

    delegationConnector match {
      case Some(connector) if delegationState == DelegationOn =>
        connector.get(OidExtractor.userIdToOid(userId)).map { delegationData: Option[DelegationData] =>
          if (delegationData.isEmpty) Logger.warn(s"Delegation state is 'On', but no delegation data found for userId: $userId")
          delegationData
        }
      case _ => Future.successful(None)
    }
  }

  def beginDelegation(result: Result, delegationData: DelegationData)(implicit hc: HeaderCarrier): Future[Result] = {
    //    Future.successful(result.addingToSession("abc" -> "123"))
    ???
  }

  def endDelegation(result: Result)(implicit hc: HeaderCarrier): Future[Result] = {
    //        result.removingFromSession()
    ???
  }
}
