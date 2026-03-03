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

package uk.gov.hmrc.softdrinksindustrylevy.util

import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import play.api.Configuration
import uk.gov.hmrc.softdrinksindustrylevy.config.{AppConfig, AppConfigHolder, SdilBandRatesConfig}

trait WithBandRatesConfig extends BeforeAndAfterAll { self: org.scalatest.Suite =>

  override protected def beforeAll(): Unit = {
    super.beforeAll()

    val rawConfig = ConfigFactory.parseString("""
      sdil {
        bandRates = [
          {
            startDate = "2018-04-01"
            endDate   = "2025-03-31"
            lowerBandCostPerLitre  = "0.18"
            higherBandCostPerLitre = "0.24"
          },
          {
            startDate = "2025-04-01"
            lowerBandCostPerLitre  = "0.194"
            higherBandCostPerLitre = "0.259"
          }
        ]
      }
    """)

    val playConfig: Configuration = Configuration(rawConfig)
    val bandRatesConfig = new SdilBandRatesConfig(playConfig)
    val appConfig = new AppConfig(bandRatesConfig)

    AppConfigHolder.set(appConfig)
  }
}
