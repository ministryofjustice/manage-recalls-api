package uk.gov.justice.digital.hmpps.managerecallsapi.integration.documents

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.greaterThan
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerator
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RevocationOrder

class RealPdfDocumentGeneratorTest() {

  private val pdfDocumentGenerator = PdfDocumentGenerator(
    WebClient.builder().build(),
    System.getenv("GOTENBERG_ENDPOINT_URL") ?: "http://localhost:9093",
    RevocationOrder()
  )

  @Test
  fun `should return byte array when requesting pdf`() {
    val makePdfResult = pdfDocumentGenerator.makePdf()

    // TODO investigate better assertion
    // Do not trying to test gotenberg, better assertion on further stories
    assertThat(makePdfResult.size, greaterThan(0))
  }
}
