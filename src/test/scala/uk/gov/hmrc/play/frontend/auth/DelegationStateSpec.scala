package uk.gov.hmrc.play.frontend.auth

import uk.gov.hmrc.play.test.UnitSpec

class DelegationStateSpec extends UnitSpec {

  "The toString method" should {

    "return On for DelegationOn" in {
      DelegationOn.toString shouldBe "On"
    }

    "return Off for DelegationOff" in {
      DelegationOff.toString shouldBe "Off"
    }
  }

  "The apply method" should {

    "return DelegationOn if the value is On" in {
      DelegationState(Some("On")) shouldBe DelegationOn
    }

    "return DelegationOff for any other string value" in {
      DelegationState(Some("Off")) shouldBe DelegationOff
      DelegationState(Some("on")) shouldBe DelegationOff
      DelegationState(Some(" On ")) shouldBe DelegationOff
      DelegationState(Some("sdfdsfdsf")) shouldBe DelegationOff
      DelegationState(Some("")) shouldBe DelegationOff
      DelegationState(Some("   ")) shouldBe DelegationOff
    }

    "return DelegationOff if None is passed in" in {
      DelegationState(None) shouldBe DelegationOff
    }
  }
}
