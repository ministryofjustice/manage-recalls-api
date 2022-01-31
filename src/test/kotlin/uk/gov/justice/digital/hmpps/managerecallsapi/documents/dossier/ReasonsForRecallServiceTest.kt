package uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.REASONS_FOR_RECALL
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDecorator
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CroNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomNoms
import uk.gov.justice.digital.hmpps.managerecallsapi.service.DocumentService
import java.time.LocalDate
import java.time.OffsetDateTime

class ReasonsForRecallServiceTest {
  private val dossierContextFactory = mockk<DossierContextFactory>()
  private val pdfDocumentGenerationService = mockk<PdfDocumentGenerationService>()
  private val reasonsForRecallGenerator = mockk<ReasonsForRecallGenerator>()
  private val pdfDecorator = mockk<PdfDecorator>()
  private val documentService = mockk<DocumentService>()

  private val underTest = ReasonsForRecallService(dossierContextFactory, reasonsForRecallGenerator, pdfDocumentGenerationService, pdfDecorator, documentService)

  @Test
  fun `getOrGeneratePdf generates and stores the document when one does not exist`() {
    val dossierContext = mockk<DossierContext>()
    val reasonsForRecallContext = mockk<ReasonsForRecallContext>()
    val generatedHtml = "some html"
    val expectedPdfWithHeaderBytes = "some other bytes".toByteArray()
    val expectedPdfBytes = "some bytes".toByteArray()
    val recallId = ::RecallId.random()
    val createdByUserId = ::UserId.random()

    every { documentService.getLatestVersionedDocumentContentWithCategoryIfExists(any(), REASONS_FOR_RECALL) } returns null
    every { dossierContext.getReasonsForRecallContext() } returns reasonsForRecallContext
    every { dossierContext.recall } returns Recall(
      recallId,
      randomNoms(),
      ::UserId.random(),
      OffsetDateTime.now(),
      FirstName("Barrie"),
      null,
      LastName("Badger"),
      CroNumber("ABC/1234A"),
      LocalDate.of(1999, 12, 1)
    )
    every { reasonsForRecallGenerator.generateHtml(reasonsForRecallContext) } returns generatedHtml
    every { pdfDocumentGenerationService.generatePdf(generatedHtml, 1.0, 1.0) } returns Mono.just(expectedPdfBytes)
    every { pdfDecorator.centralHeader(expectedPdfBytes, "OFFICIAL") } returns expectedPdfWithHeaderBytes
    every {
      documentService.storeDocument(
        recallId,
        createdByUserId,
        expectedPdfWithHeaderBytes,
        REASONS_FOR_RECALL,
        "REASONS_FOR_RECALL.pdf"
      )
    } returns ::DocumentId.random()

    val generatedPdf = underTest.getOrGeneratePdf(dossierContext, createdByUserId).block()!!

    assertArrayEquals(expectedPdfWithHeaderBytes, generatedPdf)
  }

  @Test
  fun `getOrGeneratePdf returns existing document when one already exists`() {
    val dossierContext = mockk<DossierContext>()
    val recallId = ::RecallId.random()
    val expectedBytes = "Some expected content".toByteArray()
    val createdByUserId = ::UserId.random()

    every { documentService.getLatestVersionedDocumentContentWithCategoryIfExists(any(), REASONS_FOR_RECALL) } returns expectedBytes
    every { dossierContext.recall } returns Recall(
      recallId,
      randomNoms(),
      ::UserId.random(),
      OffsetDateTime.now(),
      FirstName("Barrie"),
      null,
      LastName("Badger"),
      CroNumber("ABC/1234A"),
      LocalDate.of(1999, 12, 1)
    )

    val generatedPdf = underTest.getOrGeneratePdf(dossierContext, createdByUserId).block()!!

    assertArrayEquals(expectedBytes, generatedPdf)
    verify(exactly = 0) { documentService.storeDocument(any(), any(), any(), any(), any()) }
    verify { reasonsForRecallGenerator wasNot Called }
    verify { pdfDocumentGenerationService wasNot Called }
    verify { pdfDecorator wasNot Called }
  }

  @Test
  fun `generateAndStorePdf generates new document`() {
    val dossierContext = mockk<DossierContext>()
    val reasonsForRecallContext = mockk<ReasonsForRecallContext>()
    val recallId = ::RecallId.random()
    val generatedHtml = "Some expected content"
    val expectedPdfBytes = generatedHtml.toByteArray()
    val expectedPdfWithHeaderBytes = "some other bytes".toByteArray()
    val createdByUserId = ::UserId.random()
    val details = "Blah blah blah"
    val documentId = ::DocumentId.random()

    every { dossierContextFactory.createContext(recallId) } returns dossierContext
    every { dossierContext.getReasonsForRecallContext() } returns reasonsForRecallContext
    every { reasonsForRecallGenerator.generateHtml(reasonsForRecallContext) } returns generatedHtml
    every { pdfDocumentGenerationService.generatePdf(generatedHtml, 1.0, 1.0) } returns Mono.just(expectedPdfBytes)
    every { pdfDecorator.centralHeader(expectedPdfBytes, "OFFICIAL") } returns expectedPdfWithHeaderBytes
    every {
      documentService.storeDocument(
        recallId,
        createdByUserId,
        expectedPdfWithHeaderBytes,
        REASONS_FOR_RECALL,
        "REASONS_FOR_RECALL.pdf",
        details
      )
    } returns documentId

    val result = underTest.generateAndStorePdf(recallId, createdByUserId, details).block()!!

    assertThat(result, equalTo(documentId))
    verify(exactly = 0) { documentService.getLatestVersionedDocumentContentWithCategoryIfExists(recallId, REASONS_FOR_RECALL) }
  }
}
