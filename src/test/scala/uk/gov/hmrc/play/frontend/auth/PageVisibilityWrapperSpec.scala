/*
 * Copyright 2015 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
        def isVisible(authContext: AuthContext) = Future.successful(true)
      }
      val authContextMock = mock[AuthContext]

      val result = DummyController.action(positivePredicate, authContextMock)

      status(result.apply(FakeRequest())) shouldBe 200
    }

    "Non authorise the action requested if the predicate is false" in {

      val negativePredicate = new PageVisibilityPredicate {
        def isVisible(authContext: AuthContext) = Future.successful(false)
      }

      val authContextMock = mock[AuthContext]

      val result = DummyController.action(negativePredicate, authContextMock)

      status(result.apply(FakeRequest())) shouldBe 404

    }
  }


  object DummyController extends Controller {

    def action(predicate: PageVisibilityPredicate, authContextMock: AuthContext) = WithPageVisibility(predicate, authContextMock) {
      authContext =>
        Action(Ok)
    }
  }

}
