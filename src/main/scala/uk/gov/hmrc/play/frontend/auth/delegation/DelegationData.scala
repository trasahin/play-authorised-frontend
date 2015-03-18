package uk.gov.hmrc.play.frontend.auth.delegation

import play.api.libs.json.Json
import uk.gov.hmrc.play.auth.frontend.connectors.domain.Accounts

case class DelegationData(principalName: String, attorneyName: String, accounts: Accounts)

private[delegation] object DelegationData {
  implicit val format = Json.format[DelegationData]
}
