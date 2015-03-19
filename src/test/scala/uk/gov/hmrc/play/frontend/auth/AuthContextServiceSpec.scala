package uk.gov.hmrc.play.frontend.auth

import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.domain.{CtUtr, Nino, SaUtr}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.auth.frontend.connectors.AuthConnector
import uk.gov.hmrc.play.auth.frontend.connectors.domain._
import uk.gov.hmrc.play.frontend.auth.connectors.DelegationConnector
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class AuthContextServiceSpec extends UnitSpec with MockitoSugar {

  class TestSetup(val delegationSessionFlag: DelegationState, val provideDelegationConnector: Boolean, val returnDataFromDelegationService: Boolean = true)

  case object DelegationOffAndNoConnector extends TestSetup(DelegationOff, provideDelegationConnector = false)
  case object DelegationOffButDataAvailable extends TestSetup(DelegationOff, provideDelegationConnector = true)
  
  case object DelegationOnButNoConnector extends TestSetup(DelegationOn, provideDelegationConnector = false)
  case object DelegationOnButNoData extends TestSetup(DelegationOn, provideDelegationConnector = true, returnDataFromDelegationService = false)

  case object DelegationOnAndDataAvailable extends TestSetup(DelegationOn, provideDelegationConnector = true)

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  "When the delegation session flag is 'Off', and no delegation connector is available, the currentAuthContext method" should {
    behaveAsExpectedWithoutDelegation(DelegationOffAndNoConnector)
    returnNoneIfTheAuthorityIsMissingOrInvalid(DelegationOffAndNoConnector)
  }
  
  "When the delegation session flag is 'Off', even if the delegation data is available, the currentAuthContext method" should {
    behaveAsExpectedWithoutDelegation(DelegationOffButDataAvailable)
    returnNoneIfTheAuthorityIsMissingOrInvalid(DelegationOffButDataAvailable)
  }
  
  "When the delegation session flag is 'On', but no delegation connector is available, the currentAuthContext method" should {
    behaveAsExpectedWithoutDelegation(DelegationOnButNoConnector)
    returnNoneIfTheAuthorityIsMissingOrInvalid(DelegationOnButNoConnector)
  }

  "When the delegation session flag is 'On', but no delegation data is available, the currentAuthContext method" should {
    behaveAsExpectedWithoutDelegation(DelegationOnButNoData)
    returnNoneIfTheAuthorityIsMissingOrInvalid(DelegationOnButNoData)
  }

  "When the delegation session flag is 'On', and delegation data is available, the currentAuthContext method" should {

    "combine the session data with the current authority and delegation data to create the AuthContext" in new TestCase(DelegationOnAndDataAvailable) {

      when(mockAuthConnector.currentAuthority).thenReturn(Future.successful(Some(userAtKeyboard.authority)))

      val sessionData = UserSessionData(
        userId = Some(session.userId),
        governmentGatewayToken = Some(session.governmentGatewayToken),
        name = Some(session.name),
        delegationState = DelegationOn
      )

      val authContext = await(service.currentAuthContext(sessionData))

      authContext shouldBe Some(AuthContext(
        user = LoggedInUser(session.userId, userAtKeyboard.loggedInAt, userAtKeyboard.previouslyLoggedInAt, Some(session.governmentGatewayToken)),
        principal = Principal(Some(delegationData.principalName), delegationData.accounts),
        attorney = Some(Attorney(delegationData.attorneyName, delegationData.returnLink))
      ))
    }

    "create the correct AuthContext if the governmentGatewayToken passed in is None" in new TestCase(DelegationOnAndDataAvailable) {

      when(mockAuthConnector.currentAuthority).thenReturn(Future.successful(Some(userAtKeyboard.authority)))

      val sessionData = UserSessionData(
        userId = Some(session.userId),
        governmentGatewayToken = None,
        name = Some(session.name),
        delegationState = DelegationOn
      )

      val authContext = await(service.currentAuthContext(sessionData))

      authContext shouldBe Some(AuthContext(
        user = LoggedInUser(session.userId, userAtKeyboard.loggedInAt, userAtKeyboard.previouslyLoggedInAt, None),
        principal = Principal(Some(delegationData.principalName), delegationData.accounts),
        attorney = Some(Attorney(delegationData.attorneyName, delegationData.returnLink))
      ))
    }

    returnNoneIfTheAuthorityIsMissingOrInvalid(DelegationOnAndDataAvailable)
  }

  private def behaveAsExpectedWithoutDelegation(testSetup: TestSetup) = {

    "combine the session data with the current authority to create the AuthContext" in new TestCase(testSetup) {

      when(mockAuthConnector.currentAuthority).thenReturn(Future.successful(Some(userAtKeyboard.authority)))

      val sessionData = UserSessionData(
        userId = Some(session.userId),
        governmentGatewayToken = Some(session.governmentGatewayToken),
        name = Some(session.name),
        delegationState = testSetup.delegationSessionFlag
      )

      val authContext = await(service.currentAuthContext(sessionData))

      authContext shouldBe Some(AuthContext(
        user = LoggedInUser(session.userId, userAtKeyboard.loggedInAt, userAtKeyboard.previouslyLoggedInAt, Some(session.governmentGatewayToken)),
        principal = Principal(Some(session.name), userAtKeyboard.accounts),
        attorney = None
      ))
    }

    "create the correct AuthContext if the governmentGatewayToken passed in is None" in new TestCase(testSetup) {

      when(mockAuthConnector.currentAuthority).thenReturn(Future.successful(Some(userAtKeyboard.authority)))

      val sessionData = UserSessionData(
        userId = Some(session.userId),
        governmentGatewayToken = None,
        name = Some(session.name),
        delegationState = testSetup.delegationSessionFlag
      )

      val authContext = await(service.currentAuthContext(sessionData))

      authContext shouldBe Some(AuthContext(
        user = LoggedInUser(session.userId, userAtKeyboard.loggedInAt, userAtKeyboard.previouslyLoggedInAt, None),
        principal = Principal(Some(session.name), userAtKeyboard.accounts),
        attorney = None
      ))
    }

    "create the correct AuthContext if the nameFromSession passed in is None" in new TestCase(testSetup) {

      when(mockAuthConnector.currentAuthority).thenReturn(Future.successful(Some(userAtKeyboard.authority)))

      val sessionData = UserSessionData(
        userId = Some(session.userId),
        governmentGatewayToken = Some(session.governmentGatewayToken),
        name = None,
        delegationState = testSetup.delegationSessionFlag
      )

      val authContext = await(service.currentAuthContext(sessionData))

      authContext shouldBe Some(AuthContext(
        user = LoggedInUser(session.userId, userAtKeyboard.loggedInAt, userAtKeyboard.previouslyLoggedInAt, Some(session.governmentGatewayToken)),
        principal = Principal(None, userAtKeyboard.accounts),
        attorney = None
      ))
    }
  }

  def returnNoneIfTheAuthorityIsMissingOrInvalid(testSetup: TestSetup) = {

    "return None if the userId passed in does not match the uri of the Authority" in new TestCase(testSetup) {

      val oid = "somethingelse"

      when(mockAuthConnector.currentAuthority).thenReturn(Future.successful(Some(userAtKeyboard.authority)))
      when(mockDelegationConnector.get(oid)).thenReturn(Future.successful(Some(delegationData)))

      val sessionData = UserSessionData(
        userId = Some(s"/auth/oid/$oid"),
        governmentGatewayToken = Some(session.governmentGatewayToken),
        name = None,
        delegationState = testSetup.delegationSessionFlag
      )

      await(service.currentAuthContext(sessionData)) shouldBe None
    }

    "return None if there is no current Authority" in new TestCase(testSetup) {

      when(mockAuthConnector.currentAuthority).thenReturn(Future.successful(None))
      when(mockDelegationConnector.get(session.oid)).thenReturn(Future.successful(Some(delegationData)))

      val sessionData = UserSessionData(
        userId = Some(session.userId),
        governmentGatewayToken = Some(session.governmentGatewayToken),
        name = None,
        delegationState = testSetup.delegationSessionFlag
      )

      await(service.currentAuthContext(sessionData)) shouldBe None
    }

  }
  
  abstract class TestCase(testSetup: TestSetup) {

    val mockAuthConnector: AuthConnector = mock[AuthConnector]
    val mockDelegationConnector: DelegationConnector = mock[DelegationConnector]

    val delegationData = DelegationData(
      principalName = "Bill Principal",
      attorneyName = "Brian Agent",
      accounts = Accounts(ct = Some(CtAccount("/some/path", CtUtr("1234554321")))),
      returnLink = Link("/some/url", "Back to your dashboard")
    )

    if (testSetup.returnDataFromDelegationService) {
      when(mockDelegationConnector.get(session.oid)).thenReturn(Future.successful(Some(delegationData)))
    } else {
      when(mockDelegationConnector.get(session.oid)).thenReturn(Future.successful(None))
    }

    val service = new AuthContextService {
      override protected val authConnector = mockAuthConnector
      override protected val delegationConnector = if (testSetup.provideDelegationConnector) Some(mockDelegationConnector) else None
    }

    object session {
      val userId = "/auth/oid/1234567890"
      val oid = "1234567890"
      val governmentGatewayToken = "token"
      val name = "Dave Agent"
    }

    object userAtKeyboard {
      
      val loggedInAt = Some(new DateTime(2015, 11, 22, 11, 33, 15, 234, DateTimeZone.UTC))
      
      val previouslyLoggedInAt = Some(new DateTime(2014, 8, 3, 9, 25, 44, 342, DateTimeZone.UTC))
      
      val accounts = Accounts(
        paye = Some(PayeAccount(link = "/paye/abc", nino = Nino("AB124512C"))),
        sa = Some(SaAccount(link = "/sa/www", utr = SaUtr("1231231233")))
      )
      
      val authority = Authority(
        uri = session.userId,
        accounts = accounts,
        loggedInAt = loggedInAt,
        previouslyLoggedInAt = previouslyLoggedInAt
      )
    }
  }
}
