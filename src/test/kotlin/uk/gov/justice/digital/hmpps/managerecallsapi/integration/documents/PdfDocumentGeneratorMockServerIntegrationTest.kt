package uk.gov.justice.digital.hmpps.managerecallsapi.integration.documents

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.core.io.ClassPathResource
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ClassPathDocumentDetail
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers.GotenbergMockServer
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomString

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(PER_CLASS)
class PdfDocumentGeneratorMockServerIntegrationTest {

  @Autowired
  lateinit var gotenbergMockServer: GotenbergMockServer

  @Autowired
  lateinit var pdfDocumentGenerationService: PdfDocumentGenerationService

  @BeforeAll
  fun startMocks() {
    gotenbergMockServer.start()
  }

  @AfterAll
  fun stopMocks() {
    gotenbergMockServer.stop()
  }

  @Test
  fun `should return byte array when generating pdf from html`() {
    val expectedGeneratedPdf = randomString()
    val html = "<body><span>${randomString()}</span></body>"
    gotenbergMockServer.stubPdfGeneration(expectedGeneratedPdf.toByteArray(), html, "revocation-order-logo")

    val generatedPdf = pdfDocumentGenerationService.generatePdf(
      html,
      ClassPathDocumentDetail("revocation-order-logo.png")
    ).block()!!

    assertThat(String(generatedPdf), equalTo(expectedGeneratedPdf))
  }

  @Test
  fun `should return byte array when merging many pdfs to one`() {
    val expectedMergedPdf = randomString()
    gotenbergMockServer.stubMergePdfs(
      expectedMergedPdf.toByteArray(),
      ClassPathResource("/document/licence.pdf").file.readText(),
      ClassPathResource("/document/revocation-order.pdf").file.readText()
    )

    val details = listOf(
      ClassPathDocumentDetail("licence.pdf", "/document/licence.pdf"),
      ClassPathDocumentDetail("revocation-order.pdf", "/document/revocation-order.pdf")
    )

    val mergedPdf = pdfDocumentGenerationService.mergePdfs(details).block()!!

    assertThat(String(mergedPdf), equalTo(expectedMergedPdf))
  }
}
