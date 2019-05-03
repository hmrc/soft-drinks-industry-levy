/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.softdrinksindustrylevy.services

import org.scalatest.mockito.MockitoSugar
import sdil.models.{ReturnPeriod, SdilReturn}
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.softdrinksindustrylevy.services.DataCorrector.ReturnsCorrection

class DataCorrectorSpec extends UnitSpec with MockitoSugar{

  "ReturnsCorrection" should {
    val testSdilRef = Some("123")
    val testUtr = Some("456")

    "sdilRef and utr not defined" in {
      the [IllegalArgumentException] thrownBy ReturnsCorrection(None, None, mock[ReturnPeriod], mock[SdilReturn]) should have message("requirement failed: Either sdilRef or utr must be defined")
    }

    "Only sdilRef defined" in {
      ReturnsCorrection(testSdilRef, None, mock[ReturnPeriod], mock[SdilReturn]).sdilRef shouldBe testSdilRef
    }

    "Only utr defined" in {
      ReturnsCorrection(None, testUtr, mock[ReturnPeriod], mock[SdilReturn]).utr shouldBe testUtr
    }

    "sdilRef and utr both defined" in {
      val result = ReturnsCorrection(testSdilRef, testUtr, mock[ReturnPeriod], mock[SdilReturn])
      result.sdilRef shouldBe testSdilRef
      result.utr shouldBe testUtr
    }
  }
}
