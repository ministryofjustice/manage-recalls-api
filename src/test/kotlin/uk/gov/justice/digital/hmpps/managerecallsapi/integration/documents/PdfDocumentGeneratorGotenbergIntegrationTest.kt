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
import org.springframework.test.context.ActiveProfiles
import reactor.test.StepVerifier
import uk.gov.justice.digital.hmpps.managerecallsapi.component.randomString
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ClassPathDocumentDetail
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.HtmlDocumentDetail
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerator
import uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers.GotenbergMockServer

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(PER_CLASS)
class PdfDocumentGeneratorGotenbergIntegrationTest {

  @Autowired
  lateinit var gotenbergMockServer: GotenbergMockServer

  @Autowired
  lateinit var pdfDocumentGenerator: PdfDocumentGenerator

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
    val stubResponseAsString = randomString()
    val textOfHtml = randomString()
    gotenbergMockServer.stubPdfGeneration(stubResponseAsString.toByteArray(), textOfHtml)

    val details = listOf(
      HtmlDocumentDetail("index.html", "<body><span>$textOfHtml</span></body>"),
      ClassPathDocumentDetail("logo.png", "/document/template/revocation-order/logo.png")
    )

    val pdfResult = pdfDocumentGenerator.makePdf(details)

    StepVerifier
      .create(pdfResult)
      .assertNext {
        assertThat(String(it), equalTo(stubResponseAsString))
      }
      .verifyComplete()
  }

  @Test
  fun `should return byte array when merging many pdfs to one`() {
    val stubResponseAsString = randomString()
    gotenbergMockServer.stubMergePdfs(stubResponseAsString.toByteArray())

    val details = listOf(
      ClassPathDocumentDetail("a.pdf", "/document/licence.pdf"),
      ClassPathDocumentDetail("b.pdf", "/document/revocation-order.pdf")
    )

    val pdfResult = pdfDocumentGenerator.mergePdfs(details)

    StepVerifier
      .create(pdfResult)
      .assertNext {
        assertThat(String(it), equalTo(stubResponseAsString))
      }
      .verifyComplete()
  }
}
