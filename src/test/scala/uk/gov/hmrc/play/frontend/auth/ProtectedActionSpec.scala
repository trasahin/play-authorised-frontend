package uk.gov.hmrc.play.frontend.auth

import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.auth.connectors.domain._
import uk.gov.hmrc.play.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.time.DateTimeUtils

import scala.concurrent.Future


class ProtectedActionSpec extends UnitSpec with ProtectedActionHelper with MockitoSugar with WithFakeApplication {

  def defaultHandleNotAuthenticatedAction = Results.Redirect("/login")

  val authconnectorMock = mock[AuthConnector]

  class TestController(redirectURL: => Result = defaultHandleNotAuthenticatedAction, userAccount: Account = PayeAccount("", Nino("AB123456C")), body: => Result = Results.Ok("Success")) extends Controller {
    def securePage = ProtectedAction(AuthRequirements(200, PayeAccount("", Nino("AB123456C"))), redirectURL, authconnectorMock) {
      implicit authContext =>
        implicit request =>
          body
    }
  }


  "ProtectedAction" should {

    "redirect to Login if userId is not found in the session" in new TestController {
      val result = securePage(FakeRequest())
      status(result) should be(303)
      redirectLocation(result).get shouldBe "/login"
    }

    "redirect to the specified login page if userId is not found in the session" in new TestController(Results.Redirect("/login2")) {
      val result = securePage(FakeRequest())
      status(result) should be(303)
      redirectLocation(result).get shouldBe "/login2"
    }

    "execute specified function (redirect) if userId is not found in the session" in new TestController(Results.Redirect("/login2")) {
      val result = securePage(FakeRequest())
      status(result) should be(303)
      redirectLocation(result).get shouldBe "/login2"
    }

    "execute specified function (ok) if userId is not found in the session" in new TestController(Results.Ok("/login2")) {
      val result = securePage(FakeRequest())
      status(result) should be(200)
      await(bodyOf(result)) should be("/login2")
    }

    "execute the specified body if user is logged in " in new TestController(body = Results.Ok("Hello World")) {
      when(authconnectorMock.currentAuthority(any[HeaderCarrier])).thenReturn(Future.successful(Some(Authority("user-1", Accounts(), Some(DateTimeUtils.now), None, LevelOfAssurance.LOA_1 ))))
      val result = securePage(FakeRequest().withSession(SessionKeys.userId -> "user-1"))
      status(result) should be(200)
      await(bodyOf(result)) should be("Hello World")
    }

    "redirect to Login if the user doesn't have the specified Account" in new TestController() {
      when(authconnectorMock.currentAuthority(any[HeaderCarrier])).thenReturn(Future.successful(Some(Authority("user-1", Accounts(), Some(DateTimeUtils.now), None, LevelOfAssurance.LOA_1 ))))
      val result = securePage(FakeRequest().withSession(SessionKeys.userId -> "user-1"))
      status(result) should be(303)
      redirectLocation(result).get shouldBe "/login"
    }
  }
}