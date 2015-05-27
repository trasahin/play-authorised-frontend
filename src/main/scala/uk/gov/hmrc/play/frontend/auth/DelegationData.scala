package uk.gov.hmrc.play.frontend.auth

import uk.gov.hmrc.play.frontend.auth.connectors.domain.Accounts

private[auth] case class DelegationData(principalName: String, attorneyName: String, accounts: Accounts, link: Link, supplementaryData: Map[String, String] = Map.empty) {
  val attorney = Attorney(attorneyName, link, supplementaryData)
}

private[auth] sealed trait DelegationState

private[auth] case object DelegationOn extends DelegationState {
  override val toString = "On"
}

private[auth] case object DelegationOff extends DelegationState {
  override val toString = "Off"
}

private[auth] object DelegationState {

  def apply(value: Option[String]): DelegationState = {
    if (value == Some("On")) DelegationOn else DelegationOff
  }
}

