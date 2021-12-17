package uk.gov.justice.digital.hmpps.managerecallsapi.documents.recallnotification

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.RECALL_NOTIFICATION
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ByteArrayDocumentData
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.byteArrayDocumentDataFor
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.matchers.onlyContainsInOrder
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomString
import uk.gov.justice.digital.hmpps.managerecallsapi.service.DocumentService
import java.util.UUID

@Suppress("ReactiveStreamsUnusedPublisher")
internal class RecallNotificationServiceTest {

  private val recallNotificationContextFactory = mockk<RecallNotificationContextFactory>()
  private val revocationOrderService = mockk<RevocationOrderService>()
  private val recallSummaryService = mockk<RecallSummaryService>()
  private val letterToProbationService = mockk<LetterToProbationService>()
  private val pdfDocumentGenerationService = mockk<PdfDocumentGenerationService>()
  private val documentService = mockk<DocumentService>()

  private val underTest = RecallNotificationService(
    recallNotificationContextFactory,
    revocationOrderService,
    recallSummaryService,
    letterToProbationService,
    pdfDocumentGenerationService,
    documentService
  )

  @Test
  fun `generate recall notification calls generate for recall summary and letter to probation and getOrGenerate for revocation order`() {
    val recallId = ::RecallId.random()
    val recallSummaryContent = randomString()
    val revocationOrderContent = randomString()
    val letterToProbationContent = randomString()
    val mergedBytes = randomString().toByteArray()
    val documentId = ::DocumentId.random()
    val createdByUserId = UserId(UUID.randomUUID())
    val documentsToMergeSlot = slot<List<ByteArrayDocumentData>>()
    val recallNotificationContext = mockk<RecallNotificationContext>()
    val revocationOrderContext = mockk<RevocationOrderContext>()
    val details = "Changed a value so recreating"

    every { recallNotificationContext.getRevocationOrderContext() } returns revocationOrderContext
    every { recallNotificationContextFactory.createContext(recallId, createdByUserId) } returns recallNotificationContext
    every { letterToProbationService.generatePdf(recallNotificationContext) } returns Mono.just(letterToProbationContent.toByteArray())
    every { recallSummaryService.generatePdf(recallNotificationContext) } returns Mono.just(recallSummaryContent.toByteArray())
    every { revocationOrderService.getOrGeneratePdf(revocationOrderContext) } returns Mono.just(revocationOrderContent.toByteArray())

    every { pdfDocumentGenerationService.mergePdfs(capture(documentsToMergeSlot)) } returns Mono.just(mergedBytes)
    every {
      documentService.storeDocument(
        recallId,
        createdByUserId,
        mergedBytes,
        RECALL_NOTIFICATION,
        "RECALL_NOTIFICATION.pdf",
        details
      )
    } returns documentId

    val createdDocumentId = underTest.generateAndStorePdf(recallId, createdByUserId, details).block()!!

    verify(exactly = 0) { documentService.getLatestVersionedDocumentContentWithCategoryIfExists(recallId, RECALL_NOTIFICATION) }

    assertThat(createdDocumentId, equalTo(documentId))
    assertThat(
      documentsToMergeSlot.captured,
      onlyContainsInOrder(
        listOf(
          byteArrayDocumentDataFor(recallSummaryContent),
          byteArrayDocumentDataFor(revocationOrderContent),
          byteArrayDocumentDataFor(letterToProbationContent)
        )
      )
    )
  }
}
