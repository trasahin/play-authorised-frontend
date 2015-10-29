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

import play.api.mvc._
import uk.gov.hmrc.play.frontend.auth.connectors.domain.LevelOfAssurance.LOA_2

import scala.concurrent.Future

trait Actions extends UserActions with DelegationDisabled
trait DelegationAwareActions extends UserActions with DelegationEnabled

sealed trait UserActions
  extends SessionTimeoutWrapper
  with UserActionWrapper
  with AuthContextService
  with Authoriser
  with Results {

  self: DelegationDataProvider =>

  private type PlayRequest = (Request[AnyContent] => Result)
  private type AsyncPlayRequest = (Request[AnyContent] => Future[Result])
  private type PlayUserRequest = AuthContext => PlayRequest
  private type AsyncPlayUserRequest = AuthContext => AsyncPlayRequest

  type UserAction = AuthContext => Action[AnyContent]

  implicit def makeAction(body: PlayUserRequest): UserAction = (authContext: AuthContext) => Action(body(authContext))

  implicit def makeFutureAction(body: AsyncPlayUserRequest): UserAction = (authAction: AuthContext) => Action.async(body(authAction))

  def AuthorisedFor(taxRegime: TaxRegime,
                    redirectToOrigin: Boolean = false,
                    pageVisibility: PageVisibilityPredicate = LoaPredicate(LOA_2))
  = new AuthenticatedBy(taxRegime.authenticationType, Some(taxRegime), redirectToOrigin, pageVisibility)

  def AuthenticatedBy(authenticationProvider: AuthenticationProvider,
                      redirectToOrigin: Boolean = false,
                      pageVisibility: PageVisibilityPredicate = LoaPredicate(LOA_2))
  = new AuthenticatedBy(authenticationProvider, None, redirectToOrigin, pageVisibility)


  class AuthenticatedBy(authenticationProvider: AuthenticationProvider,
                        taxRegime: Option[TaxRegime],
                        redirectToOrigin: Boolean,
                        pageVisibility: PageVisibilityPredicate) extends AuthenticatedAction {
    def apply(body: PlayUserRequest): Action[AnyContent] = authorised(authenticationProvider, taxRegime, redirectToOrigin, pageVisibility, body)

    def async(body: AsyncPlayUserRequest): Action[AnyContent] = authorised(authenticationProvider, taxRegime, redirectToOrigin, pageVisibility, body)

    private def authorised(authenticationProvider: AuthenticationProvider,
                           taxRegime: Option[TaxRegime],
                           redirectToOrigin: Boolean,
                           pageVisibility: PageVisibilityPredicate,
                           body: UserAction) =
      WithSessionTimeoutValidation(authenticationProvider) {
        WithUserAuthenticatedBy(authenticationProvider, taxRegime, redirectToOrigin) {
          authContext =>
            WithPageVisibility(pageVisibility, authContext) {
              implicit authContext => body(authContext)
            }
        }
      }
  }

}
