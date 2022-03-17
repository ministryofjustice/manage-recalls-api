package uk.gov.justice.digital.hmpps.managerecallsapi.documents.returnedtocustodylettertoprobation

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import uk.gov.justice.digital.hmpps.managerecallsapi.config.ReturnedToCustodyRecallExpectedException
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.LETTER_TO_PROBATION
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.ReturnedToCustodyRecord
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ImageData.Companion.recallImage
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDecorator
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallDescription
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallImage
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FileName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.service.DocumentService
import java.time.OffsetDateTime

internal class ReturnedToCustodyLetterToProbationServiceTest {

  private val pdfDocumentGenerationService = mockk<PdfDocumentGenerationService>()
  private val returnedToCustodyLetterToProbationContextFactory = mockk<ReturnedToCustodyLetterToProbationContextFactory>()
  private val documentService = mockk<DocumentService>()
  private val returnedToCustodyLetterToProbationGenerator = mockk<ReturnedToCustodyLetterToProbationGenerator>()
  private val pdfDecorator = mockk<PdfDecorator>()
  private val recallRepository = mockk<RecallRepository>()

  private val underTest = ReturnedToCustodyLetterToProbationService(
    documentService,
    returnedToCustodyLetterToProbationContextFactory,
    returnedToCustodyLetterToProbationGenerator,
    pdfDocumentGenerationService,
    pdfDecorator,
    recallRepository
  )

  private val recallId = ::RecallId.random()
  private val recall = mockk<Recall>()

  @Test
  fun `generates a returned to custody letter to probation`() {
    val currentUserId = ::UserId.random()
    val context = mockk<ReturnedToCustodyLetterToProbationContext>()
    val pdfContent = "Some Html"
    val pdfBytes = "Some bytes".toByteArray()
    val letterBytes = "letter bytes".toByteArray()
    val documentId = ::DocumentId.random()
    val fileName = FileName("BADGER BARRIE LETTER TO PROBATION.pdf")
    val now = OffsetDateTime.now()

    every { recallRepository.getByRecallId(recallId) } returns recall
    every { recall.returnedToCustody } returns ReturnedToCustodyRecord(now, now, currentUserId, now)
    every { returnedToCustodyLetterToProbationContextFactory.createContext(recall, currentUserId) } returns context
    every { returnedToCustodyLetterToProbationGenerator.generateHtml(context) } returns pdfContent
    every { pdfDocumentGenerationService.generatePdf(pdfContent, 1.0, 1.0, recallImage(RecallImage.HmppsLogo)) } returns Mono.just(pdfBytes)

    every { context.recallDescription } returns RecallDescription(RecallType.FIXED, RecallLength.FOURTEEN_DAYS)
    every { pdfDecorator.numberPagesOnRightWithHeaderAndFooter(pdfBytes, any(), any(), any(), any(), "OFFICIAL", any()) } returns letterBytes
    every { documentService.storeDocument(recallId, currentUserId, letterBytes, LETTER_TO_PROBATION, fileName, "New Version") } returns documentId

    val result = underTest.generateAndStorePdf(recallId, currentUserId, fileName, "New Version")

    StepVerifier
      .create(result)
      .assertNext {
        assertThat(it, equalTo(documentId))
      }.verifyComplete()
  }

  @Test
  fun `generates a returned to custody letter to probation for an in custody recall throws an exception`() {
    val currentUserId = ::UserId.random()
    val fileName = FileName("BADGER BARRIE LETTER TO PROBATION.pdf")

    every { recallRepository.getByRecallId(recallId) } returns recall
    every { recall.returnedToCustody } returns null

    assertThrows<ReturnedToCustodyRecallExpectedException> {
      underTest.generateAndStorePdf(recallId, currentUserId, fileName, "New Version")
    }

    verify { returnedToCustodyLetterToProbationContextFactory wasNot Called }
    verify { returnedToCustodyLetterToProbationGenerator wasNot Called }
    verify { pdfDocumentGenerationService wasNot Called }
    verify { pdfDecorator wasNot Called }
    verify { documentService wasNot Called }
  }
}
