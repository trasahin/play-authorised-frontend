package uk.gov.hmrc.play.frontend.auth

import java.net.URI

import org.joda.time.{DateTimeZone, DateTime}
import uk.gov.hmrc.domain.{SaUtr, Nino}
import uk.gov.hmrc.play.auth.frontend.connectors.domain.{Authority, SaAccount, Accounts, PayeAccount}
import uk.gov.hmrc.play.test.UnitSpec

class AuthContextSpec extends UnitSpec {

  val userId = "/auth/oid/1234567890"
  val loggedInAt = Some(new DateTime(2015, 11, 22, 11, 33, 15, 234, DateTimeZone.UTC))
  val previouslyLoggedInAt = Some(new DateTime(2014, 8, 3, 9, 25, 44, 342, DateTimeZone.UTC))
  val governmentGatewayToken = Some("token")

  val accounts = Accounts(
    paye = Some(PayeAccount(link = "/paye/abc", nino = Nino("AB124512C"))),
    sa = Some(SaAccount(link = "/sa/www", utr = SaUtr("1231231233")))
  )

  val nameInSession = Some("Bob Client")

  val loggedInUser = LoggedInUser(
    userId = userId,
    loggedInAt = loggedInAt,
    previouslyLoggedInAt = previouslyLoggedInAt,
    governmentGatewayToken = governmentGatewayToken
  )

  val principal = Principal(
    name = nameInSession,
    accounts = accounts
  )

  val attorney = Attorney("Dave Agent", Link(new URI("http://stuff/blah"), "Back to dashboard"))

  val authenticationContext: AuthenticationContext = new AuthenticationContext(loggedInUser, principal, Some(attorney))

  val authority = Authority(
    uri = userId,
    accounts = accounts,
    loggedInAt = loggedInAt,
    previouslyLoggedInAt = previouslyLoggedInAt
  )

  val user = User(
    userId = userId,
    userAuthority = authority,
    nameFromGovernmentGateway = nameInSession,
    decryptedToken = governmentGatewayToken,
    actingAsAttorneyFor = None,
    attorney = Some(attorney)
  )

  "An AuthenticationContext" should {

    "inherit the unapply method from User" in {

      val u: User = authenticationContext

      u match {
        case User(xUserId, xAuthority, xNameFromGovernmentGateway, xDecryptedToken, xActingAsAttorneyFor, Some(xAttorney)) =>
          xUserId shouldBe userId
          xAuthority shouldBe authority
          xNameFromGovernmentGateway shouldBe nameInSession
          xDecryptedToken shouldBe governmentGatewayToken
          xActingAsAttorneyFor shouldBe None
          xAttorney shouldBe attorney
        case _ => fail("Expected initial unapply method to succeed")
      }
    }

    "be a User" in {
      authenticationContext shouldBe user
      user shouldBe authenticationContext
    }

    "have the same hashcode as the corresponding user" in {
      authenticationContext.hashCode shouldBe user.hashCode
    }
  }

  "The AuthContext apply method" should {

    "Construct a valid AuthContext (User) for the supplied user, principal and attorney" in {
      AuthContext(loggedInUser, principal, Some(attorney)) shouldBe authenticationContext
      authenticationContext shouldBe AuthContext(loggedInUser, principal, Some(attorney))

      AuthContext(loggedInUser, principal, Some(attorney)) shouldBe user
      user shouldBe AuthContext(loggedInUser, principal, Some(attorney))
    }

    "Construct a valid AuthContext (User) given an authority and values from the session (no delegation)" in {

      val authContext = AuthContext(authority, governmentGatewayToken, nameInSession)

      authContext.user shouldBe LoggedInUser(authority.uri, authority.loggedInAt, authority.previouslyLoggedInAt, governmentGatewayToken)
      authContext.principal shouldBe Principal(nameInSession, accounts)
      authContext.attorney shouldBe None

      // Check deprecated user fields:

      authContext.userId shouldBe authority.uri
      authContext.userAuthority shouldBe authority
      authContext.nameFromGovernmentGateway shouldBe nameInSession
      authContext.decryptedToken shouldBe governmentGatewayToken
      authContext.actingAsAttorneyFor shouldBe None
    }
  }

  "The AuthContext unapply method" should {

    "Extract the core AuthContext fields from an AuthenticationContext" in {

      authenticationContext match {
        case AuthContext(usr, prn, Some(att)) =>
          usr shouldBe loggedInUser
          prn shouldBe principal
          att shouldBe attorney
        case _ => fail("Expected a match")
      }
    }

    "cope with an empty Attorney object" in {
      AuthContext(loggedInUser, principal, None) match {
        case AuthContext(usr, prn, None) =>
          usr shouldBe loggedInUser
          prn shouldBe principal
        case _ => fail("Expected a match")
      }
    }

    "cope with a null AuthContext" in {
      null.asInstanceOf[AuthContext] match {
        case AuthContext(q, w, e) => fail("Expected no match")
        case other => other shouldBe null
      }
    }
  }
}
