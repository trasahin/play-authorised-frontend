package uk.gov.hmrc.play.frontend.auth

import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.play.auth.frontend.connectors.domain.Authority
import uk.gov.hmrc.play.test.UnitSpec

class UserSpec extends UnitSpec with MockitoSugar {

  "a Government Gateway User" should {

    "have their display name the same as name form GG" in {

      val user = User("id", mock[Authority], Some("John Small"), None)

      user.displayName shouldBe Some("John Small")

    }

  }
}
