package uk.gov.hmrc.play.frontend.auth

import uk.gov.hmrc.play.frontend.auth.connectors.domain.LevelOfAssurance._
import uk.gov.hmrc.play.test.UnitSpec

class LoggedInUserSpec extends OidConversionSpec[LoggedInUser] with UnitSpec {

  "The oid method" should {
    successfullyConvertUserIdsToOids()
  }

  override protected def constructWithUserId(userId: String) = LoggedInUser(
    userId = userId, loggedInAt = None, previouslyLoggedInAt = None, governmentGatewayToken = None, levelOfAssurance = LOA_2
  )

  override protected def oid(user: LoggedInUser): String = user.oid
}