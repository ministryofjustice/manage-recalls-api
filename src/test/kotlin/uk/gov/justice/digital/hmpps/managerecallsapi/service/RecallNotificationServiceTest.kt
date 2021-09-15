package uk.gov.justice.digital.hmpps.managerecallsapi.service

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import uk.gov.justice.digital.hmpps.managerecallsapi.component.randomString
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerator
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random

@Suppress("ReactiveStreamsUnusedPublisher")
internal class RecallNotificationServiceTest {

  private val revocationOrderService = mockk<RevocationOrderService>()
  private val recallSummaryService = mockk<RecallSummaryService>()
  private val letterToProbationService = mockk<LetterToProbationService>()
  private val pdfDocumentGenerator = mockk<PdfDocumentGenerator>()

  private val underTest = RecallNotificationService(revocationOrderService, recallSummaryService, letterToProbationService, pdfDocumentGenerator)

  @Test
  fun `get recall notification returns recall summary, revocation order and letter to probation`() {
    val recallId = ::RecallId.random()
    val recallSummaryBytes = randomString().toByteArray()
    val revocationOrderBytes = randomString().toByteArray()
    val letterToProbationBytes = randomString().toByteArray()
    val mergedBytes = randomString().toByteArray()

    every { recallSummaryService.getPdf(recallId) } returns Mono.just(recallSummaryBytes)
    every { revocationOrderService.getPdf(recallId) } returns Mono.just(revocationOrderBytes)
    every { letterToProbationService.getPdf(recallId) } returns Mono.just(letterToProbationBytes)
    every { pdfDocumentGenerator.mergePdfs(any()) } returns Mono.just(mergedBytes) // TODO: test that the argument list has 3 entries; the correct entries; but note likely to change call to decouple from Gotenberg

    val result = underTest.getDocument(recallId)

    StepVerifier.create(result).assertNext { assertThat(it, equalTo(mergedBytes)) }.verifyComplete()
  }
}
