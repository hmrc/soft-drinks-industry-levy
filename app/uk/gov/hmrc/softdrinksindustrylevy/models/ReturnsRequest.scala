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

package uk.gov.hmrc.softdrinksindustrylevy.models

import cats.implicits._
import cats.kernel.Monoid

case class ReturnsRequest(packaged: Option[ReturnsPackaging], imported: Option[ReturnsImporting], exported: Option[VolumeBands], wastage: Option[VolumeBands]) {

  lazy val totalLevy: BigDecimal = totalVolumes.dueLevy

  private lazy val totalVolumes: VolumeBands = Monoid.combineAll(Seq(
    packaged.map(_.totalSmallProdVolumes),
    packaged.map(_.largeProducerVolumes),
    imported.map(_.smallProducerVolumes),
    imported.map(_.largeProducerVolumes),
    exported,
    wastage
  ).flatten)
}

case class ReturnsPackaging(smallProducerVolumes: Seq[SmallProducerVolume], largeProducerVolumes: VolumeBands) {
  lazy val totalSmallProdVolumes: VolumeBands = smallProducerVolumes.foldLeft(Monoid[VolumeBands].empty)(_ |+| _.volumes)
}

case class ReturnsImporting(smallProducerVolumes: VolumeBands, largeProducerVolumes: VolumeBands)

case class SmallProducerVolume(producerRef: String, volumes: VolumeBands)

case class VolumeBands(low: Long, high: Long) {
  lazy val lowLevy: BigDecimal = low * BigDecimal("0.18")
  lazy val highLevy: BigDecimal = high * BigDecimal("0.24")

  lazy val dueLevy: BigDecimal = lowLevy + highLevy
}

object VolumeBands {
  implicit val volBandMonoid: Monoid[VolumeBands] = new Monoid[VolumeBands] {
    override def empty: VolumeBands = VolumeBands(0, 0)

    override def combine(x: VolumeBands, y: VolumeBands): VolumeBands = VolumeBands(x.low + y.low, x.high + y.high)
  }
}
