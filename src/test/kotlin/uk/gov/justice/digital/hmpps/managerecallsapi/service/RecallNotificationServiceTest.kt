package uk.gov.justice.digital.hmpps.managerecallsapi.service

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.RECALL_NOTIFICATION
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ByteArrayDocumentData
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.byteArrayDocumentDataFor
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.matchers.onlyContainsInOrder
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomString
import java.util.UUID

@Suppress("ReactiveStreamsUnusedPublisher")
internal class RecallNotificationServiceTest {

  private val revocationOrderService = mockk<RevocationOrderService>()
  private val recallSummaryService = mockk<RecallSummaryService>()
  private val letterToProbationService = mockk<LetterToProbationService>()
  private val pdfDocumentGenerationService = mockk<PdfDocumentGenerationService>()
  private val recallDocumentService = mockk<RecallDocumentService>()

  private val underTest = RecallNotificationService(
    revocationOrderService,
    recallSummaryService,
    letterToProbationService,
    pdfDocumentGenerationService,
    recallDocumentService
  )

  @Test
  fun `get recall notification returns recall summary, revocation order and letter to probation when one doesnt exist already`() {
    val recallId = ::RecallId.random()
    val recallSummaryContent = randomString()
    val revocationOrderContent = randomString()
    val letterToProbationContent = randomString()
    val mergedBytes = randomString().toByteArray()
    val documentId = UUID.randomUUID()
    val userId = UserId(UUID.randomUUID())
    val documentsToMergeSlot = slot<List<ByteArrayDocumentData>>()

    every { recallDocumentService.getDocumentContentWithCategoryIfExists(recallId, RECALL_NOTIFICATION) } returns null
    every { recallSummaryService.getPdf(recallId, userId) } returns Mono.just(recallSummaryContent.toByteArray())
    every { revocationOrderService.createPdf(recallId, userId) } returns Mono.just(revocationOrderContent.toByteArray())
    every { letterToProbationService.getPdf(recallId, userId) } returns Mono.just(letterToProbationContent.toByteArray())
    every { pdfDocumentGenerationService.mergePdfs(capture(documentsToMergeSlot)) } returns Mono.just(mergedBytes)
    every { recallDocumentService.uploadAndAddDocumentForRecall(recallId, mergedBytes, RECALL_NOTIFICATION) } returns documentId

    val recallNotification = underTest.getDocument(recallId, userId).block()!!

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
  fun `get recall notification returns existing recall notification`() {
    val recallId = ::RecallId.random()
    val recallNotificationBytes = randomString().toByteArray()
    val userId = UserId(UUID.randomUUID())

    every { recallDocumentService.getDocumentContentWithCategoryIfExists(recallId, RECALL_NOTIFICATION) } returns recallNotificationBytes

    val result = underTest.getDocument(recallId, userId)

    verify(exactly = 0) { recallSummaryService.getPdf(recallId, userId) }
    verify(exactly = 0) { revocationOrderService.createPdf(recallId, userId) }
    verify(exactly = 0) { letterToProbationService.getPdf(recallId, userId) }
    verify(exactly = 0) { pdfDocumentGenerationService.mergePdfs(any()) }

    StepVerifier.create(result).assertNext { assertThat(it, equalTo(recallNotificationBytes)) }.verifyComplete()
  }
}
