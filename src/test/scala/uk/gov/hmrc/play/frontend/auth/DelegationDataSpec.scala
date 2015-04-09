package uk.gov.hmrc.play.frontend.auth

import uk.gov.hmrc.domain.CtUtr
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{Accounts, CtAccount}
import uk.gov.hmrc.play.test.UnitSpec

class DelegationDataSpec extends UnitSpec {

  "The attorney for a delegation data" should {

    val principalName = "principal"
    val accounts = Accounts(ct = Some(CtAccount("/link", CtUtr("4534231201"))))
    val link = Link("/blah", "Text")

    "combine the attorneyName and the link" in {
      DelegationData(principalName, "attorney", accounts, link).attorney shouldBe Attorney("attorney", link)
    }
  }
}
