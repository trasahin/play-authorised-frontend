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

import java.util.UUID

import org.joda.time.{DateTime, DateTimeZone}
import play.api.mvc.{Action, Controller, Cookie, Session}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.AuthorisedSessionTimeoutWrapper._
import uk.gov.hmrc.play.http.SessionKeys
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

class SessionTimeoutWrapperSpec extends UnitSpec with WithFakeApplication {

  lazy val accountLoginPage = "/account"
  val hypotheticalCurrentTime = new DateTime(2012, 7, 7, 4, 6, 20, DateTimeZone.UTC)
  val invalidTime = hypotheticalCurrentTime.minusDays(1).getMillis.toString
  val justInvalidTime = hypotheticalCurrentTime.minusSeconds(timeoutSeconds + 1).getMillis.toString
  val justValidTime = hypotheticalCurrentTime.minusSeconds(timeoutSeconds - 1).getMillis.toString
  val validTime = hypotheticalCurrentTime.minusSeconds(1).getMillis.toString

  val now: () => DateTime = () => hypotheticalCurrentTime

  object AnyAuthenticationProviderForTest extends AnyAuthenticationProvider {
    override def login: String = accountLoginPage
    override def ggwAuthenticationProvider: GovernmentGateway = new GovernmentGateway { override def login: String = accountLoginPage }
    override def verifyAuthenticationProvider: Verify = new Verify { override def login: String = accountLoginPage }
  }

  object TestController extends Controller with SessionTimeoutWrapper {

    override def now = SessionTimeoutWrapperSpec.this.now

    def testWithNewSessionTimeout = WithNewSessionTimeout(Action {
      request =>
        Ok("")
    })

    def testWithNewSessionTimeoutAddingData = WithNewSessionTimeout(Action {
      request =>
        Ok("").withSession(SessionKeys.userId -> "Jim")
    })

    val addedCookie = Cookie("another", "cookie")
    def testWithNewSessionTimeoutAddingCookie = WithNewSessionTimeout(Action {
      request =>
        Ok("").withCookies(addedCookie)
    })

    def testWithSessionTimeoutValidation = WithSessionTimeoutValidation(AnyAuthenticationProviderForTest)(Action {
      request =>
        Ok("").withSession(SessionKeys.userId -> "Tim")
    })
  }


  "WithNewSessionTimeout" should {
    "add a timestamp to the session if the session is missing" in  {
      val result = TestController.testWithNewSessionTimeout(FakeRequest())
      session(result) shouldBe Session(Map(SessionKeys.lastRequestTimestamp -> now().getMillis.toString))
    }

    "add a timestamp to the session but maintain the other values if the incoming session is not empty" in  {
      val sessionId = s"session-${UUID.randomUUID}"
      val result = TestController.testWithNewSessionTimeout(FakeRequest().withSession(SessionKeys.userId -> sessionId))
      session(result) shouldBe Session(Map(SessionKeys.lastRequestTimestamp -> now().getMillis.toString, SessionKeys.userId -> sessionId))
    }

    "add a timestamp to the session but maintain other session values which have been set in the response" in  {
      val sessionId = s"session-${UUID.randomUUID}"
      val result = TestController.testWithNewSessionTimeoutAddingData(FakeRequest().withSession(SessionKeys.userId -> sessionId))
      session(result) shouldBe Session(Map(SessionKeys.lastRequestTimestamp -> now().getMillis.toString, SessionKeys.userId -> "Jim"))
    }

    "add a timestamp to the session but maintain other cookies which have been set in the response" in  {
      val sessionId = s"session-${UUID.randomUUID}"
      val result = TestController.testWithNewSessionTimeoutAddingCookie(FakeRequest().withSession(SessionKeys.userId -> sessionId))
      session(result) shouldBe Session(Map(SessionKeys.lastRequestTimestamp -> now().getMillis.toString, SessionKeys.userId -> sessionId))
      cookies(result) should contain (TestController.addedCookie)
    }
  }

  "WithSessionTimeoutValidation" should {
    "redirect to the login page with a new session containing only a timestamp if the incoming timestamp is invalid" in  {
      val result = TestController.testWithSessionTimeoutValidation(FakeRequest().withSession(SessionKeys.lastRequestTimestamp -> invalidTime))
      session(result) shouldBe Session(Map(SessionKeys.lastRequestTimestamp -> now().getMillis.toString))
      redirectLocation(result) shouldBe Some(accountLoginPage)
    }

    "redirect to the login page with a new session containing only a timestamp if the incoming timestamp is just invalid" in  {
      val result = TestController.testWithSessionTimeoutValidation(FakeRequest().withSession(SessionKeys.lastRequestTimestamp -> justInvalidTime))
      session(result) shouldBe Session(Map(SessionKeys.lastRequestTimestamp -> now().getMillis.toString))
      redirectLocation(result) shouldBe Some(accountLoginPage)
    }

    "perform the wrapped action successfully if the incoming session is empty" in  {
      val result = TestController.testWithSessionTimeoutValidation(FakeRequest())
      session(result) shouldBe Session(Map(SessionKeys.lastRequestTimestamp -> now().getMillis.toString, SessionKeys.userId -> "Tim"))
      status(result) shouldBe 200
    }

    "perform the wrapped action successfully and update the timestamp if the incoming timestamp is just valid when a custom error path is given" in  {
      val result = TestController.testWithSessionTimeoutValidation()(FakeRequest().withSession(SessionKeys.lastRequestTimestamp -> justValidTime))
      session(result) shouldBe Session(Map(SessionKeys.lastRequestTimestamp -> now().getMillis.toString, SessionKeys.userId -> "Tim"))
      status(result) shouldBe 200
    }

    "perform the wrapped action successfully and update the timestamp if the incoming timestamp is just valid" in  {
      val result = TestController.testWithSessionTimeoutValidation(FakeRequest().withSession(SessionKeys.lastRequestTimestamp -> justValidTime))
      session(result) shouldBe Session(Map(SessionKeys.lastRequestTimestamp -> now().getMillis.toString, SessionKeys.userId -> "Tim"))
      status(result) shouldBe 200
    }
    "perform the wrapped action successfully and update the timestamp if the incoming timestamp is valid" in  {
      val result = TestController.testWithSessionTimeoutValidation(FakeRequest().withSession(SessionKeys.lastRequestTimestamp -> validTime))
      session(result) shouldBe Session(Map(SessionKeys.lastRequestTimestamp -> now().getMillis.toString, SessionKeys.userId -> "Tim"))
      status(result) shouldBe 200
    }
  }

}
