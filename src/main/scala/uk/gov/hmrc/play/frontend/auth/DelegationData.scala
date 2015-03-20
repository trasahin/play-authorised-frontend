package uk.gov.hmrc.play.frontend.auth

import uk.gov.hmrc.play.auth.frontend.connectors.domain.Accounts

case class DelegationData(principalName: String, attorneyName: String, accounts: Accounts, link: Link) {
  val attorney = Attorney(attorneyName, link)
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