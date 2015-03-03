package uk.gov.hmrc.play.frontend.auth

import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.Session

case class ActingAsAttorneyFor(name:Option[String], identifiers:Map[String, String])

object ActingAsAttorneyFor {
  val ACTING_AS_ATTORNEY_FOR: String = "acting-as-attorney-for"

  implicit val formats = Json.format[ActingAsAttorneyFor]

  private def convertStringToActingAttorney(s : String) : Option[ActingAsAttorneyFor]  = {
    try {
      val jsonRepresentation = Json.parse(s)
      Some(jsonRepresentation.as[ActingAsAttorneyFor])
    } catch {
      case e : Throwable =>
        Logger.error(s"Error converting string to acting attorney: ${e.getMessage}")
        None
    }
  }
  
  def getSessionActingAsAttorneyFor(actingAttorney : ActingAsAttorneyFor, session : Session) : Session = {
    val actingAttorneyJson = Json.toJson(actingAttorney)
    val sessionDataWithAttorneyForNino = session.data - ACTING_AS_ATTORNEY_FOR + (ACTING_AS_ATTORNEY_FOR -> actingAttorneyJson.toString)
    session.copy(data = sessionDataWithAttorneyForNino)
  }

  def getSessionAndStopActingAsAttorney(session : Session) : Session = {
    val sessionDataWithAttorneyForNino = session.data - ACTING_AS_ATTORNEY_FOR
    session.copy(data = sessionDataWithAttorneyForNino)
  }

  def getActingAsAttorneyForUpdateName(newName : String, session : Session) : Option[ActingAsAttorneyFor] = {
    getActingAsAttorneyFor(session) match {
      case Some(a) =>
        Some(a.copy(name = Some(newName)))
      case _ =>
        None
    }
  }
  
  def getActingAsAttorneyFor(session : Session) : Option[ActingAsAttorneyFor] = {
    val maybeAttorney: Option[String] = session.get(ACTING_AS_ATTORNEY_FOR)
    maybeAttorney match {
      case Some(a) =>
        convertStringToActingAttorney(a)
      case _ =>
        None
    }
  }

}
