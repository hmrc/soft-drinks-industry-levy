package uk.gov.hmrc.softdrinksindustrylevy.models

import org.scalacheck.Gen
import sdil.models.{SdilReturn, SmallProducer}
import uk.gov.hmrc.softdrinksindustrylevy.models.ActivityType.{Copackee, CopackerAll, Imported, ProducedOwnBrand}

object TaxRateUtil {
  val janToMarInt: Gen[Int] = Gen.choose(1, 3)
  val aprToDecInt: Gen[Int] = Gen.choose(4, 12)

  val lowerBandCostPerLitre: BigDecimal = BigDecimal("0.18")
  val higherBandCostPerLitre: BigDecimal = BigDecimal("0.24")

  val lowerBandCostPerLitreMap: Map[Int, BigDecimal] = Map(2025 -> BigDecimal("0.194"))
  val higherBandCostPerLitreMap: Map[Int, BigDecimal] = Map(2025 -> BigDecimal("0.259"))

  def getBandsFromTaxYear(taxYear: Int): (BigDecimal, BigDecimal) = {
    taxYear match {
      case y if y < 2025 => (lowerBandCostPerLitre, higherBandCostPerLitre)
      case _ => (lowerBandCostPerLitreMap(taxYear), higherBandCostPerLitreMap(taxYear))
    }
  }

//  TODO: USE THIS UTIL IN UNIT TESTS
  def calculateLevy(litres: (Long, Long), taxYear: Int): BigDecimal = {
    val bandRates: (BigDecimal, BigDecimal) = getBandsFromTaxYear(taxYear)
    litres._1 * bandRates._1 + litres._2 * bandRates._2
  }

  def getRandomLitres: Long = Math.floor(Math.random() * 1000000).toLong
  def getRandomLitreage: (Long, Long) = (getRandomLitres, getRandomLitres)
  def getRandomSdilRef(index: Int): String = s"${Math.floor(Math.random() * 1000).toLong}SdilRef$index"

  lazy val zero: LitreBands = (0, 0)

  def getInternalActivity(
    hasProduced: Boolean = false,
    hasCopackedAll: Boolean = false,
    hasImported: Boolean = false,
    hasCopackedByOthers: Boolean = false
  ): InternalActivity =
    InternalActivity(
      Map(
        ProducedOwnBrand -> (if (hasProduced) getRandomLitreage else zero),
        CopackerAll      -> (if (hasCopackedAll) getRandomLitreage else zero),
        Imported         -> (if (hasImported) getRandomLitreage else zero),
        Copackee         -> (if (hasCopackedByOthers) getRandomLitreage else zero)
      ),
      false
    )

  def getFullInternalActivity: InternalActivity = getInternalActivity(true, true, true, true)

  def getReturnsRequest(
    packagedNumberOfSmallProducers: Int = 0,
    packagedLargeProducer: Boolean = false,
    importedSmallProducer: Boolean = false,
    importedLargeProducer: Boolean = false,
    exported: Boolean = false,
    wastage: Boolean = false
  ): ReturnsRequest = {
    val smallProducers: Seq[SmallProducerVolume] = (0 to packagedNumberOfSmallProducers)
      .map(index => SmallProducerVolume(getRandomSdilRef(index), getRandomLitreage))
    val returnsPackaged = Option(
      ReturnsPackaging(
        smallProducerVolumes = smallProducers,
        largeProducerVolumes = if (packagedLargeProducer) getRandomLitreage else zero
      )
    )
    val returnsImported = (importedSmallProducer, importedLargeProducer) match {
      case (true, true) =>
        Option(ReturnsImporting(smallProducerVolumes = getRandomLitreage, largeProducerVolumes = getRandomLitreage))
      case (true, false) =>
        Option(ReturnsImporting(smallProducerVolumes = getRandomLitreage, largeProducerVolumes = zero))
      case (false, true) =>
        Option(ReturnsImporting(smallProducerVolumes = zero, largeProducerVolumes = getRandomLitreage))
      case (false, false) => None
    }
    val returnsExported = if (exported) Option(getRandomLitreage) else None
    val returnsWastage = if (wastage) Option(getRandomLitreage) else None
    ReturnsRequest(returnsPackaged, returnsImported, returnsExported, returnsWastage)
  }

  def getFullReturnsRequest: ReturnsRequest = getReturnsRequest(
    packagedNumberOfSmallProducers = 5,
    packagedLargeProducer = true,
    importedSmallProducer = true,
    importedLargeProducer = true,
    exported = true,
    wastage = true
  )

  def getSdilReturn(
    ownBrand: Boolean = false,
    packLarge: Boolean = false,
    numberOfPackSmall: Int = 0,
    importSmall: Boolean = false,
    importLarge: Boolean = false,
    export: Boolean = false,
    wastage: Boolean = false
  ): SdilReturn = {
    val smallProducers: Seq[SmallProducer] = (0 to numberOfPackSmall)
      .map(index => SmallProducer(None, getRandomSdilRef(index), getRandomLitreage))
    SdilReturn(
      ownBrand = if (ownBrand) getRandomLitreage else zero,
      packLarge = if (packLarge) getRandomLitreage else zero,
      packSmall = smallProducers.toList,
      importSmall = if (importSmall) getRandomLitreage else zero,
      importLarge = if (importLarge) getRandomLitreage else zero,
      export = if (export) getRandomLitreage else zero,
      wastage = if (wastage) getRandomLitreage else zero,
      submittedOn = None
    )
  }

  def getFullSdilReturn: SdilReturn = getSdilReturn(
    ownBrand = true,
    packLarge = true,
    numberOfPackSmall = 5,
    importSmall = true,
    importLarge = true,
    export = true,
    wastage = true
  )

}
