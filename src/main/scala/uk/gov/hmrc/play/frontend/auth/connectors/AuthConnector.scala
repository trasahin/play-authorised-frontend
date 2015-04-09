package uk.gov.hmrc.play.frontend.auth.connectors

import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Authority
import uk.gov.hmrc.play.http.HttpGet
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

trait AuthConnector {

  val serviceUrl: String

  def http: HttpGet

  def currentAuthority(implicit hc: HeaderCarrier): Future[Option[Authority]] = {
    http.GET[Authority](s"$serviceUrl/auth/authority").map(Some.apply) // Option return is legacy of previous http library now baked into this class's api
  }
}
