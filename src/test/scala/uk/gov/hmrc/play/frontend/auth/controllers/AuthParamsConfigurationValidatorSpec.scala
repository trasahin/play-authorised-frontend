/*
 * Copyright 2015 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.play.frontend.auth.controllers

import com.typesafe.config.ConfigFactory
import play.api.Configuration
import uk.gov.hmrc.play.test.UnitSpec

class AuthParamsConfigurationValidatorSpec extends UnitSpec {

  val validator = AuthParamsConfigurationValidator

  "Validation of agent controller config for agent role" should {

    "not fail when account = agent and agentRole = admin" in {
      val config = ConfigFactory.parseString("""controllers.uk.gov.SomeController.authParams = {
                                               |  account = agent
                                               |  agentRole = admin
                                               |}""".stripMargin)
      validator.validate(Configuration(config))
    }

    "not fail when account = agent and agentRole = assistant" in {
      val config = ConfigFactory.parseString("""controllers.uk.gov.SomeController.authParams = {
                                               |  account = agent
                                               |  agentRole = assistant
                                               |}""".stripMargin)
      validator.validate(Configuration(config))
    }

    "fail when account = agent and agentRole = invalidrole" in {
      val config = ConfigFactory.parseString("""controllers.uk.gov.SomeController.authParams = {
                                               |  account = agent
                                               |  agentRole = invalidrole
                                               |}""".stripMargin)
      an[Exception] should be thrownBy validator.validate(Configuration(config))
    }

    "fail when account = agent and agentRole is not specified" in {
      val config = ConfigFactory.parseString("""controllers.uk.gov.SomeController.authParams = {
                                               |  account = agent
                                               |}""".stripMargin)
      an[Exception] should be thrownBy validator.validate(Configuration(config))
    }

    "fail when account != agent and agentRole is specified" in {
      val config = ConfigFactory.parseString("""controllers.uk.gov.SomeController.authParams = {
                                               |  account = paye
                                               |  agentRole = admin
                                               |}""".stripMargin)
      an[Exception] should be thrownBy validator.validate(Configuration(config))
    }
  }

  "Validation of agent controller config for authorisation rule" should {

    "not fail when account != agent and delegatedAuthRule is specified and contains only letters, hyphens and underscores" in {
      val config = ConfigFactory.parseString( """controllers.uk.gov.SomeController.authParams = {
                                                |  account = vat
                                                |  delegatedAuthRule = lp-paye_agent
                                                |}""".stripMargin)
      validator.validate(Configuration(config))
    }

    "not fail when account != agent and delegatedAuthRule contains only letters and hyphens" in {
      val config = ConfigFactory.parseString( """controllers.uk.gov.SomeController.authParams = {
                                                |  account = vat
                                                |  delegatedAuthRule = lp-paye
                                                |}""".stripMargin)
      validator.validate(Configuration(config))
    }

    "fail when account != agent and delegatedAuthRule contains characters that are not letters, hyphens or underscores" in {
      val config = ConfigFactory.parseString( """controllers.uk.gov.SomeController.authParams = {
                                                |  account = vat
                                                |  delegatedAuthRule = w%c#y
                                                |}""".stripMargin)
      an[Exception] should be thrownBy validator.validate(Configuration(config))
    }

    "fail when account = agent and delegatedAuthRule is specified" in {
      val config = ConfigFactory.parseString( """controllers.uk.gov.SomeController.authParams = {
                                                |  account = agent
                                                |  delegatedAuthRule = lp-paye
                                                |}""".stripMargin)
      an[Exception] should be thrownBy validator.validate(Configuration(config))
    }
  }

  "Validation of agent controller config when account is not declared" should {

    "not fail when delegatedAuthRule is specified" in {
      val config = ConfigFactory.parseString( """controllers.uk.gov.SomeController.authParams = {
                                                |  delegatedAuthRule = lp-paye_agent
                                                |}""".stripMargin)
      validator.validate(Configuration(config))
    }

    "fail when agentRole is is declared" in {
      val config = ConfigFactory.parseString( """controllers.uk.gov.SomeController.authParams = {
                                                |  agentRole = admin
                                                |}""".stripMargin)
      an[Exception] should be thrownBy validator.validate(Configuration(config))
    }
  }

  "Validation of multiple controllers" should {

    "correctly validate when there are multiple controllers specified" in {
      val config = ConfigFactory.parseString("""controllers {
                                               | uk.gov.SomeController.authParams = {
                                               |  account = agent
                                               |  agentRole = admin
                                               | }
                                               | uk.gov.AnotherController.authParams = {
                                               |  account = agent
                                               |  agentRole = assistant
                                               | }
                                               |}""".stripMargin)
      validator.validate(Configuration(config))
    }

    "correctly fail when there are multiple controllers specified and only one is invalid" in {
      val config = ConfigFactory.parseString("""controllers {
                                               | uk.gov.SomeController.authParams = {
                                               |  account = agent
                                               |  agentRole = admin
                                               | }
                                               | uk.gov.SecondController.authParams = {
                                               |  account = agent
                                               |  agentRole = oops
                                               | }
                                               | uk.gov.ThirdController.authParams = {
                                               |  account = agent
                                               |  agentRole = assistant
                                               | }
                                               |}""".stripMargin)
      an[Exception] should be thrownBy validator.validate(Configuration(config))
    }
  }
  "Validation of controller config when authparams.mode is declared" should {

    "not fail when authparams.mode is specified correctly" in {
      val config = ConfigFactory.parseString( """controllers.uk.gov.SomeController.authParams = {
                                                |  mode = identity
                                                |}""".stripMargin)
      validator.validate(Configuration(config))
    }

    "not fail when authparams.mode is specified correctly 2" in {
      val config = ConfigFactory.parseString( """controllers.uk.gov.SomeController.authParams = {
                                                |  mode = passcode
                                                |}""".stripMargin)
      validator.validate(Configuration(config))
    }

    "fail when authparams.mode is not specified correctly" in {
      val config = ConfigFactory.parseString( """controllers.uk.gov.SomeController.authParams = {
                                                |  mode = passscode
                                                |}""".stripMargin)
      an[Exception] should be thrownBy validator.validate(Configuration(config))
    }
  }


  "Validation of controller config when authparams.anonymous.pattern is declared" should {

    "not fail when authparams.pattern is specified correctly" in {
      val config = ConfigFactory.parseString( """controllers.uk.gov.SomeController.authParams = {
                                                |  pattern = "[A-Z]{3,6}(regime)/(\\d+)?.*"
                                                |}""".stripMargin)
      validator.validate(Configuration(config))
    }

    "not fail when authparams.anonymous.pattern is specified correctly" in {
      val config = ConfigFactory.parseString( """controllers.uk.gov.SomeController.authParams = {
                                                |  anonymous.pattern = "[A-Z]{3,6}(regime)/?.*"
                                                |}""".stripMargin)
      validator.validate(Configuration(config))
    }

    "fail when authparams.pattern has no capturing group is specified" in {
      val config = ConfigFactory.parseString( """controllers.uk.gov.SomeController.authParams = {
                                                |  pattern = "([A-Z]{3,6})/?.*"
                                                |}""".stripMargin)
      an[Exception] should be thrownBy validator.validate(Configuration(config))
    }

    "fail when authparams.anonymous.pattern has no capturing group is specified" in {
      val config = ConfigFactory.parseString( """controllers.uk.gov.SomeController.authParams = {
                                                |  anonymous.pattern = "[A-Z]{3,6}/?.*"
                                                |}""".stripMargin)
      an[Exception] should be thrownBy validator.validate(Configuration(config))
    }
  }

  "Validation of levelOfAssurance in controllers" should {
    val invalidDefaultLOA = ConfigFactory.parseString("""
                                                        |controllers {
                                                        |  defaultLevelOfAssurance = "3"
                                                        |}
                                                      """.stripMargin)

    val validDefaultLOA = ConfigFactory.parseString("""
                                                      |controllers {
                                                      |  defaultLevelOfAssurance = "1.5"
                                                      |}
                                                    """.stripMargin)

    val invalidControllerConfig = ConfigFactory.parseString("""
                                                              |controllers { 
                                                              | uk.gov.hmrc.play.controllers.AbsentDelegateAuthController {
                                                              |    needsAuth = true
                                                              |    authParams = {
                                                              |      pattern = "/(\\w*)/(\\d)/.*"
                                                              |      levelOfAssurance = "7"
                                                              |    }
                                                              | }
                                                              |}""".stripMargin)

    val validControllerConfig = ConfigFactory.parseString("""
                                                            |controllers { 
                                                            | uk.gov.hmrc.play.controllers.AbsentDelegateAuthController {
                                                            |    needsAuth = true
                                                            |    authParams = {
                                                            |      pattern = "/(\\w*)/(\\d)/.*"
                                                            |      levelOfAssurance = "1"
                                                            |    }
                                                            | }
                                                            |}""".stripMargin)

    "throw Exception  when defaultLevelOfAssurance is invalid at global level" in {
      val e = the [Exception] thrownBy validator.validate(Configuration(invalidDefaultLOA))
      e.getMessage should include ("Invalid config:controllers.defaultLevelOfAssurance")

    }
    "throw Exception  when levelOfAssurance is invalid at controller level" in {
      val e= the [Exception] thrownBy validator.validate(Configuration(invalidControllerConfig))
      e.getMessage should include ("Invalid config uk.gov.hmrc.play.controllers.AbsentDelegateAuthController.authParams.levelOfAssurance")
    }

    "not fail when defaultLevelOfAssurance is valid at global level" in {
      validator.validate(Configuration(validDefaultLOA))
    }

    "not fail when levelOfAssurance is valid at controller level" in {
      validator.validate(Configuration(validControllerConfig))
    }

  }
}
