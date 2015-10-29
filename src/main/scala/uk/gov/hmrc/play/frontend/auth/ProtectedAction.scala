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

import org.joda.time.DateTime
import play.api.Logger
import play.api.mvc._
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.time.DateTimeUtils

import scala.concurrent.Future

class ProtectedAction(authRequirements: AuthRequirements, authActionProvider: AuthActionProvider, authConnectorProvided: AuthConnector) extends AuthenticatedAction with SessionTimeoutWrapperX {
  override def apply(body: (AuthContext) => (Request[AnyContent]) => Result): Action[AnyContent] = {
    WithSessionTimeoutValidationX(authActionProvider) {
      Action.async {
        implicit request =>

          val sd = UserSessionData(request.session)
          implicit val hc = HeaderCarrier.fromHeadersAndSession(request.headers, Some(request.session))
          authContextService.currentAuthContext(sd) flatMap {
            case Some(authContext) =>
              AuthReqPageVisibilityPredicate(authRequirements).isVisible(authContext).flatMap {
                case true => Future.successful(body(authContext)(request))
                case _ => authActionProvider.handleNotAuthenticated
              }
            case _ =>
              authActionProvider.handleNotAuthenticated
          }
      }
    }
  }

  override def async(body: (AuthContext) => (Request[AnyContent]) => Future[Result]): Action[AnyContent] = ???

  def authContextService: AuthContextService = new AuthContextService with DelegationDisabled {
    override protected def authConnector: AuthConnector = authConnectorProvided
  }

}

class ProtectedSubAction(authRequirements: AuthRequirements)(implicit authContext: AuthContext) {

  def apply[T](body: => T, fail: => T): T = {
    AuthReqPageVisibilityPredicate(authRequirements).check(authContext) match {
      case true => body
      case _ => fail
    }
  }

  def async[T](body: => T, fail: => T): Future[T] = ???
}

trait ProtectedActionHelper {
  def ProtectedAction(authRequirements: AuthRequirements, handleNotAuthenticated: AuthActionProvider, authConnectorProvided: AuthConnector) = new ProtectedAction(authRequirements, handleNotAuthenticated, authConnectorProvided)

  def ProtectedSubAction(authRequirements: AuthRequirements)(implicit authContext: AuthContext) = new ProtectedSubAction(authRequirements)(authContext)
}


trait AuthActionProvider {
  def handleSessionTimeout(implicit request: Request[AnyContent]): Future[Result]

  def handleNotAuthenticated()(implicit request: Request[AnyContent]): Future[Result]
}

class WithSessionTimeoutValidationX(val now: () => DateTime) extends SessionTimeout {

  def apply(authActionProvider: AuthActionProvider)(action: Action[AnyContent]): Action[AnyContent] = Action.async {
    implicit request: Request[AnyContent] => {
      implicit val loggingDetails = HeaderCarrier.fromHeadersAndSession(request.headers, Some(request.session))
      val result = if (AuthorisedSessionTimeoutWrapper.userNeedsNewSession(request.session, now)) {
        Logger.info(s"request refused as the session had timed out in ${request.path}")
        authActionProvider.handleSessionTimeout.flatMap {
          result =>
            Action(result)(request).map(_.withNewSession)
        }
      } else {
        action(request)
      }
      addTimestamp(request, result)
    }
  }

}

trait SessionTimeoutWrapperX {

  def now: () => DateTime = () => DateTimeUtils.now

  object WithSessionTimeoutValidationX extends WithSessionTimeoutValidationX(now)

  object WithNewSessionTimeout extends WithNewSessionTimeout(now)

}

case class AuthReqPageVisibilityPredicate(authRequirements: AuthRequirements) extends PageVisibilityPredicate {

  override def isVisible(authContext: AuthContext): Future[Boolean] = {
    Future.successful(authContext.principal.accounts.contains(authRequirements.account))
  }

  def check(authContext: AuthContext) : Boolean = {
    authContext.principal.accounts.contains(authRequirements.account)
  }
}

