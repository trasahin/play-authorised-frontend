package uk.gov.hmrc.play.frontend.auth

import play.api.mvc.{Action, AnyContent, Request, Result}

import scala.concurrent.Future

trait AuthenticatedAction {
  def apply(body: (AuthContext => (Request[AnyContent]) => Result)): Action[AnyContent]
  def async(body: (AuthContext => (Request[AnyContent]) => Future[Result])): Action[AnyContent]
}
