package uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.DOSSIER
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.REASONS_FOR_RECALL
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDecorator
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomNoms
import uk.gov.justice.digital.hmpps.managerecallsapi.service.DocumentService
import java.time.OffsetDateTime

class ReasonsForRecallServiceTest {
  private val pdfDocumentGenerationService = mockk<PdfDocumentGenerationService>()
  private val reasonsForRecallGenerator = mockk<ReasonsForRecallGenerator>()
  private val pdfDecorator = mockk<PdfDecorator>()
  private val documentService = mockk<DocumentService>()

  private val underTest = ReasonsForRecallService(reasonsForRecallGenerator, pdfDocumentGenerationService, pdfDecorator, documentService)

  @Test
  internal fun `getDocument generates and stores the html and PDf if one does not exist`() {
    val dossierContext = mockk<DossierContext>()
    val reasonsForRecallContext = mockk<ReasonsForRecallContext>()
    val generatedHtml = "some html"
    val expectedPdfWithHeaderBytes = "some other bytes".toByteArray()
    val expectedPdfBytes = "some bytes".toByteArray()
    val recallId = ::RecallId.random()

    every { documentService.getLatestVersionedDocumentContentWithCategoryIfExists(any(), DOSSIER) } returns null
    every { dossierContext.getReasonsForRecallContext() } returns reasonsForRecallContext
    every { dossierContext.recall } returns Recall(recallId, randomNoms(), ::UserId.random(), OffsetDateTime.now(), FirstName("Barrie"), null, LastName("Badger"))
    every { reasonsForRecallGenerator.generateHtml(reasonsForRecallContext) } returns generatedHtml
    every { pdfDocumentGenerationService.generatePdf(generatedHtml, 1.0, 1.0) } returns Mono.just(expectedPdfBytes)
    every { pdfDecorator.centralHeader(expectedPdfBytes, "OFFICIAL") } returns expectedPdfWithHeaderBytes
    every { documentService.storeDocument(recallId, expectedPdfWithHeaderBytes, REASONS_FOR_RECALL, "REASONS_FOR_RECALL.pdf") } returns ::DocumentId.random()

    val generatedPdf = underTest.getDocument(dossierContext).block()!!

    assertArrayEquals(expectedPdfWithHeaderBytes, generatedPdf)
  }

  @Test
  internal fun `getDocument returns existing PDf if one already exists`() {
    val dossierContext = mockk<DossierContext>()
    val recallId = ::RecallId.random()
    val expectedBytes = "Some expected content".toByteArray()

    every { documentService.getLatestVersionedDocumentContentWithCategoryIfExists(any(), DOSSIER) } returns expectedBytes
    every { dossierContext.recall } returns Recall(recallId, randomNoms(), ::UserId.random(), OffsetDateTime.now(), FirstName("Barrie"), null, LastName("Badger"))

    val generatedPdf = underTest.getDocument(dossierContext).block()!!

    assertArrayEquals(expectedBytes, generatedPdf)
    verify(exactly = 0) { documentService.storeDocument(any(), any(), any(), any()) }
    verify { reasonsForRecallGenerator wasNot Called }
    verify { pdfDocumentGenerationService wasNot Called }
    verify { pdfDecorator wasNot Called }
  }
}
