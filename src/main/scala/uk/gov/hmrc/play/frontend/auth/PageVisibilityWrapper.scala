package uk.gov.hmrc.play.frontend.auth

import play.api.mvc.Results._
import play.api.mvc.{Result, _}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.connectors.domain.LevelOfAssurance.LevelOfAssurance
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent._

trait PageVisibilityPredicate {
  def isVisible(authContext: AuthContext, request: Request[AnyContent]): Future[Boolean]

  def nonVisibleResult: Result = NotFound
}

case class LoaPredicate(requiredLevelOfAssurance: LevelOfAssurance) extends PageVisibilityPredicate {
  override def isVisible(authContext: AuthContext, request: Request[AnyContent]): Future[Boolean] =
    Future.successful(authContext.user.levelOfAssurance >= requiredLevelOfAssurance)

  override def nonVisibleResult: Result = Unauthorized // TODO: other ways to do xx page?
}

private[auth] object WithPageVisibility {


  def apply(predicate: PageVisibilityPredicate, authContext: AuthContext)(action: AuthContext => Action[AnyContent]): Action[AnyContent] =

    Action.async {
      request =>
        implicit val hc = HeaderCarrier.fromSessionAndHeaders(request.session, request.headers)
        predicate.isVisible(authContext, request).flatMap { visible =>
          if (visible)
            action(authContext)(request)
          else
            Action(predicate.nonVisibleResult)(request)
        }
    }
}

object DefaultPageVisibilityPredicate extends PageVisibilityPredicate {
  def isVisible(authContext: AuthContext, request: Request[AnyContent]) = Future.successful(true)
}
