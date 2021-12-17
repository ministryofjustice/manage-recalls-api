package uk.gov.justice.digital.hmpps.managerecallsapi.documents.lettertoprison

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.LETTER_TO_PRISON
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDecorator
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallLengthDescription
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
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
    every {
      documentService.getLatestVersionedDocumentContentWithCategoryIfExists(
        recallId,
        LETTER_TO_PRISON
      )
    } returns expectedBytes
    val createdByUserId = ::UserId.random()

    val result = underTest.getOrGeneratePdf(recallId, createdByUserId)

    verify { recallRepository wasNot Called }
    verify { letterToPrisonContextFactory wasNot Called }
    verify { letterToPrisonCustodyOfficeGenerator wasNot Called }
    verify { letterToPrisonGovernorGenerator wasNot Called }
    verify { letterToPrisonConfirmationGenerator wasNot Called }
    verify { pdfDocumentGenerationService wasNot Called }
    verify { pdfDocumentGenerationService wasNot Called }
    verify { pdfDecorator wasNot Called }

    StepVerifier
      .create(result)
      .assertNext {
        assertThat(it, equalTo(expectedBytes))
      }.verifyComplete()
  }

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

    every { letterToPrisonContextFactory.createContext(recallId, currentUserId) } returns context
    every { letterToPrisonConfirmationGenerator.generateHtml(context) } returns ltpConfirmationHtml
    every { pdfDocumentGenerationService.generatePdf(ltpConfirmationHtml) } returns Mono.just("Some Confirmation bytes".toByteArray())
    every { letterToPrisonGovernorGenerator.generateHtml(context) } returns ltpGovernorHtml
    every { pdfDocumentGenerationService.generatePdf(ltpGovernorHtml, 1.0, 1.0, any()) } returns Mono.just("Some Governor bytes".toByteArray())
    every { letterToPrisonCustodyOfficeGenerator.generateHtml(context) } returns ltpCustodyOfficeHtml
    every { pdfDocumentGenerationService.generatePdf(ltpCustodyOfficeHtml, 1.0, 1.0, any()) } returns Mono.just(
      custodyOfficeBytes
    )
    every { pdfDocumentGenerationService.mergePdfs(any()) } returns Mono.just(mergedBytes)
    every { context.recallLengthDescription } returns RecallLengthDescription(RecallLength.FOURTEEN_DAYS)
    every { pdfDecorator.numberPagesOnRightWithHeaderAndFooter(mergedBytes, any(), any(), any(), any(), "OFFICIAL", any()) } returns letterBytes
    every { documentService.storeDocument(recallId, currentUserId, letterBytes, LETTER_TO_PRISON, "LETTER_TO_PRISON.pdf", "New Version") } returns documentId

    val result = underTest.generateAndStorePdf(recallId, currentUserId, "New Version")

    StepVerifier
      .create(result)
      .assertNext {
        assertThat(it, equalTo(documentId))
      }.verifyComplete()
  }
}
