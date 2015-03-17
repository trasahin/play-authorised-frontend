package uk.gov.hmrc.play.frontend.auth

import java.net.URI

import org.joda.time.DateTime
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.domain.{SaUtr, Nino}
import uk.gov.hmrc.play.auth.frontend.connectors.domain.{SaAccount, PayeAccount, Accounts, Authority}
import uk.gov.hmrc.play.test.UnitSpec

class UserSpec extends UnitSpec with MockitoSugar {

  "a Government Gateway User" should {

    "have their display name the same as name form GG" in {

      val user = User("id", mock[Authority], Some("John Small"), None)

      user.displayName shouldBe Some("John Small")
    }
  }

  "As an AuthContext, a User" should {

    val userId = "/auth/oid/1234567890"

    val accounts = Accounts(
      paye = Some(PayeAccount(link = "/paye/abc", nino = Nino("AB124512C"))),
      sa = Some(SaAccount(link = "/sa/www", utr = SaUtr("1231231233")))
    )

    val authority = Authority(
      uri = userId,
      accounts = accounts,
      loggedInAt = None,
      previouslyLoggedInAt = None
    )

    val user = User(
      userId = userId,
      userAuthority = authority,
      nameFromGovernmentGateway = Some("Bob Client"),
      decryptedToken = Some("token"),
      actingAsAttorneyFor = None,
      attorney = Some(Attorney("Dave Agent", Link(new URI("http://stuff/blah"), "Back to dashboard")))
    )

    "provide the principal field, containing the name from the (deprecated) nameFromGovernmentGateway field and accounts data from the (deprecated) Authority field" in {
      user.principal shouldBe Principal(Some("Bob Client"), accounts)
    }

    "provide the principal field, containing no name if there is none in the (deprecated) nameFromGovernmentGateway field" in {
      user.copy(nameFromGovernmentGateway = None).principal shouldBe Principal(None, accounts)
    }

    "provide a LoggedInUser object containing the login data from the (deprecated) Authority field" in {

      user.user shouldBe LoggedInUser(
        userId = userId,
        loggedInAt = None,
        previouslyLoggedInAt = None,
        governmentGatewayToken = Some("token"))

      val loggedInAt = new DateTime(2014, 11, 12, 4, 56, 22, 231)
      val previouslyLoggedInAt = new DateTime(2012, 9, 1, 13, 52, 2, 999)

      val user2 = user.copy(userAuthority = authority.copy(loggedInAt = Some(loggedInAt)))
      user2.user shouldBe LoggedInUser(userId, Some(loggedInAt), None, Some("token"))

      val user3 = user.copy(userAuthority = authority.copy(previouslyLoggedInAt = Some(previouslyLoggedInAt)))
      user3.user shouldBe LoggedInUser(userId, None, Some(previouslyLoggedInAt), Some("token"))

      val user4 = user.copy(userAuthority = authority.copy(loggedInAt = Some(loggedInAt), previouslyLoggedInAt = Some(previouslyLoggedInAt)))
      user4.user shouldBe LoggedInUser(userId, Some(loggedInAt), Some(previouslyLoggedInAt), Some("token"))
    }

    "provide a LoggedInUser object containing the Government Gateway Token from the decryptedToken field" in {
      user.user.governmentGatewayToken shouldBe Some("token")
      user.copy(decryptedToken = Some("othertoken")).user.governmentGatewayToken shouldBe Some("othertoken")
      user.copy(decryptedToken = None).user.governmentGatewayToken shouldBe None
    }

    "provide the oid parsed from the user id (the value after the last slash)" in {
      user.oid shouldBe "1234567890"
      user.copy(userId = "/abc/123/456").oid shouldBe "456"
      user.copy(userId = "/abcde").oid shouldBe "abcde"
      user.copy(userId = "abcd").oid shouldBe "abcd"
    }
  }
}
