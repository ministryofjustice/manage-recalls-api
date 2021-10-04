package uk.gov.justice.digital.hmpps.managerecallsapi.service

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ImageData.Companion.recallImage
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallImage.HmppsLogo

class LetterToProbationServiceTest {
  private val pdfDocumentGenerationService = mockk<PdfDocumentGenerationService>()
  private val letterToProbationGenerator = mockk<LetterToProbationGenerator>()

  private val underTest = LetterToProbationService(
    letterToProbationGenerator,
    pdfDocumentGenerationService
  )

  @Test
  fun `create letter to probation generates html and creates the PDF`() {
    val recallNotificationContext = mockk<RecallNotificationContext>()
    val letterToProbationContext = mockk<LetterToProbationContext>()
    val generatedHtml = "some html"
    val pdfBytes = "pdf".toByteArray()

    every { recallNotificationContext.getLetterToProbationContext() } returns letterToProbationContext
    every { letterToProbationGenerator.generateHtml(letterToProbationContext) } returns generatedHtml
    every { pdfDocumentGenerationService.generatePdf(generatedHtml, recallImage(HmppsLogo)) } returns Mono.just(pdfBytes)

    val result = underTest.createPdf(recallNotificationContext).block()!!

    assertArrayEquals(result, pdfBytes)
  }
}
