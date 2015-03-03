package uk.gov.hmrc.play.frontend.auth

import play.api.mvc.{Action, AnyContent, Request, Result}

import scala.concurrent.Future

trait AuthenticatedAction {
  def apply(body: (User => (Request[AnyContent]) => Result)): Action[AnyContent]
  def async(body: (User => (Request[AnyContent]) => Future[Result])): Action[AnyContent]
}
