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

import uk.gov.hmrc.softdrinksindustrylevy.util.FakeApplicationSpec

class EncodedConfigSpec extends FakeApplicationSpec {

  "EncodedConfig" should {
    val testDecoded = "I love scala"
    val testEncoded = "SSBsb3ZlIHNjYWxh"
    "b64encode" in {
      EncodedConfig.b64encode(testDecoded) mustBe testEncoded
    }

    "decoder" in {
      EncodedConfig.decoder.decode(testEncoded).map(_.toChar).mkString mustBe testDecoded
    }
  }
}
