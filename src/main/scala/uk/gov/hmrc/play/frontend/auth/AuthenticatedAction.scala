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

import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Account

import scala.concurrent.Future

trait AuthenticatedAction {
  def apply(body: (AuthContext => (Request[AnyContent]) => Result)): Action[AnyContent]
  def async(body: (AuthContext => (Request[AnyContent]) => Future[Result])): Action[AnyContent]
}

case class AuthRequirements(cl: Int, account: Account)

