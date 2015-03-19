package uk.gov.hmrc.play.frontend.auth

import uk.gov.hmrc.play.auth.frontend.connectors.domain.Accounts

case class DelegationData(principalName: String, attorneyName: String, accounts: Accounts, returnLink: Link) {
  val attorney = Attorney(attorneyName, returnLink)
}

sealed trait DelegationState

case object DelegationOn extends DelegationState {
  override val toString = "On"
}

case object DelegationOff extends DelegationState {
  override val toString = "Off"
}

object DelegationState {

  def apply(value: Option[String]): DelegationState = {
    if (value == Some("On")) DelegationOn else DelegationOff
  }
}