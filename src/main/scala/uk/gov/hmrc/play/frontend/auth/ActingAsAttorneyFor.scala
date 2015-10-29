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

package uk.gov.hmrc.play.frontend.auth

import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.Session

@deprecated("Use AuthContext.principal and AuthContext.attorney", since = "March 2015")
case class ActingAsAttorneyFor(name:Option[String], identifiers:Map[String, String])

@deprecated("Use AuthContext.principal and AuthContext.attorney", since = "March 2015")
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
