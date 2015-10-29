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

package uk.gov.hmrc.play.frontend.auth.connectors.domain

import org.joda.time.DateTime
import play.api.libs.json._
import uk.gov.hmrc.domain._
import uk.gov.hmrc.play.controllers.RestFormats
import uk.gov.hmrc.play.frontend.auth.connectors.domain.LevelOfAssurance.LevelOfAssurance
import uk.gov.hmrc.time.DateTimeUtils


object LevelOfAssurance extends Enumeration {
  type LevelOfAssurance = Value
  val LOA_1 = Value("1")
  val LOA_1_5 = Value("1.5")
  val LOA_2 = Value("2")

  implicit val format = new Format[LevelOfAssurance] {

    override def reads(json: JsValue): JsResult[LevelOfAssurance] = json match {
      case JsString(v) => try {
        JsSuccess(LevelOfAssurance.withName(v))
      } catch {
        case e: NoSuchElementException => JsError(s"Invalid value for LevelOfAssurance: '$v'")
      }
      case _ => JsError("String value expected")
    }

    override def writes(v: LevelOfAssurance): JsValue = JsString(v.toString)
  }
}

case class Authority(uri: String,
                     accounts: Accounts,
                     loggedInAt: Option[DateTime],
                     previouslyLoggedInAt: Option[DateTime],
                     levelOfAssurance: LevelOfAssurance = LevelOfAssurance.LOA_2)

object Authority {
  implicit val format = {
    implicit val dateFormat = RestFormats.dateTimeFormats
    implicit val accountsFormat = Accounts.format
    implicit val levelOfAssuranceFormat = LevelOfAssurance.format
    Json.format[Authority]
  }
}

case class IdaPid(pid: String)

object IdaPid {
  implicit val format = {
    Json.format[IdaPid]
  }
}


sealed trait AgentRole {
  def satisfiesRequiredRole(role: AgentRole): Boolean
}

object AgentRole {

  implicit def format: Format[AgentRole] = new Format[AgentRole] {

    override def reads(json: JsValue): JsResult[AgentRole] = {
      json.as[JsString].value match {
        case "admin" => JsSuccess(AgentAdmin)
        case "assistant" => JsSuccess(AgentAssistant)
        case other => throw new Exception(s"Unexpected role: $other")
      }
    }

    override def writes(role: AgentRole): JsValue = {
      role match {
        case AgentAdmin => JsString("admin")
        case AgentAssistant => JsString("assistant")
        case other => throw new Exception(s"Unexpected role: $other")
      }
    }
  }
}

object AgentAdmin extends AgentRole {
  def satisfiesRequiredRole(role: AgentRole): Boolean = {
    role match {
      case AgentAdmin => true
      case AgentAssistant => true
    }
  }
}

object AgentAssistant extends AgentRole {
  def satisfiesRequiredRole(role: AgentRole): Boolean = {
    role match {
      case AgentAdmin => false
      case AgentAssistant => true
    }
  }
}


case class AgentAccount(link: String, agentCode: AgentCode, agentUserId: AgentUserId,
                        agentUserRole: AgentRole,
                        payeReference: Option[PayeAgentReference],
                        agentBusinessUtr: Option[AgentBusinessUtr] = None) extends Account

object AgentAccount {
  implicit val format = Json.format[AgentAccount]
}


