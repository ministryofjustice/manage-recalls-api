package uk.gov.justice.digital.hmpps.managerecallsapi.integration.documents

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.greaterThan
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import reactor.test.StepVerifier
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ClassPathDocumentDetail
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerator
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.StringDocumentDetail

class RealPdfDocumentGeneratorTest {

  private val pdfDocumentGenerator = PdfDocumentGenerator(
    WebClient.builder().build(),
    System.getenv("GOTENBERG_ENDPOINT_URL") ?: "http://localhost:9093",
  )

  @Test
  fun `should return byte array when requesting pdf`() {
    val details = listOf(
      StringDocumentDetail("index.html", "<html><body></body></html>"),
      ClassPathDocumentDetail("revocation-order-logo.png", "/templates/images/revocation-order-logo.png")
    )

    val makePdfResult = pdfDocumentGenerator.makePdf(details)

    StepVerifier
      .create(makePdfResult)
      .assertNext {
        assertThat(it.size, greaterThan(0))
        // java.io.File("makePdfTest.pdf").writeBytes(it)   // uncomment to save to temp file for viewing
      }
      .verifyComplete()
  }

  @Test
  fun `should return byte array when merging pdfs`() {
    val details = listOf(
      ClassPathDocumentDetail("a.pdf", "/document/licence.pdf"),
      ClassPathDocumentDetail("b.pdf", "/document/revocation-order.pdf")
    )

    val pdfResult = pdfDocumentGenerator.mergePdfs(details)

    StepVerifier
      .create(pdfResult)
      .assertNext {
        assertThat(it.size, greaterThan(0))
        // java.io.File("mergePdfsTest.pdf").writeBytes(it)   // uncomment to save to temp file for viewing
      }
      .verifyComplete()
  }
}
