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
                    gmp: Option[GmpAccount] = None) {
  def toMap = Map() ++
    sa.map("saUtr" -> _.utr.utr).toMap ++
    vat.map("vrn" -> _.vrn.vrn).toMap ++
    ct.map("ctUtr" -> _.utr.utr).toMap ++
    epaye.map("empRef" -> _.empRef.toString).toMap ++
    paye.map("nino" -> _.nino.nino).toMap ++
    org.map("org" -> _.org.org).toMap ++
    ei.map("empRef" -> _.empRef.toString).toMap ++
    agent.map("agentCode" -> _.agentCode).toMap ++
    ated.map("atedUtr" -> _.utr.utr).toMap ++
    gmp.map("psaId" -> _.psaId.id).toMap ++
    taxsAgent.map("uar" -> _.uar.uar).toMap
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
    implicit val ninoFormat = Json.format[Nino]
    implicit val orgFormat = Json.format[OrgAccount]
    implicit val agentFormat = Json.format[AgentAccount]
    implicit val eiFormat = Json.format[EIAccount]
    implicit val gmpFormat = Json.format[GmpAccount]
    implicit val atedFormat = Json.format[AtedAccount]
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

case class GmpAccount(link: String, psaId: PsaId) extends Account

sealed abstract class Account {
  val link: String
}

case class CreationAndLastModifiedDetail(createdAt: DateTime, lastUpdated: DateTime)

object CreationAndLastModifiedDetail {
  def apply(): CreationAndLastModifiedDetail = CreationAndLastModifiedDetail(DateTimeUtils.now, DateTimeUtils.now)
}
