package uk.gov.hmrc.play.frontend.auth

import uk.gov.hmrc.play.test.UnitSpec

class LoggedInUserSpec extends UnitSpec {

  "The oid method" should {

    val user = LoggedInUser(userId = "/auth/oid/1234567890", loggedInAt = None, previouslyLoggedInAt = None, governmentGatewayToken = None)

    "return the value after the last slash" in {
      user.oid shouldBe "1234567890"
      user.copy(userId = "/abc/123/456").oid shouldBe "456"
      user.copy(userId = "/abcde").oid shouldBe "abcde"
    }

    "just return the userId if there is no slash" in {
      user.copy(userId = "abcd").oid shouldBe "abcd"
    }
  }
}