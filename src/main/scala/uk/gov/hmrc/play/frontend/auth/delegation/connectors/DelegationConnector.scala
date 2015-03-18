package uk.gov.hmrc.play.frontend.auth.delegation.connectors

import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.frontend.auth.delegation.DelegationData
import uk.gov.hmrc.play.http.{HttpDelete, HttpGet, HttpPut}

import scala.concurrent.Future

private[auth] trait DelegationConnector extends ServicesConfig {

  protected def serviceUrl: String

  protected def http: HttpGet with HttpPut with HttpDelete

  def get(oid: String): Future[DelegationData] = ???

  def put(oid: String, delegationData: DelegationData): Future[Unit] = ???

  def delete(oid: String): Future[Unit] = ???
}


//private[auth] object DelegationConnector extends DelegationConnector {
//
//  override protected val serviceUrl = baseUrl("delegation")
//
//  override protected lazy val http = new WSHttp with AppName with RunMode {
//    override def auditConnector: AuditConnector = new AuditConnector {
//      override def auditingConfig: AuditingConfig = LoadAuditingConfig(s"$env.auditing")
//    }
//  }
//}
