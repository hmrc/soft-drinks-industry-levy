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

import cats.syntax.semigroup._
import cats.instances.option._
import cats.kernel.Monoid
import sdil.models._

@deprecated("use SdilReturn", "0.36")
case class ReturnsRequest(
  packaged: Option[ReturnsPackaging],
  imported: Option[ReturnsImporting],
  exported: Option[LitreBands],
  wastage: Option[LitreBands]) {

  lazy val totalLevy: BigDecimal = liableVolumes.dueLevy - nonLiableVolumes.dueLevy

  private lazy val liableVolumes = (packaged.map(_.largeProducerVolumes) |+| imported.map(_.largeProducerVolumes))
    .getOrElse(Monoid[LitreBands].empty)

  private lazy val nonLiableVolumes: LitreBands = (exported |+| wastage).getOrElse(Monoid[LitreBands].empty)

}
@deprecated("use SdilReturn", "0.36")
case class ReturnsPackaging(smallProducerVolumes: Seq[SmallProducerVolume], largeProducerVolumes: LitreBands) {
  lazy val totalSmallProdVolumes: LitreBands = smallProducerVolumes.foldLeft(Monoid[LitreBands].empty)(_ |+| _.volumes)
}

@deprecated("use SdilReturn", "0.36")
case class ReturnsImporting(smallProducerVolumes: LitreBands, largeProducerVolumes: LitreBands)

@deprecated("use SdilReturn", "0.36")
case class SmallProducerVolume(producerRef: String, volumes: LitreBands)

object ReturnsRequest {
  import cats.implicits._
  def apply(sdilReturn: SdilReturn): ReturnsRequest = {

    val pack = ReturnsPackaging(
      sdilReturn.packSmall.map { sp =>
        SmallProducerVolume(sp.sdilRef, sp.litreage)
      },
      (sdilReturn.packLarge._1 + sdilReturn.ownBrand._1, sdilReturn.packLarge._2 + sdilReturn.ownBrand._2)
    )

    ReturnsRequest(
      packaged = pack.some,
      imported = ReturnsImporting(sdilReturn.importSmall, sdilReturn.importLarge).some,
      exported = sdilReturn.export.some,
      wastage = sdilReturn.wastage.some
    )
  }
}
