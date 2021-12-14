package uk.gov.justice.digital.hmpps.managerecallsapi.documents.recallnotification

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
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
  fun `get recall notification returns recall summary, revocation order and letter to probation when one doesnt exist already`() {
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

    every { recallNotificationContext.getRevocationOrderContext() } returns revocationOrderContext
    every { documentService.getLatestVersionedDocumentContentWithCategoryIfExists(recallId, RECALL_NOTIFICATION) } returns null
    every { recallNotificationContextFactory.createContext(recallId, createdByUserId) } returns recallNotificationContext
    every { letterToProbationService.createPdf(recallNotificationContext) } returns Mono.just(letterToProbationContent.toByteArray())
    every { recallSummaryService.createPdf(recallNotificationContext) } returns Mono.just(recallSummaryContent.toByteArray())
    every { revocationOrderService.getOrCreatePdf(revocationOrderContext, createdByUserId) } returns Mono.just(revocationOrderContent.toByteArray())

    every { pdfDocumentGenerationService.mergePdfs(capture(documentsToMergeSlot)) } returns Mono.just(mergedBytes)
    every {
      documentService.storeDocument(
        recallId,
        createdByUserId,
        mergedBytes,
        RECALL_NOTIFICATION,
        "$RECALL_NOTIFICATION.pdf"
      )
    } returns documentId

    val recallNotification = underTest.getOrCreatePdf(recallId, createdByUserId).block()!!

    assertThat(recallNotification, equalTo(mergedBytes))
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

  @Test
  fun `create recall notification uses latest revocation order, or creates if not exists, and creates recall summary and letter to probation`() {
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

    every { recallNotificationContext.getRevocationOrderContext() } returns revocationOrderContext
    every { recallNotificationContextFactory.createContext(recallId, createdByUserId) } returns recallNotificationContext
    every { letterToProbationService.createPdf(recallNotificationContext) } returns Mono.just(letterToProbationContent.toByteArray())
    every { recallSummaryService.createPdf(recallNotificationContext) } returns Mono.just(recallSummaryContent.toByteArray())
    every { revocationOrderService.getOrCreatePdf(revocationOrderContext, createdByUserId) } returns Mono.just(revocationOrderContent.toByteArray())

    every { pdfDocumentGenerationService.mergePdfs(capture(documentsToMergeSlot)) } returns Mono.just(mergedBytes)
    every {
      documentService.storeDocument(
        recallId,
        createdByUserId,
        mergedBytes,
        RECALL_NOTIFICATION,
        "$RECALL_NOTIFICATION.pdf"
      )
    } returns documentId

    val (createdDocumentId, recallNotification) = underTest.createAndStorePdf(recallId, createdByUserId, "Changed a value so recreating").block()!!

    verify(exactly = 0) { documentService.getLatestVersionedDocumentContentWithCategoryIfExists(recallId, RECALL_NOTIFICATION) }

    assertThat(recallNotification, equalTo(mergedBytes))
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

  @Test
  fun `get recall notification returns existing recall notification`() {
    val recallId = ::RecallId.random()
    val recallNotificationBytes = randomString().toByteArray()
    val userId = UserId(UUID.randomUUID())

    every { documentService.getLatestVersionedDocumentContentWithCategoryIfExists(recallId, RECALL_NOTIFICATION) } returns recallNotificationBytes

    val result = underTest.getOrCreatePdf(recallId, userId)

    verify { recallSummaryService wasNot Called }
    verify { revocationOrderService wasNot Called }
    verify { letterToProbationService wasNot Called }
    verify { pdfDocumentGenerationService wasNot Called }

    StepVerifier.create(result).assertNext { assertThat(it, equalTo(recallNotificationBytes)) }.verifyComplete()
  }
}
