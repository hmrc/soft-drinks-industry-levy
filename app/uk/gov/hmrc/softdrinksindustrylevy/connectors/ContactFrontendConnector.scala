/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.softdrinksindustrylevy.connectors

import com.google.inject.{Inject, Singleton}
import play.api.{Configuration, Mode}
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.softdrinksindustrylevy.models.Subscription
import uk.gov.hmrc.softdrinksindustrylevy.models.json.internal.subscriptionFormat

import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ContactFrontendConnector @Inject() (http: HttpClientV2, val mode: Mode, val configuration: Configuration)
    extends ServicesConfig(configuration) {

  lazy val contactFrontendUrl: String = baseUrl("contact-frontend")

  def raiseTicket(subscription: Subscription, safeId: String)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Unit] = {

    // nothing useful we can do with this
    val resubmitUrl = "/"

    val payload = Map(
      "contact-name"  -> Seq("SDIL service"),
      "contact-email" -> Seq("sdil.service.email@somewhere.com"),
      "contact-comments" ->
        Seq(
          s"""
             |Subscription stuck in pending queue:
             |
             |${Json.prettyPrint(Json.toJson(subscription))}
             |
             |Safe Id: $safeId
             |
             |Submitted at: ${LocalDateTime.now()}
           """.stripMargin
        ),
      "isJavascript" -> Seq("false"),
      "referer"      -> Seq("soft-drinks-industry-levy"),
      "csrfToken"    -> Seq("nocheck"),
      "service"      -> Seq("soft-drinks-industry-levy")
    )

    val submitUrl = s"$contactFrontendUrl/contact/contact-hmrc/form?resubmitUrl=$resubmitUrl"
    http
      .post(url"$submitUrl")
      .transform(_.addHttpHeaders("Csrf-Token" -> "nocheck"))
      .withBody(payload)
      .execute[HttpResponse]
      .map(_ => ())
  }
}
