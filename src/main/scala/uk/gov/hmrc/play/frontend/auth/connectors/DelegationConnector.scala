package uk.gov.hmrc.play.frontend.auth.connectors

import play.api.libs.json.Json
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.config.{AuditingConfig, LoadAuditingConfig}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.{AppName, RunMode, ServicesConfig}
import uk.gov.hmrc.play.frontend.auth.{DelegationData, Link}
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.play.http.ws.WSHttp

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

private[auth] trait DelegationConnector {

  protected def serviceUrl: String

  protected def http: HttpGet with HttpPut with HttpDelete

  implicit val linkFormat = Json.format[Link]
  implicit val format = Json.format[DelegationData]

  private def delegationUrl(oid: String): String = s"$serviceUrl/oid/$oid"

  def getDelegationData(oid: String)(implicit hc: HeaderCarrier): Future[Option[DelegationData]] = {

    implicit val responseHandler = new HttpReads[Option[DelegationData]] {
      override def read(method: String, url: String, response: HttpResponse): Option[DelegationData] = {
        response.status match {
          case 200 => Try(response.json.as[DelegationData]) match {
            case Success(data) => Some(data)
            case Failure(e) => throw DelegationServiceException("Unable to parse response", method, url, e)
          }
          case 404 => None
          case unexpectedStatus => throw DelegationServiceException(s"Unexpected response code '$unexpectedStatus'", method, url)
        }
      }
    }

    http.GET[Option[DelegationData]](delegationUrl(oid))
  }

  def startDelegation(oid: String, delegationData: DelegationData)(implicit hc: HeaderCarrier): Future[Unit] = {

    http.PUT[DelegationData](delegationUrl(oid), delegationData, (response: Future[HttpResponse], _: String) => response).map { response =>
      response.status match {
        case 201 => ()
        case unexpectedStatus => throw DelegationServiceException(s"Unexpected response code '$unexpectedStatus'", "PUT", delegationUrl(oid))
      }
    }
  }

  def endDelegation(oid: String)(implicit hc: HeaderCarrier): Future[Unit] = {

    case class DeletionResponse(override val status: Int, cause: Option[Throwable] = None) extends HttpResponse

    val deletionResponse = http.DELETE(delegationUrl(oid)).map(r => DeletionResponse(r.status)).recover {
      case e: HttpException => DeletionResponse(e.responseCode, Some(e))
      case e: Upstream4xxResponse => DeletionResponse(e.upstreamResponseCode, Some(e))
      case e: Upstream5xxResponse => DeletionResponse(e.upstreamResponseCode, Some(e))
    }

    deletionResponse.map {
      case DeletionResponse(204 | 404, _) => ()
      case DeletionResponse(unexpectedStatus, cause) =>
        throw DelegationServiceException(s"Unexpected response code '$unexpectedStatus'", "DELETE", delegationUrl(oid), cause.orNull)
    }
  }
}

case class DelegationServiceException(message: String, method: String, url: String, cause: Throwable = null)
  extends RuntimeException(s"$message: $method $url", cause)

private[auth] object DelegationConnector extends DelegationConnector with ServicesConfig {

  override protected val serviceUrl = baseUrl("delegation")

  override protected lazy val http = new WSHttp with AppName with RunMode {
    override def auditConnector: AuditConnector = new AuditConnector {
      override def auditingConfig: AuditingConfig = LoadAuditingConfig(s"$env.auditing")
    }
  }
}

