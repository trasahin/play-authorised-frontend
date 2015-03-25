package uk.gov.hmrc.play.frontend.auth

import play.api.mvc.Results._
import play.api.mvc.{Result, _}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent._

trait PageVisibilityPredicate {
  def isVisible(user: AuthContext, request: Request[AnyContent]): Future[Boolean]

  def nonVisibleResult: Result = NotFound
}

private[auth] object WithPageVisibility {


  def apply(predicate: PageVisibilityPredicate, user: AuthContext)(action: AuthContext => Action[AnyContent]): Action[AnyContent] =

    Action.async {
      request =>
        implicit val hc = HeaderCarrier.fromSessionAndHeaders(request.session, request.headers)
        predicate.isVisible(user, request).flatMap { visible =>
          if (visible)
            action(user)(request)
          else
            Action(predicate.nonVisibleResult)(request)
        }
    }
}

object DefaultPageVisibilityPredicate extends PageVisibilityPredicate {
  def isVisible(user: AuthContext, request: Request[AnyContent]) = Future.successful(true)
}
