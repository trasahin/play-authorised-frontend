package uk.gov.hmrc.play.frontend.auth

import play.api.mvc._
import uk.gov.hmrc.domain._
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.connectors.DelegationConnector
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future


trait Delegator {

  protected def delegationConnector: DelegationConnector

  def startDelegationAndRedirect(delegationContext: DelegationContext, redirectUrl: String)(implicit hc: HeaderCarrier, authContext: AuthContext, request: RequestHeader): Future[Result] = {

    delegationConnector.startDelegation(authContext.user.oid, delegationContext).map { _ =>
      Results.SeeOther(redirectUrl).addingToSession(UserSessionData.DelegationStateSessionKey -> DelegationOn.toString)
    }
  }

  def endDelegation(result: Result)(implicit hc: HeaderCarrier, authContext: AuthContext, request: RequestHeader): Future[Result] = {
    delegationConnector.endDelegation(authContext.user.oid).map { _ =>
      result.removingFromSession(UserSessionData.DelegationStateSessionKey)
    }
  }
}

case class DelegationContext(principalName: String, attorneyName: String, link: Link, principalTaxIdentifiers: TaxIdentifiers)

case class TaxIdentifiers(paye: Option[Nino] = None,
                          sa: Option[SaUtr] = None,
                          ct: Option[CtUtr] = None,
                          vat: Option[Vrn] = None,
                          epaye: Option[EmpRef] = None,
                          taxsAgent: Option[Uar] = None,
                          ated: Option[AtedUtr] = None)


