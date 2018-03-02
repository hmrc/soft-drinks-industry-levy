package uk.gov.hmrc.softdrinksindustrylevy.views

import java.time.LocalDate

import org.jsoup.Jsoup
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.softdrinksindustrylevy.models._
import views.html.variations_pdf

import scala.collection.JavaConverters._

class VariationsSpec extends PlaySpec with GuiceOneAppPerSuite {

  "The variations HTML" when {
    "the trading name has changed" should {
      "contain the updated trading name" in {
        val tradingName = "Generic Soft Drinks Company Inc Ltd LLC Intl GB UK"
        val page = variations_pdf(VariationsRequest(tradingName = Some(tradingName)))
        val html = Jsoup.parse(page.toString)
        val rows = html.select("tr").asScala.map(_.text())

        val subheading = "Organisation Details"

        rows must contain (subheading)
        rows must contain (s"Trading Name $tradingName")
      }
    }

    "the business contact details have changed" should {
      "contain the updated contact details" in {
        val contactDetails = VariationsContact(
          addressLine1 = Some("line 1"),
          addressLine2 = Some("line 2"),
          postCode = Some("AA11 1AA"),
          telephoneNumber = Some("999"),
          emailAddress = Some("aa@bb.cc")
        )

        val page = variations_pdf(VariationsRequest(businessContact = Some(contactDetails)))
        val html = Jsoup.parse(page.toString)
        val rows = html.select("tr").asScala.map(_.text)

        val subheading = "Business Contact Details"

        val expectedRows = Seq(
          s"Address line 1 ${contactDetails.addressLine1.get}",
          s"Address line 2 ${contactDetails.addressLine2.get}",
          s"Postcode ${contactDetails.postCode.get}",
          s"Telephone Number ${contactDetails.telephoneNumber.get}",
          s"Email Address ${contactDetails.emailAddress.get}"
        )

        rows must contain (subheading)
        rows must contain allElementsOf expectedRows
      }
    }

    "the correspondence contact details have changed" should {
      "contain the updated correspondence contact details" in {
        val contactDetails = VariationsContact(
          addressLine1 = Some("line 1"),
          addressLine2 = Some("line 2"),
          postCode = Some("AA11 1AA"),
          telephoneNumber = Some("999"),
          emailAddress = Some("aa@bb.cc")
        )

        val page = variations_pdf(VariationsRequest(correspondenceContact = Some(contactDetails)))
        val html = Jsoup.parse(page.toString)
        val rows = html.select("tr").asScala.map(_.text)

        val subheading = "Correspondence Contact Details"

        val expectedRows = Seq(
          s"Address line 1 ${contactDetails.addressLine1.get}",
          s"Address line 2 ${contactDetails.addressLine2.get}",
          s"Postcode ${contactDetails.postCode.get}",
          s"Telephone Number ${contactDetails.telephoneNumber.get}",
          s"Email Address ${contactDetails.emailAddress.get}"
        )

        rows must contain (subheading)
        rows must contain allElementsOf expectedRows
      }
    }

    "the primary person contact details have changed" should {
      "contain the updated primary contact details" in {
        val personalDetails = VariationsPersonalDetails(
          name = Some("Guy"),
          position = Some("thing"),
          telephoneNumber = Some("111"),
          emailAddress = Some("aa@bb.cc")
        )

        val page = variations_pdf(VariationsRequest(primaryPersonContact = Some(personalDetails)))
        val html = Jsoup.parse(page.toString)
        val rows = html.select("tr").asScala.map(_.text)

        val subheading = "Primary Person Contact Details"

        val expectedRows = Seq(
          s"Name ${personalDetails.name.get}",
          s"Position ${personalDetails.position.get}",
          s"Telephone Number ${personalDetails.telephoneNumber.get}",
          s"Email Address ${personalDetails.emailAddress.get}"
        )

        rows must contain (subheading)
        rows must contain allElementsOf expectedRows
      }
    }

    "the SDIL activity has changed" should {
      "contain the updated SDIL activity" in {
        val activity = SdilActivity(
          activity = InternalActivity(Map(ActivityType.ProducedOwnBrand -> (100L, 200L))),
          produceLessThanOneMillionLitres = Some(true),
          smallProducerExemption = Some(true),
          usesContractPacker = Some(true),
          voluntarilyRegistered = Some(true),
          reasonForAmendment = Some("""¯\_(ツ)_/¯"""),
          estimatedTaxAmount = Some(0),
          taxObligationStartDate = Some(LocalDate.now)
        )

        val page = variations_pdf(VariationsRequest(sdilActivity = Some(activity)))
        val html = Jsoup.parse(page.toString)
        val rows = html.select("tr").asScala.map(_.text)

        val subheading = "Soft Drinks Industry Levy Details"

        val expectedRows = Seq(
          "Producer Yes",
          "Do you produce less than 1 million litres of leviable product per annum? Yes",
          "Are you requesting Small Producer Exemption? Yes",
          "Do you use a Contract Packer? Yes",
          "Customer is accepted as voluntarily reqistered? Yes",
          """Reason For Amendment ¯\_(ツ)_/¯""",
          "Litres Produced in UK each year Higher Rate 200",
          "Lower Rate 100",
          "Estimated amount of tax in the next 12 months £0.00"
        )

        rows must contain (subheading)
        rows must contain allElementsOf expectedRows
      }
    }
  }

  lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val messages: Messages = messagesApi.preferred(request)
  implicit lazy val request: Request[_] = FakeRequest()
}
