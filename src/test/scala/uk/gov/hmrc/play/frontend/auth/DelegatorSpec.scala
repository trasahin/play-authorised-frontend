package uk.gov.hmrc.play.frontend.auth

import org.mockito.Mockito
import org.scalatest.mock.MockitoSugar
import play.api.mvc.{RequestHeader, Results}
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.{AgentCode, AgentUserId, Nino, SaUtr}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.auth.frontend.connectors.domain._
import uk.gov.hmrc.play.frontend.auth.connectors.DelegationConnector
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.Future

class DelegatorSpec extends UnitSpec with WithFakeApplication with Results {

  import org.mockito.Mockito._

  implicit val authContext = new AuthContext {

    override def attorney: Option[Attorney] = None

    override def user: LoggedInUser = LoggedInUser("/auth/oid/1234", None, None, None)

    override def principal: Principal = Principal(Some("Dave Agent"), accounts = Accounts(agent = Some(AgentAccount(
      link = "http//agent/4567",
      agentCode = AgentCode("4567"),
      agentUserId = AgentUserId("ABC"),
      agentUserRole = AgentAdmin,
      payeReference = None
    ))))
  }

  implicit val hc = new HeaderCarrier()

  "The startDelegation method" should {

    "pass the delegation data to the connector and return a redirect response with the delegation session flag" in new TestCase {

      implicit val request: RequestHeader = FakeRequest()

      val delegationData = DelegationData(
        principalName = "Dave Client",
        attorneyName = "Bob Agent",
        accounts = Accounts(
          paye = Some(PayeAccount(link = "http://paye/some/path", nino = Nino("AB123456D"))),
          sa = Some(SaAccount(link = "http://sa/some/utr", utr = SaUtr("1234567890")))
        ),
        link = Link(url = "http://taxplatform/some/dashboard", text = "Back to dashboard")
      )

      val redirectTo = "http://blah/blah"

      when(mockDelegationConnector.startDelegation("1234", delegationData)).thenReturn(Future.successful(()))

      val result = await(delegator.startDelegation(delegationData, redirectTo))

      result.header.status shouldBe 303
      result.header.headers.get("Location") shouldBe Some("http://blah/blah")

      UserSessionData(result.session).delegationState shouldBe DelegationOn

      verify(mockDelegationConnector).startDelegation("1234", delegationData)
    }
  }

  "The endDelegation method" should {

    "call the delegation connector to end delegation and remove the delegation session flag" in new TestCase {

      implicit val request: RequestHeader = FakeRequest().withSession(UserSessionData.DelegationStateSessionKey -> DelegationOn.toString)

      assert(UserSessionData(request.session).delegationState == DelegationOn)

      when(mockDelegationConnector.endDelegation("1234")).thenReturn(Future.successful(()))

      val result = await(delegator.endDelegation(Ok))

      UserSessionData(result.session).delegationState shouldBe DelegationOff

      result.header.status shouldBe 200

      verify(mockDelegationConnector).endDelegation("1234")
    }
  }

  trait TestCase extends MockitoSugar {

    val mockDelegationConnector = mock[DelegationConnector]

    val delegator = new Delegator {
      override protected def delegationConnector: DelegationConnector = mockDelegationConnector
    }
  }
}
