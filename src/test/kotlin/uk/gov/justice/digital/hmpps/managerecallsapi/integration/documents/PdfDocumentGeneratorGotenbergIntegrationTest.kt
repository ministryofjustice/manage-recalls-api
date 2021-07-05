package uk.gov.justice.digital.hmpps.managerecallsapi.integration.documents

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import reactor.test.StepVerifier
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ClassPathDocumentDetail
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.HtmlDocumentDetail
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerator
import uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers.GotenbergMockServer

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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
  fun `should return byte array when requesting pdf`() {
    gotenbergMockServer.stubPdfGeneration("test".toByteArray(), "<<FIRST_NAMES>>")

    val details = listOf(
      HtmlDocumentDetail("index.html", "<body><span><<FIRST_NAMES>></span></body>"),
      ClassPathDocumentDetail("logo.png", "/document/template/revocation-order/logo.png")
    )

    val makePdfResult = pdfDocumentGenerator.makePdf(details)

    StepVerifier
      .create(makePdfResult)
      .assertNext {
        assertThat(String(it), equalTo("test"))
      }
      .verifyComplete()
  }
}
