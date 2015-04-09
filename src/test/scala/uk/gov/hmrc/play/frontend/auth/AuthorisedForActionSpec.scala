package uk.gov.hmrc.play.frontend.auth

import java.util.UUID

import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEachTestData, TestData}
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{Accounts, Authority}
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, domain}
import uk.gov.hmrc.play.http.SessionKeys
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.time.DateTimeUtils.now

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuthorisedForActionSpec extends UnitSpec with BeforeAndAfterEachTestData with MockitoSugar with WithFakeApplication {

  val mockAuthConnector = mock[AuthConnector]

  val testController = new TestController with Actions {
    override protected implicit def authConnector: AuthConnector = mockAuthConnector
  }

  override protected def beforeEach(testData: TestData) {
    reset(mockAuthConnector)

    //FIXME: mocking expectation should not be done in the before callback EVER!
    // It makes refactoring so much harder later on. Move this out and defined for each that requires it
    when(mockAuthConnector.currentAuthority(Matchers.any())).thenReturn(Some(saAuthority("jdensmore", "AB123456C")))
  }

  "basic homepage test" should {
    "contain the result of the controller action in the response" in  {
      val result = testController.testAuthorisation(FakeRequest().withSession(
        SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
        SessionKeys.lastRequestTimestamp -> now.getMillis.toString,
        SessionKeys.userId -> "/auth/oid/jdensmore",
        SessionKeys.token -> "validtoken")
      )

      status(result) should equal(200)
      contentAsString(result) should include("jdensmore")
    }
  }

  "AuthorisedForIdaAction" should {
    "return redirect to login if no Authority is returned from the Auth service" in  {
      when(mockAuthConnector.currentAuthority(Matchers.any[HeaderCarrier])).thenReturn(None)

      val result = testController.testAuthorisation(FakeRequest().withSession(
        SessionKeys.sessionId -> s"session-${UUID.randomUUID().toString}",
        SessionKeys.lastRequestTimestamp -> now.getMillis.toString,
        SessionKeys.userId -> "/auth/oid/jdensmore")
      )

      status(result) should equal(303)
      redirectLocation(result) shouldBe Some("/login")
    }

    "redirect to the login page when the userId is not found in the session " in  {
      val result = testController.testAuthorisation(FakeRequest().withSession(
        SessionKeys.lastRequestTimestamp -> now.getMillis.toString
      ))

      status(result) should equal(303)
      redirectLocation(result).get shouldBe "/login"
    }
  }

  def saAuthority(id: String, utr: String): Authority =
    Authority(s"/auth/oid/$id",  Accounts(sa = Some(domain.SaAccount(s"/sa/individual/$utr", SaUtr(utr)))), None, None)

}

sealed class TestController
  extends Controller {

  this: Authoriser =>

  def testAuthorisation = AuthorisedFor(TestTaxRegime) {
    implicit authContext =>
      implicit request =>
        Ok("jdensmore")
  }

  def testAuthorisationWithRedirectCommand = AuthenticatedBy(authenticationProvider = TestAuthenticationProvider, redirectToOrigin = true) {
    implicit authContext =>
      implicit request =>
        Ok("jdensmore")
  }

  def testThrowsException = AuthorisedFor(TestTaxRegime) {
    implicit authContext =>
      implicit request =>
        throw new RuntimeException("ACTION TEST")
  }
}

object TestAuthenticationProvider extends AuthenticationProvider {

  override val id = "TST"

  def login = "/login"

  def redirectToLogin(redirectToOrigin: Boolean)(implicit request: Request[AnyContent]) = Future.successful(Results.Redirect(login))

  def handleNotAuthenticated(redirectToOrigin: Boolean)(implicit request: Request[AnyContent]): PartialFunction[UserCredentials, Future[Either[AuthContext, FailureResult]]] = {
    case UserCredentials(None, None) =>
      redirectToLogin(redirectToOrigin).map(Right(_))
    case UserCredentials(Some(userId), None) =>
      redirectToLogin(redirectToOrigin).map(Right(_))
  }
}

object TestTaxRegime extends TaxRegime {

  def isAuthorised(accounts: Accounts) = accounts.sa.isDefined

  def authenticationType = TestAuthenticationProvider
}