case class Accounts(paye: Option[PayeAccount] = None,
                    sa: Option[SaAccount] = None,
                    ct: Option[CtAccount] = None,
                    vat: Option[VatAccount] = None,
                    epaye: Option[EpayeAccount] = None,
                    agent: Option[AgentAccount] = None,
                    tai: Option[TaxForIndividualsAccount] = None,
                    taxsAgent: Option[TaxSummariesAgentAccount] = None,
                    tcs: Option[TaxCreditServiceAccount] = None,
                    ei: Option[EIAccount] = None,
                    org: Option[OrgAccount] = None,
                    ated: Option[AtedAccount] = None,
                    awrs: Option[AwrsAccount] = None,
                    gmp: Option[GmpAccount] = None,
                    iht: Option[IhtAccount] = None) {
  val toMap: Map[String, String] = Map() ++
    sa.map("saUtr" -> _.utr.utr).toMap ++
    vat.map("vrn" -> _.vrn.vrn).toMap ++
    ct.map("ctUtr" -> _.utr.utr).toMap ++
    epaye.map("empRef" -> _.empRef.toString).toMap ++
    paye.map("nino" -> _.nino.nino).toMap ++
    org.map("org" -> _.org.org).toMap ++
    ei.map("empRef" -> _.empRef.toString).toMap ++
    agent.map("agentCode" -> _.agentCode.toString()).toMap ++
    ated.map("atedUtr" -> _.utr.utr).toMap ++
    awrs.map("awrsUtr" -> _.utr.utr).toMap ++
    gmp.map("psaId" -> _.id.id).toMap ++
    taxsAgent.map("uar" -> _.uar.uar).toMap ++
    iht.map("iht" -> _.nino.nino).toMap

  def contains(account: Account) = {
    account match {
      case a: PayeAccount => val nino = toMap.get("nino"); Option(a.nino.value) == nino
      case a: TaxForIndividualsAccount => val nino = toMap.get("nino"); Option(a.nino.value) == nino
      case a: SaAccount => val saUtr = toMap.get("saUtr"); Option(a.utr.value) == saUtr
      case a: CtAccount => val ctUtr = toMap.get("ctUtr"); Option(a.utr.value) == ctUtr
      case a: VatAccount => val vrn = toMap.get("vrn"); Option(a.vrn.value) == vrn
      case a: EpayeAccount => val empRef = toMap.get("empRef"); Option(a.empRef.value) == empRef
      case a: AgentAccount => val agentCode = toMap.get("agentCode"); Option(a.agentCode.value) == agentCode
      case a: TaxSummariesAgentAccount => val uar = toMap.get("uar"); Option(a.uar.value) == uar
      case a: TaxCreditServiceAccount => val nino = toMap.get("nino"); Option(a.nino.value) == nino
      case a: EIAccount => val empRef = toMap.get("empRef"); Option(a.empRef.value) == empRef
      case a: OrgAccount => val org = toMap.get("org"); Option(a.org.value) == org
      case a: AtedAccount => val atedUtr = toMap.get("atedUtr"); Option(a.utr.value) == atedUtr
      case a: AwrsAccount => val awrsUtr = toMap.get("awrsUtr"); Option(a.utr.value) == awrsUtr
      case a: GmpAccount => val psaId = toMap.get("psaId"); Option(a.id.value) == psaId
      case a: IhtAccount => val nino = toMap.get("nino"); Option(a.nino.value) == nino
      case _ => false
    }
  }
}

object Accounts {
  implicit val format = {
    implicit val payeFormat = Json.format[PayeAccount]
    implicit val saFormat = Json.format[SaAccount]
    implicit val ctFormat = Json.format[CtAccount]
    implicit val vatFormat = Json.format[VatAccount]
    implicit val epayeFormat = Json.format[EpayeAccount]
    implicit val tfiFormat = Json.format[TaxForIndividualsAccount]
    implicit val taxsAgentFormat = Json.format[TaxSummariesAgentAccount]
    implicit val tcsFormat = Json.format[TaxCreditServiceAccount]
    implicit val ihtFormat = Json.format[IhtAccount]
    implicit val ninoFormat = Json.format[Nino]
    implicit val orgFormat = Json.format[OrgAccount]
    implicit val agentFormat = Json.format[AgentAccount]
    implicit val eiFormat = Json.format[EIAccount]
    implicit val gmpFormat = Json.format[GmpAccount]
    implicit val atedFormat = Json.format[AtedAccount]
    implicit val awrsFormat = Json.format[AwrsAccount]
    Json.format[Accounts]
  }
}

case class PayeAccount(link: String, nino: Nino) extends Account

case class TaxForIndividualsAccount(link: String, nino: Nino) extends Account

case class TaxCreditServiceAccount(link: String, nino: Nino) extends Account

case class SaAccount(link: String, utr: SaUtr) extends Account

case class CtAccount(link: String, utr: CtUtr) extends Account

case class VatAccount(link: String, vrn: Vrn) extends Account

case class EpayeAccount(link: String, empRef: EmpRef) extends Account

case class TaxSummariesAgentAccount(link: String, uar: Uar) extends Account

case class OrgAccount(link: String, org: Org) extends Account

case class EIAccount(link: String, empRef: EmpRef) extends Account

case class AtedAccount(link: String, utr: AtedUtr) extends Account

case class AwrsAccount(link: String, utr: AwrsUtr) extends Account

case class GmpAccount(link: String, id: PsaId) extends Account

case class IhtAccount(link: String, nino: Nino) extends Account

sealed abstract class Account {
  val link: String
}

case class CreationAndLastModifiedDetail(createdAt: DateTime, lastUpdated: DateTime)

object CreationAndLastModifiedDetail {
  def apply(): CreationAndLastModifiedDetail = CreationAndLastModifiedDetail(DateTimeUtils.now, DateTimeUtils.now)
}
