package uk.gov.justice.digital.hmpps.managerecallsapi.documents.lettertoprison

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.LETTER_TO_PRISON
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDecorator
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallLengthDescription
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FileName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.service.DocumentService

@Suppress("ReactiveStreamsUnusedPublisher")
internal class LetterToPrisonServiceTest {

  private val pdfDocumentGenerationService = mockk<PdfDocumentGenerationService>()
  private val letterToPrisonContextFactory = mockk<LetterToPrisonContextFactory>()
  private val documentService = mockk<DocumentService>()
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

  @Test
  fun `generates a letter to prison`() {
    val currentUserId = ::UserId.random()
    val context = mockk<LetterToPrisonContext>()
    val ltpConfirmationHtml = "Some Confirmation Html"
    val ltpGovernorHtml = "Some Governor Html"
    val ltpCustodyOfficeHtml = "Some Custody Office Html"
    val mergedBytes = "Some merged bytes".toByteArray()
    val letterBytes = "letter bytes".toByteArray()
    val documentId = ::DocumentId.random()
    val custodyOfficeBytes = ClassPathResource("/document/3_pages_unnumbered.pdf").file.readBytes()
    val fileName = FileName("LETTER_TO_PRISON.pdf")

    every { letterToPrisonContextFactory.createContext(recallId, currentUserId) } returns context
    every { letterToPrisonConfirmationGenerator.generateHtml(context) } returns ltpConfirmationHtml
    every { pdfDocumentGenerationService.generatePdf(ltpConfirmationHtml) } returns Mono.just("Some Confirmation bytes".toByteArray())
    every { letterToPrisonGovernorGenerator.generateHtml(context) } returns ltpGovernorHtml
    every { pdfDocumentGenerationService.generatePdf(ltpGovernorHtml, 1.0, 0.8, any()) } returns Mono.just("Some Governor bytes".toByteArray())
    every { letterToPrisonCustodyOfficeGenerator.generateHtml(context) } returns ltpCustodyOfficeHtml
    every { pdfDocumentGenerationService.generatePdf(ltpCustodyOfficeHtml, 1.0, 0.8, any()) } returns Mono.just(
      custodyOfficeBytes
    )
    every { pdfDocumentGenerationService.mergePdfs(any()) } returns Mono.just(mergedBytes)
    every { context.recallLengthDescription } returns RecallLengthDescription(RecallLength.FOURTEEN_DAYS)
    every { pdfDecorator.numberPagesOnRightWithHeaderAndFooter(mergedBytes, any(), any(), any(), any(), "OFFICIAL", any()) } returns letterBytes
    every { documentService.storeDocument(recallId, currentUserId, letterBytes, LETTER_TO_PRISON, fileName, "New Version") } returns documentId

    val result = underTest.generateAndStorePdf(recallId, currentUserId, fileName, "New Version")

    StepVerifier
      .create(result)
      .assertNext {
        assertThat(it, equalTo(documentId))
      }.verifyComplete()
  }
}
