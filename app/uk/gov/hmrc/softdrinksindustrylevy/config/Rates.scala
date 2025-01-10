package uk.gov.hmrc.softdrinksindustrylevy.config

object Rates {

  val lowerBandCostPerLitreString: String = "0.18"
  val higherBandCostPerLitreString: String = "0.24"

  val lowerBandCostPerLitre: BigDecimal = BigDecimal(lowerBandCostPerLitreString)
  val higherBandCostPerLitre: BigDecimal = BigDecimal(higherBandCostPerLitreString)

}
