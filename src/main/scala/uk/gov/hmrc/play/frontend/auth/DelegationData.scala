package uk.gov.hmrc.play.frontend.auth

import play.api.libs.json.Json
import uk.gov.hmrc.play.auth.frontend.connectors.domain.Accounts

case class DelegationData(principalName: Option[String], attorneyName: Option[String], accounts: Accounts, returnLink: Link) {
  val attorney = Attorney(attorneyName, returnLink)
}

//private[auth] object DelegationData {
//  implicit val linkFormat = Json.format[Link]
//  implicit val format = Json.format[DelegationData]
//}
