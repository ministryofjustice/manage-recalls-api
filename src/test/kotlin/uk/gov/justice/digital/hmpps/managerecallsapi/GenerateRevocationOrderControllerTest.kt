package uk.gov.justice.digital.hmpps.managerecallsapi

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerator

internal class GenerateRevocationOrderControllerTest {

  private val pdfDocumentGenerator = mockk<PdfDocumentGenerator>()

  private val underTest = GenerateRevocationOrderController(pdfDocumentGenerator)

  @Test
  fun `generates a revocation order using the pdf generator`() {
    val expectedPdf = "Some pdf".toByteArray()
    every { pdfDocumentGenerator.makePdf() } returns expectedPdf

    val result = underTest.generateRevocationOrder()

    assertThat(result.body.content.contentEquals(expectedPdf), equalTo(true))
  }
}
