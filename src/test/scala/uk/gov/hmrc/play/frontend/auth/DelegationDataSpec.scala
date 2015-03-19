package uk.gov.hmrc.play.frontend.auth

import java.net.URI

import uk.gov.hmrc.domain.CtUtr
import uk.gov.hmrc.play.auth.frontend.connectors.domain.{CtAccount, Accounts}
import uk.gov.hmrc.play.test.UnitSpec

class DelegationDataSpec extends UnitSpec {

  "The attorney for a delegation data" should {

    val principalName = Some("principal")
    val accounts = Accounts(ct = Some(CtAccount("/link", CtUtr("4534231201"))))
    val link = Link(new URI("/blah"), "Text")

    "combine the attorneyName and link if the attorneyName is provided" in {
      DelegationData(principalName, Some("attorney"), accounts, link).attorney shouldBe Attorney(Some("attorney"), link)
    }

    "combine the attorneyName and link if the attorneyName is not provided" in {
      DelegationData(principalName, None, accounts, link).attorney shouldBe Attorney(None, link)
    }
  }
}
