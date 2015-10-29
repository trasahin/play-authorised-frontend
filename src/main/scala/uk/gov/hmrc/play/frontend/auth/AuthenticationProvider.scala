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

import play.api.Logger
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.play.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext.fromLoggingDetails

import scala.concurrent._

case class UserCredentials(userId: Option[String], token: Option[String])

object AuthenticationProviderIds {
  val GovernmentGatewayId = "GGW"
  val AnyAuthenticationProviderId = "IDAorGGW"
  val VerifyProviderId = "IDA"
}

trait AuthenticationProvider {
  type FailureResult = Result

  def id: String

  def redirectToLogin(redirectToOrigin: Boolean = false)(implicit request: Request[AnyContent]): Future[Result]

  def handleSessionTimeout(implicit request: Request[AnyContent]): Future[Result] = redirectToLogin(redirectToOrigin = false)

  def handleNotAuthenticated(redirectToOrigin: Boolean)(implicit request: Request[AnyContent]): PartialFunction[UserCredentials, Future[Either[AuthContext, FailureResult]]]

  def handleAuthenticated(implicit request: Request[AnyContent]): PartialFunction[UserCredentials, Future[Either[AuthContext, Result]]] = PartialFunction.empty

  implicit def hc(implicit request: Request[_]) = HeaderCarrier.fromHeadersAndSession(request.headers,Some(request.session) )
}

trait GovernmentGateway extends AuthenticationProvider {

  override val id = AuthenticationProviderIds.GovernmentGatewayId

  def login: String

  def redirectToLogin(redirectToOrigin: Boolean)(implicit request: Request[AnyContent]) = Future.successful(Redirect(login))

  def handleNotAuthenticated(redirectToOrigin: Boolean)(implicit request: Request[AnyContent]): PartialFunction[UserCredentials, Future[Either[AuthContext, FailureResult]]] = {
    case UserCredentials(None, token@_) =>
      Logger.info(s"No userId found - redirecting to login. user: None token : $token")
      redirectToLogin(redirectToOrigin).map(Right(_))
    case UserCredentials(Some(userId), None) =>
      Logger.info(s"No gateway token - redirecting to login. user : $userId token : None")
      redirectToLogin(redirectToOrigin).map(Right(_))
  }
}

trait Verify extends AuthenticationProvider {

  override val id = AuthenticationProviderIds.VerifyProviderId

  def login: String

  def redirectToLogin(redirectToOrigin: Boolean)(implicit request: Request[AnyContent]) = Future.successful(Redirect(login))

  def handleNotAuthenticated(redirectToOrigin: Boolean)(implicit request: Request[AnyContent]): PartialFunction[UserCredentials, Future[Either[AuthContext, FailureResult]]] = {
    case UserCredentials(None, None) =>
      Logger.info(s"No userId found - unauthorized. user: None")
      redirectToLogin(redirectToOrigin).map(Right(_))
  }
}

trait AnyAuthenticationProvider extends AuthenticationProvider {

  val id = AuthenticationProviderIds.AnyAuthenticationProviderId

  def ggwAuthenticationProvider: GovernmentGateway
  def verifyAuthenticationProvider: Verify

  def login: String

  def redirectToLogin(redirectToOrigin: Boolean)(implicit request: Request[AnyContent]) = Future.successful(Redirect(login))

  private def handleMissingProvider(redirectToOrigin: Boolean)(implicit request: Request[AnyContent]): PartialFunction[UserCredentials, Future[Either[AuthContext, FailureResult]]] = {
    case _ =>
      Logger.info("No provider in the session")
      redirectToLogin(redirectToOrigin).map(Right(_))
  }

  def handleNotAuthenticated(redirectToOrigin: Boolean)(implicit request: Request[AnyContent]) =
    request.session.get(SessionKeys.authProvider) match {
      case Some(AuthenticationProviderIds.GovernmentGatewayId) =>
        ggwAuthenticationProvider.handleNotAuthenticated(redirectToOrigin)
      case Some(AuthenticationProviderIds.VerifyProviderId) =>
        verifyAuthenticationProvider.handleNotAuthenticated(redirectToOrigin)
      case _ =>
        handleMissingProvider(redirectToOrigin)
    }
}
