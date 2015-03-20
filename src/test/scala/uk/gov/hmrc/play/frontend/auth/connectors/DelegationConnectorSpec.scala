package uk.gov.hmrc.play.frontend.auth.connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.{JsNull, Json}
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.auth.frontend.connectors.domain.{Accounts, PayeAccount, SaAccount}
import uk.gov.hmrc.play.config.{AppName, RunMode}
import uk.gov.hmrc.play.frontend.auth.{DelegationData, Link}
import uk.gov.hmrc.play.http.ws.WSHttp
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

class DelegationConnectorSpec extends UnitSpec with WithFakeApplication with WireMockedSpec {

  private implicit val hc = HeaderCarrier()

  "The get(oid) method" should {

    "return the delegation data returned from the service, if the response code is 200" in new TestCase {

      val oid = "1234"

      val responseBody = Json.obj(
        "attorneyName" -> "Bob Agent",
        "principalName" -> "Dave Client",
        "link" -> Json.obj(
          "url" -> "http://taxplatform/some/dashboard",
          "text" -> "Back to dashboard"
        ),
        "accounts" -> Json.obj(
          "paye" -> Json.obj(
            "link" -> "http://paye/some/path",
            "nino" -> "AB123456D"
          ),
          "ct" -> JsNull,
          "sa" -> Json.obj(
            "link" -> "http://sa/some/utr",
            "utr" -> "1234567890"
          )
        )
      ).toString()

      stubFor(get(urlEqualTo(s"/oid/$oid")).willReturn(aResponse().withStatus(200).withBody(responseBody)))

      await(connector.get(oid)) shouldBe Some(DelegationData(
        principalName = "Dave Client",
        attorneyName = "Bob Agent",
        accounts = Accounts(
          paye = Some(PayeAccount(link = "http://paye/some/path", nino = Nino("AB123456D"))),
          sa = Some(SaAccount(link = "http://sa/some/utr", utr = SaUtr("1234567890")))
        ),
        link = Link(url = "http://taxplatform/some/dashboard", text = "Back to dashboard")
      ))
    }

    "return None when the response code is 404" in new TestCase {

      val oid = "1234"

      stubFor(get(urlEqualTo(s"/oid/$oid")).willReturn(aResponse().withStatus(404)))

      await(connector.get(oid)) shouldBe None
    }

    "throw an exception if the response is anything else" in new TestCase {

      val oid204 = "204oid"
      val oid400 = "400oid"
      val oid500 = "500oid"

      stubFor(get(urlEqualTo(s"/oid/$oid204")).willReturn(aResponse().withStatus(204)))
      stubFor(get(urlEqualTo(s"/oid/$oid400")).willReturn(aResponse().withStatus(400)))
      stubFor(get(urlEqualTo(s"/oid/$oid500")).willReturn(aResponse().withStatus(500)))

      a [DelegationServiceException] should be thrownBy await(connector.get(oid204))
      a [DelegationServiceException] should be thrownBy await(connector.get(oid400))
      a [DelegationServiceException] should be thrownBy await(connector.get(oid500))
    }
  }

  trait TestCase extends MockitoSugar {

    val baseUrl = s"http://localhost:$Port"

    val connector = new DelegationConnector {

      val mockAuditConnector = mock[AuditConnector]

      override protected val serviceUrl = baseUrl

      override protected lazy val http = new WSHttp with AppName with RunMode {
        override def auditConnector: AuditConnector = mock[AuditConnector]
      }
    }
  }
}
