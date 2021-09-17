package uk.gov.justice.digital.hmpps.managerecallsapi.service

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerator
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomString
import java.util.UUID

@Suppress("ReactiveStreamsUnusedPublisher")
internal class RecallNotificationServiceTest {

  private val revocationOrderService = mockk<RevocationOrderService>()
  private val recallSummaryService = mockk<RecallSummaryService>()
  private val letterToProbationService = mockk<LetterToProbationService>()
  private val pdfDocumentGenerator = mockk<PdfDocumentGenerator>()
  private val recallDocumentService = mockk<RecallDocumentService>()

  private val underTest = RecallNotificationService(revocationOrderService, recallSummaryService, letterToProbationService, pdfDocumentGenerator, recallDocumentService)

  @Test
  fun `get recall notification returns recall summary, revocation order and letter to probation when one doesnt exist already`() {
    val recallId = ::RecallId.random()
    val recallSummaryBytes = randomString().toByteArray()
    val revocationOrderBytes = randomString().toByteArray()
    val letterToProbationBytes = randomString().toByteArray()
    val mergedBytes = randomString().toByteArray()
    val documentId = UUID.randomUUID()

    every { recallDocumentService.getDocumentContentWithCategoryIfExists(recallId, RecallDocumentCategory.RECALL_NOTIFICATION) } returns null
    every { recallSummaryService.getPdf(recallId) } returns Mono.just(recallSummaryBytes)
    every { revocationOrderService.getPdf(recallId) } returns Mono.just(revocationOrderBytes)
    every { letterToProbationService.getPdf(recallId) } returns Mono.just(letterToProbationBytes)
    every { pdfDocumentGenerator.mergePdfs(any()) } returns Mono.just(mergedBytes) // TODO: test that the argument list has 3 entries; the correct entries; but note likely to change call to decouple from Gotenberg
    every { recallDocumentService.uploadAndAddDocumentForRecall(recallId, mergedBytes, RecallDocumentCategory.RECALL_NOTIFICATION) } returns documentId

    val result = underTest.getDocument(recallId)

    StepVerifier.create(result).assertNext { assertThat(it, equalTo(mergedBytes)) }.verifyComplete()
  }

  @Test
  fun `get recall notification returns existing recall notification`() {
    val recallId = ::RecallId.random()
    val recallNotificationBytes = randomString().toByteArray()

    every { recallDocumentService.getDocumentContentWithCategoryIfExists(recallId, RecallDocumentCategory.RECALL_NOTIFICATION) } returns recallNotificationBytes

    val result = underTest.getDocument(recallId)

    verify(exactly = 0) { recallSummaryService.getPdf(recallId) }
    verify(exactly = 0) { revocationOrderService.getPdf(recallId) }
    verify(exactly = 0) { letterToProbationService.getPdf(recallId) }
    verify(exactly = 0) { pdfDocumentGenerator.mergePdfs(any()) }

    StepVerifier.create(result).assertNext { assertThat(it, equalTo(recallNotificationBytes)) }.verifyComplete()
  }
}
