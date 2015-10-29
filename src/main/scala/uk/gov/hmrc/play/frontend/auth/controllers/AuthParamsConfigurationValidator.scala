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

import play.api.Configuration
import uk.gov.hmrc.play.frontend.auth.connectors.domain.LevelOfAssurance._

import scala.util.matching.Regex

object AuthParamsConfigurationValidator {

  private val controllerRegex = """([\w\.]+\.authParams).*""".r

  private val validAccountName = matches("""^([A-Za-z]+)$""".r)_

  private val validAuthorisationRule = matches("""^([A-Za-z-_]+)$""".r)_

  def validate(config: Configuration) {
    config.getConfig("controllers") foreach validateControllers
  }

  private val validAssuranceLevels = Set(LOA_1.toString, LOA_1_5.toString, LOA_2.toString)
  private def validateControllers(controllersConfig: Configuration) {
    validateDefaultLevelOfAssurance(controllersConfig)
    val validate = validateAuthParams(controllersConfig) _
    authParamKeysFor(controllersConfig.keys).foreach(validate)
  }

  private def authParamKeysFor(names: Set[String]): Set[String] = {
    names flatMap {
      case controllerRegex(authParamsKey) => Some(authParamsKey)
      case _ => None
    }
  }

  private def validateAuthParams(controllerConfig: Configuration)(authParamKey: String) {
    val accountOption = controllerConfig.getString(s"$authParamKey.account")
    val agentRoleOption = controllerConfig.getString(s"$authParamKey.agentRole")
    val delegatedAuthRuleOption = controllerConfig.getString(s"$authParamKey.delegatedAuthRule")

    accountOption match {
      case Some(account) if account == "agent" => validateAgentAccount(agentRoleOption, delegatedAuthRuleOption, authParamKey)
      case Some(account) => validateNonAgentAccount(account, agentRoleOption, delegatedAuthRuleOption, authParamKey)
      case None => validateNoAccountDeclared(agentRoleOption, delegatedAuthRuleOption, authParamKey)
    }

    val authMode = controllerConfig.getString(s"$authParamKey.mode")
    require(authMode.forall(validAuthMode), s"Invalid config:$authParamKey.mode, may only be 'identity' or 'passcode'")

    val authUrlPattern = controllerConfig.getString(s"$authParamKey.pattern")
    require(authUrlPattern.forall(s => countCaptureGroups(s.r) == 2), s"Invalid config:$authParamKey.pattern, has the wrong number of capture groups")

    val anonAuthUrlPattern = controllerConfig.getString(s"$authParamKey.anonymous.pattern")
    require(anonAuthUrlPattern.forall(s => countCaptureGroups(s.r) == 1), s"Invalid config:$authParamKey.anonymous.pattern, has the wrong number of capture groups")
    controllerConfig.getConfig(authParamKey) foreach {validateLevelOfAssurance(_,authParamKey)}
  }

  private def validateAgentAccount(agentRoleOption: Option[String], delegatedAuthRuleOption: Option[String], authParamKey: String) {
    require(agentRoleOption.nonEmpty, s"Invalid config:$authParamKey, agentRole must be specified for agent account")
    require(delegatedAuthRuleOption.isEmpty, s"Invalid config:$authParamKey, delegatedAuthRule cannot be specified for agent account")
    require(agentRoleOption.forall(validAgentRole), s"Invalid config:$authParamKey, agentRole ${agentRoleOption.get} may only be 'admin' or 'assistant'")
  }

  private def validateNonAgentAccount(account: String, agentRoleOption: Option[String], delegatedAuthRuleOption: Option[String], authParamKey: String) {
    require(validAccountName(account), s"Invalid config:$authParamKey, account $account may only contain may letters")
    require(agentRoleOption.isEmpty, s"Invalid config:$authParamKey, agentRole may only be defined for the agent account")
    require(delegatedAuthRuleOption.forall(validAuthorisationRule), s"Invalid config:$authParamKey, delegatedAuthRule ${delegatedAuthRuleOption.get} may only contain letters, hyphens and underscores")
  }

  private def validateNoAccountDeclared(agentRoleOption: Option[String], delegatedAuthRuleOption: Option[String], authParamKey: String) {
    require (agentRoleOption.isEmpty, s"Invalid config:$authParamKey, agentRole may only be defined when the account is explicitly declared and is set to agent")
    require (delegatedAuthRuleOption.forall(validAuthorisationRule), s"Invalid config:$authParamKey, delegatedAuthRule ${delegatedAuthRuleOption.get} may only contain letters, hyphens and underscores")
  }

  private def validateLevelOfAssurance(authParams: Configuration, path:String) = {
    authParams.getString("levelOfAssurance" ).map( loa => require(validAssuranceLevels contains loa, s"Invalid config $path.levelOfAssurance" ) )
  }

  private def validateDefaultLevelOfAssurance(controllerConfig: Configuration) = {
    controllerConfig.getString("defaultLevelOfAssurance").map( loa => require(validAssuranceLevels contains loa, s"Invalid config:controllers.defaultLevelOfAssurance" ) )
  }

  private def validAuthMode(value: String) = Seq("identity", "passcode").contains(value)

  private def validAgentRole(value: String) = Seq("admin", "assistant").contains(value)

  private def matches(regex: Regex)(value: String): Boolean = {
    value match {
      case regex(found) => true
      case _ => false
    }
  }

  private def countCaptureGroups(regex: Regex): Int = {
    val lParen = "\\("
    val rParen = "\\)"
    val nonRParen = s"[^$rParen]"
    val groups = lParen + nonRParen + "*" + rParen

    groups.r.findAllMatchIn(regex.regex).size
  }

}
