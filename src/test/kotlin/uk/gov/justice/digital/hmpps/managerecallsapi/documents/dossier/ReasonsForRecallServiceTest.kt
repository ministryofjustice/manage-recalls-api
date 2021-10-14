package uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDecorator
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService

class ReasonsForRecallServiceTest {
  private val pdfDocumentGenerationService = mockk<PdfDocumentGenerationService>()
  private val reasonsForRecallGenerator = mockk<ReasonsForRecallGenerator>()
  private val pdfDecorator = mockk<PdfDecorator>()

  private val underTest = ReasonsForRecallService(reasonsForRecallGenerator, pdfDocumentGenerationService, pdfDecorator)

  @Test
  internal fun `createPdf generates the html and PDf`() {
    val dossierContext = mockk<DossierContext>()
    val reasonsForRecallContext = mockk<ReasonsForRecallContext>()
    val generatedHtml = "some html"
    val expectedPdfWithHeaderBytes = "some other bytes".toByteArray()
    val expectedPdfBytes = "some bytes".toByteArray()
    every { dossierContext.getReasonsForRecallContext() } returns reasonsForRecallContext
    every { reasonsForRecallGenerator.generateHtml(reasonsForRecallContext) } returns generatedHtml
    every { pdfDocumentGenerationService.generatePdf(generatedHtml, 1.0, 1.0) } returns Mono.just(expectedPdfBytes)
    every { pdfDecorator.centralHeader(expectedPdfBytes, "OFFICIAL") } returns expectedPdfWithHeaderBytes

    val generatedPdf = underTest.createPdf(dossierContext).block()!!

    assertArrayEquals(expectedPdfWithHeaderBytes, generatedPdf)
  }
}
