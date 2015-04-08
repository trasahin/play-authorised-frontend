package uk.gov.hmrc.play.frontend.auth.connectors

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import org.scalatest.{Suite, BeforeAndAfterEach, BeforeAndAfterAll}
import uk.gov.hmrc.play.http.ws.PortTester

trait WireMockedSpec extends BeforeAndAfterAll with BeforeAndAfterEach {

  self: Suite =>

  val Port = PortTester.findPort()

  private val wireMockServer = new WireMockServer(wireMockConfig().port(Port))

  override def beforeAll() {
    wireMockServer.start()
    WireMock.configureFor("localhost", Port)
    super.beforeAll()
  }

  override def beforeEach() {
    WireMock.reset()
    super.beforeEach()
  }

  override def afterAll() {
    super.afterAll()
    wireMockServer.stop()
  }
}
