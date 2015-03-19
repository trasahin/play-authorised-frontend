package uk.gov.hmrc.play.frontend.auth.connectors

import play.api.libs.json.Json
import uk.gov.hmrc.play.audit.http.config.{AuditingConfig, LoadAuditingConfig}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.{AppName, RunMode, ServicesConfig}
import uk.gov.hmrc.play.frontend.auth.{Link, DelegationData}
import uk.gov.hmrc.play.http.ws.WSHttp
import uk.gov.hmrc.play.http.{HttpDelete, HttpGet, HttpPut}

import scala.concurrent.Future

private[auth] trait DelegationConnector {

  protected def serviceUrl: String

  protected def http: HttpGet with HttpPut with HttpDelete

  import DelegationFormatters._

  def get(oid: String): Future[Option[DelegationData]] = ???

  def put(oid: String, delegationData: DelegationData): Future[Unit] = ???

  def delete(oid: String): Future[Unit] = ???
}


//private[auth] object DelegationConnector extends DelegationConnector with ServicesConfig {
//
//  override protected val serviceUrl = baseUrl("delegation")
//
//  override protected lazy val http = new WSHttp with AppName with RunMode {
//    override def auditConnector: AuditConnector = new AuditConnector {
//      override def auditingConfig: AuditingConfig = LoadAuditingConfig(s"$env.auditing")
//    }
//  }
//}

private object DelegationFormatters {
  implicit val linkFormat = Json.format[Link]
  implicit val format = Json.format[DelegationData]
}
