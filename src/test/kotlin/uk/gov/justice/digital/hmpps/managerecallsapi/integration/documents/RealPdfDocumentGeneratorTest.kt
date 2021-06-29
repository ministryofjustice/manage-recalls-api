package uk.gov.justice.digital.hmpps.managerecallsapi.integration.documents

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.greaterThan
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerator

@SpringBootTest
class RealPdfDocumentGeneratorTest(
  @Autowired private val pdfDocumentGenerator: PdfDocumentGenerator
) {
  @Test
  fun `should return byte array when requesting pdf`() {
    val makePdfResult = pdfDocumentGenerator.makePdf()

    // TODO investigate better assertion
    // Do not trying to test gotenberg, better assertion on further stories
    assertThat(makePdfResult.size, greaterThan(0))
  }
}
