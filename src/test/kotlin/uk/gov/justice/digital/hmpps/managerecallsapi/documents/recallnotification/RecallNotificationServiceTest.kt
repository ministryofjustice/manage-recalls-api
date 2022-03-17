package uk.gov.justice.digital.hmpps.managerecallsapi.documents.recallnotification

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.RECALL_NOTIFICATION
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ByteArrayDocumentData
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.byteArrayDocumentDataFor
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FileName
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
  private val recallNotificationLetterToProbationService = mockk<RecallNotificationLetterToProbationService>()
  private val offenderNotificationService = mockk<OffenderNotificationService>()
  private val pdfDocumentGenerationService = mockk<PdfDocumentGenerationService>()
  private val documentService = mockk<DocumentService>()

  private val underTest = RecallNotificationService(
    recallNotificationContextFactory,
    revocationOrderService,
    recallSummaryService,
    recallNotificationLetterToProbationService,
    offenderNotificationService,
    pdfDocumentGenerationService,
    documentService
  )

  @Test
  fun `generate recall notification calls generate for recall summary and letter to probation and getOrGenerate for revocation order for in custody`() {
    val recallId = ::RecallId.random()
    val recallSummaryContent = randomString()
    val revocationOrderContent = randomString()
    val letterToProbationContent = randomString()
    val mergedBytes = randomString().toByteArray()
    val documentId = ::DocumentId.random()
    val createdByUserId = UserId(UUID.randomUUID())
    val documentsToMergeSlot = slot<List<ByteArrayDocumentData>>()
    val recallNotificationContext = mockk<RecallNotificationContext>()
    val recallSummaryContext = mockk<RecallSummaryContext>()
    val letterToProbationContext = mockk<LetterToProbationContext>()
    val revocationOrderContext = mockk<RevocationOrderContext>()
    val details = "Changed a value so recreating"
    val recall = mockk<Recall>()
    val fileName = FileName("RECALL_NOTIFICATION.pdf")

    every { recallNotificationContext.getRevocationOrderContext() } returns revocationOrderContext
    every { recallNotificationContext.getLetterToProbationContext() } returns letterToProbationContext
    every { recallNotificationContext.getRecallSummaryContext() } returns recallSummaryContext
    every { recallNotificationContext.recall } returns recall
    every { recall.inCustodyRecall() } returns true
    every { recallNotificationContextFactory.createContext(recallId, createdByUserId) } returns recallNotificationContext
    every { recallNotificationLetterToProbationService.generatePdf(letterToProbationContext) } returns Mono.just(letterToProbationContent.toByteArray())
    every { recallSummaryService.generatePdf(recallSummaryContext) } returns Mono.just(recallSummaryContent.toByteArray())
    every { revocationOrderService.getOrGeneratePdf(revocationOrderContext) } returns Mono.just(revocationOrderContent.toByteArray())

    every { pdfDocumentGenerationService.mergePdfs(capture(documentsToMergeSlot)) } returns Mono.just(mergedBytes)
    every {
      documentService.storeDocument(
        recallId,
        createdByUserId,
        mergedBytes,
        RECALL_NOTIFICATION,
        fileName,
        details
      )
    } returns documentId

    val createdDocumentId = underTest.generateAndStorePdf(
      recallId,
      createdByUserId,
      fileName,
      details
    ).block()!!

    verify(exactly = 0) { documentService.getLatestVersionedDocumentContentWithCategoryIfExists(recallId, RECALL_NOTIFICATION) }
    verify(exactly = 0) { recallNotificationContext.getOffenderNotificationContext() }
    verify { offenderNotificationService wasNot Called }

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
  fun `generate recall notification calls generate for recall summary and letter to probation and getOrGenerate for revocation order and 2 extra docs for NOT in custody`() {
    val recallId = ::RecallId.random()
    val recallSummaryContent = randomString()
    val revocationOrderContent = randomString()
    val letterToProbationContent = randomString()
    val offenderNotificationContent = randomString()
    val policeNotificationContent = ClassPathResource("/pdfs/Police-Notification.pdf").inputStream.readAllBytes()
    val mergedBytes = randomString().toByteArray()
    val documentId = ::DocumentId.random()
    val createdByUserId = UserId(UUID.randomUUID())
    val documentsToMergeSlot = slot<List<ByteArrayDocumentData>>()
    val recallNotificationContext = mockk<RecallNotificationContext>()
    val recallSummaryContext = mockk<RecallSummaryContext>()
    val letterToProbationContext = mockk<LetterToProbationContext>()
    val revocationOrderContext = mockk<RevocationOrderContext>()
    val offenderNotificationContext = mockk<OffenderNotificationContext>()
    val details = "Changed a value so recreating"
    val recall = mockk<Recall>()
    val fileName = FileName("RECALL_NOTIFICATION.pdf")

    every { recallNotificationContext.getRevocationOrderContext() } returns revocationOrderContext
    every { recallNotificationContext.getLetterToProbationContext() } returns letterToProbationContext
    every { recallNotificationContext.getRecallSummaryContext() } returns recallSummaryContext
    every { recallNotificationContext.getOffenderNotificationContext() } returns offenderNotificationContext
    every { recallNotificationContext.recall } returns recall
    every { recall.inCustodyRecall() } returns false
    every { recallNotificationContextFactory.createContext(recallId, createdByUserId) } returns recallNotificationContext
    every { recallNotificationLetterToProbationService.generatePdf(letterToProbationContext) } returns Mono.just(letterToProbationContent.toByteArray())
    every { offenderNotificationService.generatePdf(offenderNotificationContext) } returns Mono.just(offenderNotificationContent.toByteArray())
    every { recallSummaryService.generatePdf(recallSummaryContext) } returns Mono.just(recallSummaryContent.toByteArray())
    every { revocationOrderService.getOrGeneratePdf(revocationOrderContext) } returns Mono.just(revocationOrderContent.toByteArray())

    every { pdfDocumentGenerationService.mergePdfs(capture(documentsToMergeSlot)) } returns Mono.just(mergedBytes)
    every {
      documentService.storeDocument(
        recallId,
        createdByUserId,
        mergedBytes,
        RECALL_NOTIFICATION,
        fileName,
        details
      )
    } returns documentId

    val createdDocumentId = underTest.generateAndStorePdf(
      recallId,
      createdByUserId,
      fileName,
      details
    ).block()!!

    verify(exactly = 0) { documentService.getLatestVersionedDocumentContentWithCategoryIfExists(recallId, RECALL_NOTIFICATION) }

    assertThat(createdDocumentId, equalTo(documentId))
    assertThat(
      documentsToMergeSlot.captured,
      onlyContainsInOrder(
        listOf(
          byteArrayDocumentDataFor(recallSummaryContent),
          byteArrayDocumentDataFor(revocationOrderContent),
          byteArrayDocumentDataFor(offenderNotificationContent),
          byteArrayDocumentDataFor(policeNotificationContent),
          byteArrayDocumentDataFor(letterToProbationContent)
        )
      )
    )
  }
}
