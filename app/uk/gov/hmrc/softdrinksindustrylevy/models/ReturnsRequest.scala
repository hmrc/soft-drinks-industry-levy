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

case class ReturnsRequest(packaged: Option[ReturnsPackaging], imported: Option[ReturnsImporting], otherActivity: Map[ActivityType.Value, VolumeBands]) {
  lazy val totalLevy: BigDecimal = totalVolumes.dueLevy

  private lazy val totalVolumes: VolumeBands = Seq(
    packaged.map(_.totalSmallProdVolumes),
    packaged.map(_.largeProducerVolumes),
    imported.map(_.smallProducerVolumes),
    imported.map(_.largeProducerVolumes),
    otherActivity.get(ActivityType.Exported),
    otherActivity.get(ActivityType.Wastage)
  ).flatten.foldLeft(VolumeBands.zero)(_ + _)
}

case class ReturnsPackaging(smallProducerVolumes: Seq[SmallProducerVolume], largeProducerVolumes: VolumeBands) {
  lazy val totalSmallProdVolumes: VolumeBands = smallProducerVolumes.foldLeft(VolumeBands.zero)(_ + _.volumes)
}

case class ReturnsImporting(smallProducerVolumes: VolumeBands, largeProducerVolumes: VolumeBands)

case class SmallProducerVolume(producerRef: String, volumes: VolumeBands)

case class VolumeBands(low: Long, high: Long) {
  lazy val lowLevy: BigDecimal = low * BigDecimal("0.18")
  lazy val highLevy: BigDecimal = high * BigDecimal("0.24")

  lazy val dueLevy: BigDecimal = lowLevy + highLevy

  def +(other: VolumeBands) = VolumeBands(low + other.low, high + other.high)
}

object VolumeBands {
  val zero = VolumeBands(0, 0)
}