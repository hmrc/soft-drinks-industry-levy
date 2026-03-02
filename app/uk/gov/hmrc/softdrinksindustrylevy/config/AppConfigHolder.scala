/*
 * Copyright 2026 HM Revenue & Customs
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
import scala.compiletime.uninitialized

object AppConfigHolder {
  @volatile private var value: AppConfig = uninitialized

  def set(appConfig: AppConfig): Unit =
    value = appConfig

  def get: AppConfig =
    if (value == null)
      throw new IllegalStateException("AppConfigHolder not initialised (missing eager binding).")
    else value
}
