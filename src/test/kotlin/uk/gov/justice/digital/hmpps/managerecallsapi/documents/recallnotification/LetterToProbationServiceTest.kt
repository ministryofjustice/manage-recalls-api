package uk.gov.justice.digital.hmpps.managerecallsapi.documents.recallnotification

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ImageData.Companion.recallImage
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallImage.HmppsLogo

class LetterToProbationServiceTest {
  private val pdfDocumentGenerationService = mockk<PdfDocumentGenerationService>()
  private val letterToProbationGenerator = mockk<LetterToProbationGenerator>()

  private val underTest = LetterToProbationService(
    letterToProbationGenerator,
    pdfDocumentGenerationService
  )

  @Test
  fun `create letter to probation generates html and creates the PDF`() {
    val letterToProbationContext = mockk<LetterToProbationContext>()
    val generatedHtml = "some html"
    val pdfBytes = "pdf".toByteArray()

    every { letterToProbationGenerator.generateHtml(letterToProbationContext) } returns generatedHtml
    every { pdfDocumentGenerationService.generatePdf(generatedHtml, recallImage(HmppsLogo)) } returns Mono.just(pdfBytes)

    val result = underTest.generatePdf(letterToProbationContext).block()!!

    assertArrayEquals(result, pdfBytes)
  }
}
