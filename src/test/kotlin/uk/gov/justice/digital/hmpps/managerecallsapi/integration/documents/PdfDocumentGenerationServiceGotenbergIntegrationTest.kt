package uk.gov.justice.digital.hmpps.managerecallsapi.integration.documents

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ClassPathDocumentDetail
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.GotenbergApi
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.matchers.isPdfWithNumberOfPages

class PdfDocumentGenerationServiceGotenbergIntegrationTest {

  private val gotenbergApi = GotenbergApi(
    WebClient.builder().build(),
    System.getenv("GOTENBERG_ENDPOINT_URL") ?: "http://localhost:9093"
  )
  private val pdfDocumentGenerationService = PdfDocumentGenerationService(gotenbergApi)

  @Test
  fun `should return byte array when generating a pdf`() {
    val html = """
        <html>
          <body>
              <center>
                <h1>Test landscape pdf</h1>
                <img width=65 height=57 id=logo.png src="revocation-order-logo.png"/>
              </center>
          </body>
        </html>
        """.trimIndent()

    val generatedBytes = pdfDocumentGenerationService.generatePdf(
      html,
      ClassPathDocumentDetail("revocation-order-logo.png")
    ).block()!!

    java.io.File("generated.pdf").writeBytes(generatedBytes) // uncomment to save to temp file for viewing
    assertThat(generatedBytes, isPdfWithNumberOfPages(equalTo(1)))
  }

  @Test
  fun `should return byte array when merging pdfs`() {
    val details = listOf(
      ClassPathDocumentDetail("1.pdf", "/document/licence.pdf"),
      ClassPathDocumentDetail("2.pdf", "/document/revocation-order.pdf"),
      ClassPathDocumentDetail("3.pdf", "/document/landscape.pdf")
    )

    val mergedBytes = pdfDocumentGenerationService.mergePdfs(details).block()!!

    java.io.File("merged.pdf").writeBytes(mergedBytes) // uncomment to save to temp file for viewing
    assertThat(mergedBytes, isPdfWithNumberOfPages(equalTo(5)))
  }
}
