package uk.gov.justice.digital.hmpps.managerecallsapi.integration

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerator
import java.io.File

class RealPdfDocumentGeneratorTest {

  private val pdfDocumentGenerator = PdfDocumentGenerator(
    WebClient.builder().build(),
    "http://localhost:9991"
  )

  @Disabled
  @Test
  fun `should return byte array when requesting pdf`() {
    val makePdfResult = pdfDocumentGenerator.makePdf()

    val expected = File("expected.pdf")

    assertThat(makePdfResult.contentEquals(expected.readBytes()), equalTo(true))
  }
}
