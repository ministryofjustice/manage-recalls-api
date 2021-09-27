package uk.gov.justice.digital.hmpps.managerecallsapi.integration.documents

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ClassPathDocumentData
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ClassPathImageData
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.GotenbergApi
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.matchers.isPdfWithNumberOfPages
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallImage.RevocationOrderLogo

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
                <img width=65 height=57 id=logo.png src="${RevocationOrderLogo.fileName}"/>
              </center>
          </body>
        </html>
    """.trimIndent()

    val generatedBytes = pdfDocumentGenerationService.generatePdf(
      html,
      ClassPathImageData(RevocationOrderLogo)
    ).block()!!

    assertThat(generatedBytes, isPdfWithNumberOfPages(equalTo(1)))
  }

  @Test
  fun `should return byte array when merging pdfs`() {
    val details = listOf(
      ClassPathDocumentData("/document/licence.pdf"),
      ClassPathDocumentData("/document/revocation-order.pdf"),
      ClassPathDocumentData("/document/landscape.pdf")
    )

    val mergedBytes = pdfDocumentGenerationService.mergePdfs(details).block()!!

    assertThat(mergedBytes, isPdfWithNumberOfPages(equalTo(5)))
  }
}
