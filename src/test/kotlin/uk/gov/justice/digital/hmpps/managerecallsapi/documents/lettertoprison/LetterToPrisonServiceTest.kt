package uk.gov.justice.digital.hmpps.managerecallsapi.documents.lettertoprison

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import reactor.test.StepVerifier
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.LETTER_TO_PRISON
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDecorator
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomString
import uk.gov.justice.digital.hmpps.managerecallsapi.service.DocumentService

@Suppress("ReactiveStreamsUnusedPublisher")
internal class LetterToPrisonServiceTest {

  private val pdfDocumentGenerationService = mockk<PdfDocumentGenerationService>()
  private val letterToPrisonContextFactory = mockk<LetterToPrisonContextFactory>()
  private val documentService = mockk<DocumentService>()
  private val recallRepository = mockk<RecallRepository>()
  private val letterToPrisonCustodyOfficeGenerator = mockk<LetterToPrisonCustodyOfficeGenerator>()
  private val letterToPrisonGovernorGenerator = mockk<LetterToPrisonGovernorGenerator>()
  private val letterToPrisonConfirmationGenerator = mockk<LetterToPrisonConfirmationGenerator>()
  private val pdfDecorator = mockk<PdfDecorator>()

  private val underTest = LetterToPrisonService(
    documentService,
    letterToPrisonContextFactory,
    letterToPrisonCustodyOfficeGenerator,
    letterToPrisonGovernorGenerator,
    letterToPrisonConfirmationGenerator,
    pdfDocumentGenerationService,
    pdfDecorator
  )

  private val recallId = ::RecallId.random()
  private val expectedBytes = randomString().toByteArray()

  @Test
  fun `returns a letter to prison for a recall if one exists already`() {
    every { documentService.getLatestVersionedDocumentContentWithCategoryIfExists(recallId, LETTER_TO_PRISON) } returns expectedBytes

    val result = underTest.getPdf(recallId)

    verify(exactly = 0) { recallRepository.getByRecallId(any()) }
    verify(exactly = 0) { letterToPrisonContextFactory.createContext(any()) }
    verify(exactly = 0) { letterToPrisonCustodyOfficeGenerator.generateHtml(any()) }
    verify(exactly = 0) { letterToPrisonGovernorGenerator.generateHtml(any()) }
    verify(exactly = 0) { letterToPrisonConfirmationGenerator.generateHtml(any()) }
    verify(exactly = 0) { pdfDocumentGenerationService.generatePdf(any()) }
    verify(exactly = 0) { pdfDocumentGenerationService.mergePdfs(any()) }
    verify(exactly = 0) { pdfDecorator.numberPagesOnRightWithHeaderAndFooter(any(), any(), any(), any(), any(), any(), any()) }

    StepVerifier
      .create(result)
      .assertNext {
        assertThat(it, equalTo(expectedBytes))
      }.verifyComplete()
  }
}
