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
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{Accounts, Authority, LevelOfAssurance, SaAccount}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.SessionKeys
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.time.DateTimeUtils.now

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuthorisedForActionSpec extends UnitSpec with BeforeAndAfterEachTestData with MockitoSugar with WithFakeApplication {

  val mockAuthConnector = mock[AuthConnector]

  val testedActions = new TestController with Actions {
    override protected implicit def authConnector: AuthConnector = mockAuthConnector
  }

  override protected def beforeEach(testData: TestData) {
    reset(mockAuthConnector)
  }

  "asserting authorization" should {
    "redirect to the login page when the userId is not found in the session " in  {
      val result = testedActions.testAuthorisation(FakeRequest())

      status(result) should be (303)
      redirectLocation(result).get shouldBe "/login"
    }

    "contain the result of the controller action in the response" in  {
      when(mockAuthConnector.currentAuthority(Matchers.any())).thenReturn(Some(saAuthority("jdensmore", "AB123456C")))
      val result = testedActions.testAuthorisation(requestFromLoggedInUser)

      status(result) should be (200)
      contentAsString(result) should include("jdensmore")
    }

    "respond 403 if the user has insufficient LoA" in  {
      when(mockAuthConnector.currentAuthority(Matchers.any())).thenReturn(Some(lowAssuranceUser))
      val result = testedActions.testAuthorisation(requestFromLoggedInUser)

      status(result) should be (403)
    }
  }

  def lowAssuranceUser: Authority = {
    Authority(s"/auth/oid/jdensmore", Accounts(sa = Some(SaAccount(s"/sa/individual/AB123456C", SaUtr("AB123456C")))), None, None, LevelOfAssurance.LOA_1_5)
  }

  def requestFromLoggedInUser: FakeRequest[AnyContentAsEmpty.type] = {
    FakeRequest().withSession(
      SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
      SessionKeys.lastRequestTimestamp -> now.getMillis.toString,
      SessionKeys.userId -> "/auth/oid/jdensmore",
      SessionKeys.token -> "validtoken")
  }

  def saAuthority(id: String, utr: String): Authority =
    Authority(s"/auth/oid/$id",  Accounts(sa = Some(SaAccount(s"/sa/individual/$utr", SaUtr(utr)))), None, None, LevelOfAssurance.LOA_2)
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