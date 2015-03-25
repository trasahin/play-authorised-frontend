package uk.gov.hmrc.play.frontend.auth

import org.scalatest.mock.MockitoSugar
import play.api.mvc.{Action, AnyContent, Controller, Request}
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class PageVisibilityWrapperSpec extends UnitSpec with MockitoSugar {

  "PageVisibilityWrapper " should {

    "authorise the action requested if the predicate is true" in  {

      val positivePredicate = new PageVisibilityPredicate {
        def isVisible(user: AuthContext, request: Request[AnyContent]) = Future.successful(true)
      }
      val userMock = mock[AuthContext]

      val result = DummyController.action(positivePredicate, userMock)

      status(result.apply(FakeRequest())) shouldBe 200
    }

    "Non authorise the action requested if the predicate is false" in {

      val negativePredicate = new PageVisibilityPredicate {
        def isVisible(user: AuthContext, request: Request[AnyContent]) = Future.successful(false)
      }

      val userMock = mock[AuthContext]

      val result = DummyController.action(negativePredicate, userMock)

      status(result.apply(FakeRequest())) shouldBe 404

    }
  }


  object DummyController extends Controller {

    def action(predicate: PageVisibilityPredicate, user: AuthContext) = WithPageVisibility(predicate, user) {
      user =>
        Action(Ok)
    }
  }

}
