package uk.gov.hmrc.play.frontend.auth

import org.joda.time.{DateTimeZone, DateTime}
import uk.gov.hmrc.domain.{Vrn, SaUtr, Nino}
import uk.gov.hmrc.play.auth.frontend.connectors.domain._
import uk.gov.hmrc.play.test.UnitSpec

class AuthContextSpec extends UnitSpec {

  object SessionData {

    val userId = "/auth/oid/1234567890"
    val name = Some("Gary 'Government' Gateway")
    val governmentGatewayToken = Some("token")
  }

  object AuthData {

    val loggedInAt = Some(new DateTime(2015, 11, 22, 11, 33, 15, 234, DateTimeZone.UTC))
    val previouslyLoggedInAt = Some(new DateTime(2014, 8, 3, 9, 25, 44, 342, DateTimeZone.UTC))


    val accounts = Accounts(
      paye = Some(PayeAccount(link = "/paye/abc", nino = Nino("AB124512C"))),
      sa = Some(SaAccount(link = "/sa/www", utr = SaUtr("1231231233")))
    )

    val authority = Authority(
      uri = SessionData.userId,
      accounts = accounts,
      loggedInAt = loggedInAt,
      previouslyLoggedInAt = previouslyLoggedInAt
    )
  }

  object DelegationServiceData {

    val principalName = "Bill Principal"
    val attorneyName = "Bob Attorney"
    val returnLink = Link("http://agentdashboard.com", "Return to your dashboard")
    val principalAccounts = Accounts(vat = Some(VatAccount("/vat/123123123", Vrn("123123123"))))

    val delegationData = DelegationData(principalName, attorneyName, principalAccounts, returnLink)
  }

  val expectedLoggedInUser = LoggedInUser(
    userId = SessionData.userId,
    loggedInAt = AuthData.loggedInAt,
    previouslyLoggedInAt = AuthData.previouslyLoggedInAt,
    governmentGatewayToken = SessionData.governmentGatewayToken
  )

  object ExpectationsWhenNotDelegating {

    val principal = Principal(
      name = SessionData.name,
      accounts = AuthData.accounts
    )

    val authenticationContext: AuthenticationContext = new AuthenticationContext(expectedLoggedInUser, principal, None)

    val user = User(
      userId = SessionData.userId,
      userAuthority = AuthData.authority,
      nameFromGovernmentGateway = SessionData.name,
      decryptedToken = SessionData.governmentGatewayToken,
      actingAsAttorneyFor = None,
      attorney = None
    )
  }

  object ExpectationsWhenDelegating {

    val principal = Principal(
      name = Some(DelegationServiceData.principalName),
      accounts = DelegationServiceData.principalAccounts
    )

    val attorney = Attorney(DelegationServiceData.attorneyName, DelegationServiceData.returnLink)

    val authenticationContext = new AuthenticationContext(expectedLoggedInUser, principal, Some(attorney))

    val user = User(
      userId = SessionData.userId,
      userAuthority = AuthData.authority.copy(accounts = DelegationServiceData.principalAccounts),
      nameFromGovernmentGateway = Some(DelegationServiceData.principalName),
      decryptedToken = SessionData.governmentGatewayToken,
      actingAsAttorneyFor = None,
      attorney = Some(attorney)
    )
  }

  "An AuthenticationContext" should {

    "correctly implement User when not delegating" in {

      val authenticationContext = new AuthenticationContext(
        expectedLoggedInUser,
        ExpectationsWhenNotDelegating.principal,
        None
      )

      val expectedUser = ExpectationsWhenNotDelegating.user

      authenticationContext shouldBe expectedUser
      expectedUser shouldBe authenticationContext

      authenticationContext.hashCode shouldBe expectedUser.hashCode
    }

    "correctly implement User when delegating" in {

      val authenticationContext = new AuthenticationContext(
        expectedLoggedInUser,
        ExpectationsWhenDelegating.principal,
        Some(ExpectationsWhenDelegating.attorney))

      val expectedUser = ExpectationsWhenDelegating.user

      authenticationContext shouldBe expectedUser
      expectedUser shouldBe authenticationContext

      authenticationContext.hashCode shouldBe expectedUser.hashCode
    }
  }

