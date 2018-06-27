/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.softdrinksindustrylevy.config

import java.time.Clock

import akka.stream.Materializer
import play.api.{Configuration, Environment}
import play.api.Mode.Mode
import play.api.i18n.MessagesApi
import play.api.inject.ApplicationLifecycle

import scala.concurrent.ExecutionContext

trait PlayWiring {
  def applicationLifecycle: ApplicationLifecycle
  def configuration: Configuration
  def environment: Environment
  implicit def executionContext: ExecutionContext
  implicit def materializer: Materializer
  def messagesApi: MessagesApi
  implicit def clock: Clock = Clock.systemDefaultZone()

  lazy val mode: Mode = environment.mode

  val config: SdilConfig = {
    import pureconfig._
    import pureconfig.configurable._
    import java.time.format._
    implicit val localDateInstance = localDateConfigConvert(DateTimeFormatter.ISO_DATE)
    pureconfig.loadConfig[SdilConfig] match {
      case Left(error) => throw new IllegalStateException(error.toString)
      case Right(conf) => conf
    }
  }


}
