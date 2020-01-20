/*
 * Copyright 2020 HM Revenue & Customs
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

import java.util.Base64

import com.typesafe.config._
import com.typesafe.config.impl.Parseable
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.Base64ConfigDecoder

import scala.collection.JavaConverters._

object EncodedConfig {

  lazy val decoder = Base64.getDecoder

  def b64encode(in: String): String =
    Base64.getEncoder.encodeToString(in.getBytes("UTF-8"))

  def apply(in: Config): Config =
    in.entrySet.asScala.foldLeft(in) {
      case (config, entry) =>
        val key = entry.getKey
        if (key.endsWith("-b64encoded")) {
          val value = new String(decoder.decode(in.getString(key)), "UTF-8")
          val newVal = Parseable.newString(value, ConfigParseOptions.defaults).parse()
          config.withValue(key.replace("-b64encoded", ""), newVal)
        } else
          config
    }
}

object DefaultBase64ConfigDecoder extends Base64ConfigDecoder {
  override def decodeConfig(configuration: Configuration): Configuration = super.decodeConfig(configuration)
}
