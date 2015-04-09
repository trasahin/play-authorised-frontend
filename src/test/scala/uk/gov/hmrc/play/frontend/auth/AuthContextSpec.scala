package uk.gov.hmrc.play.frontend.auth

import org.joda.time.{DateTime, DateTimeZone}
import uk.gov.hmrc.domain.{Nino, SaUtr, Vrn}
import uk.gov.hmrc.play.frontend.auth.connectors.domain._
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

    val expectedAuthContext = AuthContext(expectedLoggedInUser, principal, None)
  }

  object ExpectationsWhenDelegating {

    val principal = Principal(
      name = Some(DelegationServiceData.principalName),
      accounts = DelegationServiceData.principalAccounts
    )

    val attorney = Attorney(DelegationServiceData.attorneyName, DelegationServiceData.returnLink)

    val expectedAuthContext = AuthContext(expectedLoggedInUser, principal, Some(attorney))
  }

  "The AuthContext apply method" should {

    "Construct a valid AuthContext for the supplied authority, session values and delegation data when delegating" in {

      val authContext = AuthContext(AuthData.authority, SessionData.governmentGatewayToken, SessionData.name, Some(DelegationServiceData.delegationData))

      authContext shouldBe ExpectationsWhenDelegating.expectedAuthContext
    }

    "Construct a valid AuthContext for the supplied authority and session values and when not delegating" in {

      val authContext = AuthContext(AuthData.authority, SessionData.governmentGatewayToken, SessionData.name, None)

      authContext shouldBe ExpectationsWhenNotDelegating.expectedAuthContext
    }
  }

  "The isDelegating flag" should {

    val loggedInUser = LoggedInUser("uid", None, None, None)
    val principal = Principal(Some("Bob P"), Accounts())
    
    "be true if the attorney is defined" in {
      AuthContext(loggedInUser, principal, Some(Attorney("Dave A", Link("A", "A")))).isDelegating shouldBe true
    }

    "be false if the attorney is None" in {
      AuthContext(loggedInUser, principal, None).isDelegating shouldBe false
    }
  }
}
