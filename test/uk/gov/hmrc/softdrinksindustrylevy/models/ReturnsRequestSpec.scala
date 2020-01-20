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

package uk.gov.hmrc.softdrinksindustrylevy.models

import org.scalatest.mockito.MockitoSugar
import sdil.models.{SdilReturn, SmallProducer}
import uk.gov.hmrc.softdrinksindustrylevy.util.FakeApplicationSpec

class ReturnsRequestSpec extends FakeApplicationSpec with MockitoSugar {
  "ReturnsPackaging" should {
    "totalSmallProdVolumes" in {
      val testSmallProdLitre1 = (109L, 110L)
      val testSmallProdLitre2 = (111L, 112L)
      val testSmallProd1 = SmallProducerVolume("", testSmallProdLitre1)
      val testSmallProd2 = SmallProducerVolume("", testSmallProdLitre2)

      val result = ReturnsPackaging(Seq(testSmallProd1, testSmallProd2), (0, 0)).totalSmallProdVolumes

      result._1 mustBe testSmallProdLitre1._1 + testSmallProdLitre2._1
      result._2 mustBe testSmallProdLitre1._2 + testSmallProdLitre2._2
    }
  }

  "ReturnsRequest" should {
    "apply converts values correctly" in {
      val testSdilRef = "someSdilRef"
      val testPackSmallLitre = (109L, 110L)
      val testPackSmall = SmallProducer(Some("Small producer co"), testSdilRef, testPackSmallLitre)
      val testImportSmall = (111L, 112L)
      val testImportLarge = (113L, 114L)
      val testPackLarge = (115L, 116L)
      val testOwnBrand = (117L, 118L)
      val testExport = (119L, 120L)
      val testWastage = (121L, 122L)

      val testSdilReturn = SdilReturn(
        importSmall = testImportSmall,
        importLarge = testImportLarge,
        packLarge = testPackLarge,
        ownBrand = testOwnBrand,
        export = testExport,
        wastage = testWastage,
        packSmall = List(testPackSmall),
        submittedOn = None
      )

      val result = ReturnsRequest(testSdilReturn)

      val resultSmallProducer = result.packaged.get.smallProducerVolumes.head
      resultSmallProducer.producerRef mustBe testSdilRef
      resultSmallProducer.volumes._1 mustBe testPackSmallLitre._1
      resultSmallProducer.volumes._2 mustBe testPackSmallLitre._2
      result.packaged.get.largeProducerVolumes._1 mustBe testPackLarge._1 + testOwnBrand._1
      result.packaged.get.largeProducerVolumes._2 mustBe testPackLarge._2 + testOwnBrand._2

      result.imported.get.smallProducerVolumes._1 mustBe testImportSmall._1
      result.imported.get.smallProducerVolumes._2 mustBe testImportSmall._2
      result.imported.get.largeProducerVolumes._1 mustBe testImportLarge._1
      result.imported.get.largeProducerVolumes._2 mustBe testImportLarge._2

      result.exported.get._1 mustBe testExport._1
      result.exported.get._2 mustBe testExport._2

      result.wastage.get._1 mustBe testWastage._1
      result.wastage.get._2 mustBe testWastage._2
    }
  }
}
