package uk.gov.hmrc.play.frontend.auth

import play.api.Logger
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Authority
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

private[auth] trait AuthContextService {

  self: DelegationDataProvider =>

  protected def authConnector: AuthConnector

  private[auth] def currentAuthContext(sessionData: UserSessionData)(implicit hc: HeaderCarrier): Future[Option[AuthContext]] = {

    sessionData.userId match {
      case Some(userId) => loadAuthContext(userId, sessionData.governmentGatewayToken, sessionData.name, sessionData.delegationState)
      case None => Future.successful(None)
    }
  }

  private def loadAuthContext(userId: String,
                              governmentGatewayToken: Option[String],
                              nameFromSession: Option[String],
                              delegationState: DelegationState)
                             (implicit hc: HeaderCarrier): Future[Option[AuthContext]] = {

    val authorityResponse = loadAuthority(userId)

    val delegationDataResponse = delegationState match {
      case DelegationOn => loadDelegationData(userId)
      case _ => Future.successful(None)
    }

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
}

private[auth] sealed trait DelegationDataProvider {
  protected def loadDelegationData(userId: String)(implicit hc: HeaderCarrier): Future[Option[DelegationData]]
}

private[auth] trait DelegationDisabled extends DelegationDataProvider {
  protected override def loadDelegationData(userId: String)(implicit hc: HeaderCarrier): Future[Option[DelegationData]] =
    Future.successful(None)
}

private[auth] trait DelegationEnabled extends DelegationDataProvider {

  protected def delegationConnector: DelegationConnector

  protected override def loadDelegationData(userId: String)(implicit hc: HeaderCarrier): Future[Option[DelegationData]] = {
    delegationConnector.getDelegationData(OidExtractor.userIdToOid(userId))
  }
}
