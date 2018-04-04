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

package uk.gov.hmrc.softdrinksindustrylevy

import cats.kernel.Group

package object models {

  type Litres = Long
  type LitreBands = (Litres, Litres)

  implicit val litreBandsGroup: Group[(Litres, Litres)] = new Group[(LitreBands)] {
    override def inverse(a: (Litres, Litres)): (Litres, Litres) = (-a._1, -a._2)

    override def empty: (Litres, Litres) = (0, 0)

    override def combine(x: (Litres, Litres), y: (Litres, Litres)): (Litres, Litres) = (x._1 + y._1, x._2 + y._2)
  }

  implicit class LitreOps(litreBands: LitreBands) {
    lazy val lowLevy: BigDecimal = litreBands._1 * BigDecimal("0.18")
    lazy val highLevy: BigDecimal = litreBands._2 * BigDecimal("0.24")
    lazy val dueLevy: BigDecimal = lowLevy + highLevy
  }

}
