/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.softdrinksindustrylevy.services.dms

import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class PdfGenerationServiceSpec extends AnyWordSpecLike with Matchers {
  val pdfGenerationService = new PdfGenerationService()

  private val blankHtml =
    """
      |<!DOCTYPE html>
      |<html lang="en">
      |
      |<head>
      |    <title>Blah</title>
      |</head>
      |
      |<body>
      |
      |<div class="container">
      |   <p>Blah</p>
      |</div>
      |
      |</body>
      |</html>
      |""".stripMargin

  "PdfGenerationService.generatePDFBytes" should {
    "successfully generate pdf bytes" in {
      val pdfBytes = pdfGenerationService.generatePDFBytes(blankHtml)

      pdfBytes.length should be > 0

      try {
        val doc = Loader.loadPDF(pdfBytes)
        doc.getNumberOfPages shouldBe 1
        val page = doc.getPage(0)
        page.hasContents shouldBe true
        val text = new PDFTextStripper().getText(doc)
        text.strip shouldBe "Blah"
      } catch {
        case x: Throwable => fail(s"Failed to parse content into Pdf bytes - ${x.getMessage}")
      }
    }
  }
}
