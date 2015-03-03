package uk.gov.hmrc.play.frontend.auth

import org.joda.time.DateTime
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ShouldMatchers, WordSpec}
import play.api.mvc.Controller
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.domain.EmpRef
import uk.gov.hmrc.play.auth.frontend.connectors.domain.{Accounts, Authority, EpayeAccount}

import scala.concurrent.Future


class AuthorisorSpec extends WordSpec with ShouldMatchers with MockitoSugar{
  val userAuthority: Authority = new Authority("uri",
                                              Accounts(epaye = Some(EpayeAccount("line", EmpRef("foo", "bar")))),
                                              Some(new DateTime()),
                                              None)
  private val mockUser: User = User("bilbo/baggins", userAuthority)
  private val mockProvider = mock[AuthenticationProvider]

  class TestController extends Controller {
    this: Authoriser =>

    def synchronousAuthorisedFor() = AuthorisedFor(TestTaxRegime) { implicit user => implicit request =>
      user shouldBe mockUser
      Ok("Woohoo")
    }

    def asynchronousAuthorisedFor() = AuthorisedFor(TestTaxRegime).async { implicit user => implicit request =>
      user shouldBe mockUser
      Future.successful(Ok("Booyah"))
    }

    def synchronousAuthenticatedBy() = AuthenticatedBy(mockProvider) { implicit user => implicit request =>
      user shouldBe mockUser
      Ok("Woohoo")
    }

    def asynchronousAuthenticatedBy() = AuthenticatedBy(mockProvider).async { implicit user => implicit request =>
      user shouldBe mockUser
      Future.successful(Ok("Booyah"))
    }
  }

  val stubbedController = new TestController with StubbedAuthoriser {
    override val user = mockUser
  }

  val fakeRequest = FakeRequest("GET", "/foo")

  "Stubbed Authorisor" should {
    "call a synchronous route with the supplied user with AuthorisedFor" in {
      val future = stubbedController.synchronousAuthorisedFor()(fakeRequest)
      contentAsString(future) shouldBe "Woohoo"
    }

    "call an asynchronous route with the supplied user with AuthorisedFor" in {
      val future = stubbedController.asynchronousAuthorisedFor()(fakeRequest)
      contentAsString(future) shouldBe "Booyah"
    }

    "call a synchronous route with the supplied user with AuthenticatedBy" in {
      val future = stubbedController.synchronousAuthenticatedBy()(fakeRequest)
      contentAsString(future) shouldBe "Woohoo"
    }

    "call an asynchronous route with the supplied user with AuthenticatedBy" in {
      val future = stubbedController.asynchronousAuthenticatedBy()(fakeRequest)
      contentAsString(future) shouldBe "Booyah"
    }
  }
}