  "The AuthContext apply method" should {

    "Construct a valid AuthContext (User) for the supplied user, principal and attorney when delegating" in {

      import ExpectationsWhenDelegating._

      val authContext = AuthContext(expectedLoggedInUser, principal, Some(attorney))

      authContext shouldBe authenticationContext
      authContext shouldBe user
    }

    "Construct a valid AuthContext (User) for the supplied user and principal when not delegating" in {

      import ExpectationsWhenNotDelegating._

      val authContext = AuthContext(expectedLoggedInUser, principal, None)

      authContext shouldBe authenticationContext
      authContext shouldBe user
    }

    "Construct a valid AuthContext (User) for the supplied authority, session values and delegation data when delegating" in {

      val authContext = AuthContext(AuthData.authority, SessionData.governmentGatewayToken, SessionData.name, Some(DelegationServiceData.delegationData))

      authContext shouldBe ExpectationsWhenDelegating.authenticationContext
      authContext shouldBe ExpectationsWhenDelegating.user
    }

    "Construct a valid AuthContext (User) for the supplied authority and session values and when not delegating" in {

      val authContext = AuthContext(AuthData.authority, SessionData.governmentGatewayToken, SessionData.name, None)

      authContext shouldBe ExpectationsWhenNotDelegating.authenticationContext
      authContext shouldBe ExpectationsWhenNotDelegating.user
    }
  }

  "The AuthContext unapply method" should {

    "Extract the core AuthContext fields from an AuthenticationContext when delegating" in {

      import ExpectationsWhenDelegating._

      authenticationContext match {
        case AuthContext(usr, prn, Some(att)) =>
          usr shouldBe expectedLoggedInUser
          prn shouldBe principal
          att shouldBe attorney
        case _ => fail("Expected a match")
      }
    }

    "Extract the core AuthContext fields from a User when delegating" in {

      import ExpectationsWhenDelegating._

      user match {
        case AuthContext(usr, prn, Some(att)) =>
          usr shouldBe expectedLoggedInUser
          prn shouldBe principal
          att shouldBe attorney
        case _ => fail("Expected a match")
      }
    }

    "Extract the core AuthContext fields from an AuthenticationContext when not delegating" in {

      import ExpectationsWhenNotDelegating._

      authenticationContext match {
        case AuthContext(usr, prn, att) =>
          usr shouldBe expectedLoggedInUser
          prn shouldBe principal
          att shouldBe None
        case _ => fail("Expected a match")
      }
    }

    "Extract the core AuthContext fields from a User when not delegating" in {

      import ExpectationsWhenNotDelegating._

      user match {
        case AuthContext(usr, prn, att) =>
          usr shouldBe expectedLoggedInUser
          prn shouldBe principal
          att shouldBe None
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

  "The display name of an AuthContext" should {

    "be the principal name when not delegating" in {

      val authContext = new AuthContext {
        override def user: LoggedInUser = ???

        override def attorney: Option[Attorney] = None

        override def principal: Principal = Principal(name = Some("Alan Principal"), accounts = Accounts())
      }

      authContext.displayName shouldBe Some("Alan Principal")
    }

    "be None if not delegating and the principal name is None" in {

      val authContext = new AuthContext {
        override def user: LoggedInUser = ???

        override def attorney: Option[Attorney] = None

        override def principal: Principal = Principal(name = None, accounts = Accounts())
      }

      authContext.displayName shouldBe None
    }

    "be the attorneyName if delegating and the principal name is None" in {

      val authContext = new AuthContext {
        override def user: LoggedInUser = ???

        override def attorney: Option[Attorney] = Some(Attorney(name = "Alice Accountant", returnLink = Link("aaa", "bbb")))

        override def principal: Principal = Principal(name = None, accounts = Accounts())
      }

      authContext.displayName shouldBe Some("Alice Accountant")
    }

    "be 'attorneyName on behalf of principalName' if delegating and both are provided" in {

      val authContext = new AuthContext {
        override def user: LoggedInUser = ???

        override def attorney: Option[Attorney] = Some(Attorney(name = "Alice Accountant", returnLink = Link("aaa", "bbb")))

        override def principal: Principal = Principal(name = Some("Alan Principal"), accounts = Accounts())
      }

      authContext.displayName shouldBe Some("Alice Accountant on behalf of Alan Principal")
    }
  }
}
