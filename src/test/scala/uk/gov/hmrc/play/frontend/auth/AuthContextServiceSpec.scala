package uk.gov.hmrc.play.frontend.auth

import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.auth.frontend.connectors.AuthConnector
import uk.gov.hmrc.play.auth.frontend.connectors.domain.{Accounts, Authority, PayeAccount, SaAccount}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class AuthContextServiceSpec extends UnitSpec with MockitoSugar {

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  "The currentAuthContext method, when not delegating" should {

    "combine the passed in userId, governmentGatewayToken and session name and combine with the current authority to create the AuthContext" in new TestCase {

      when(mockAuthConnector.currentAuthority).thenReturn(Future.successful(Some(authority)))

      val authContext = await(service.currentAuthContext(userId, Some(governmentGatewayToken), Some(nameFromSession)))

      authContext shouldBe Some(AuthContext(
        user = LoggedInUser(userId, loggedInAt, previouslyLoggedInAt, Some(governmentGatewayToken)),
        principal = Principal(Some(nameFromSession), accounts),
        attorney = None
      ))
    }

    "create the correct AuthContext if the governmentGatewayToken passed in is None" in new TestCase {

      when(mockAuthConnector.currentAuthority).thenReturn(Future.successful(Some(authority)))

      val authContext = await(service.currentAuthContext(userId, None, Some(nameFromSession)))

      authContext shouldBe Some(AuthContext(
        user = LoggedInUser(userId, loggedInAt, previouslyLoggedInAt, None),
        principal = Principal(Some(nameFromSession), accounts),
        attorney = None
      ))
    }

    "create the correct AuthContext if the nameFromSession passed in is None" in new TestCase {

      when(mockAuthConnector.currentAuthority).thenReturn(Future.successful(Some(authority)))

      val authContext = await(service.currentAuthContext(userId, Some(governmentGatewayToken), None))

      authContext shouldBe Some(AuthContext(
        user = LoggedInUser(userId, loggedInAt, previouslyLoggedInAt, Some(governmentGatewayToken)),
        principal = Principal(None, accounts),
        attorney = None
      ))
    }

    "return None if the userId passed in does not match the uri of the Authority" in new TestCase {

      when(mockAuthConnector.currentAuthority).thenReturn(Future.successful(Some(authority)))

      await(service.currentAuthContext("/some/other/id", Some(governmentGatewayToken), Some(nameFromSession))) shouldBe None
    }
    
    "return None if there is no current Authority" in new TestCase {

      when(mockAuthConnector.currentAuthority).thenReturn(Future.successful(None))

      await(service.currentAuthContext(userId, Some(governmentGatewayToken), Some(nameFromSession))) shouldBe None
    }
  }

  trait TestCase {

    val mockAuthConnector: AuthConnector = mock[AuthConnector]

    val service = new AuthContextService {
      override protected val authConnector = mockAuthConnector
    }

    val userId = "/auth/oid/1234567890"
    val loggedInAt = Some(new DateTime(2015, 11, 22, 11, 33, 15, 234, DateTimeZone.UTC))
    val previouslyLoggedInAt = Some(new DateTime(2014, 8, 3, 9, 25, 44, 342, DateTimeZone.UTC))
    val governmentGatewayToken = "token"

    val nameFromSession = "Dave Agent"

    val accounts = Accounts(
      paye = Some(PayeAccount(link = "/paye/abc", nino = Nino("AB124512C"))),
      sa = Some(SaAccount(link = "/sa/www", utr = SaUtr("1231231233")))
    )

    val authority = Authority(
      uri = userId,
      accounts = accounts,
      loggedInAt = loggedInAt,
      previouslyLoggedInAt = previouslyLoggedInAt
    )

//    val expectedPrincipalName = Some("Bob Client")
//
//    val expectedLoggedInUser = LoggedInUser(
//      userId = userId,
//      loggedInAt = loggedInAt,
//      previouslyLoggedInAt = previouslyLoggedInAt,
//      governmentGatewayToken = Some(governmentGatewayToken)
//    )
//
//    val expectedPrincipal = Principal(
//      name = expectedPrincipalName,
//      accounts = accounts
//    )
//
//    val attorney = Attorney("Dave Agent", Link(new URI("http://stuff/blah"), "Back to dashboard"))
//
//    val authenticationContext: AuthenticationContext = new AuthenticationContext(loggedInUser, principal, Some(attorney))
//
//    val user = User(
//      userId = userId,
//      userAuthority = authority,
//      nameFromGovernmentGateway = principalName,
//      decryptedToken = governmentGatewayToken,
//      actingAsAttorneyFor = None,
//      attorney = Some(attorney)
//    )
  }
}
