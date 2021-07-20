package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RevocationOrderService
import java.util.Base64
import java.util.UUID

class RecallsControllerTest {
  private val recallRepository = mockk<RecallRepository>()
  private val revocationOrderService = mockk<RevocationOrderService>()

  private val underTest = RecallsController(recallRepository, revocationOrderService)

  private val recallId = ::RecallId.random()
  private val nomsNumber = NomsNumber("A1234AA")
  private val revocationOrderDocS3Key = UUID.randomUUID()
  private val recallRequest = BookRecallRequest(nomsNumber)

  @Test
  fun `book recall returns request with id`() {
    val recall = recallRequest.toRecall()
    every { recallRepository.save(any()) } returns recall

    val results = underTest.bookRecall(recallRequest)

    assertThat(results.body, equalTo(RecallResponse(recall.recallId(), nomsNumber, null)))
  }

  @Test
  fun `gets all recalls`() {
    val recall = Recall(recallId, nomsNumber, null, emptySet())
    every { recallRepository.findAll() } returns listOf(recall)

    val results = underTest.findAll()

    assertThat(results, equalTo(listOf(RecallResponse(recallId, nomsNumber, null))))
  }

  @Test
  fun `gets a recall`() {
    val recall = Recall(recallId, nomsNumber, revocationOrderDocS3Key, emptySet())
    every { recallRepository.getByRecallId(recallId) } returns recall

    val results = underTest.getRecall(recallId)

    assertThat(results, equalTo(RecallResponse(recallId, nomsNumber, revocationOrderDocS3Key)))
  }

  @Test
  fun `book a recall`() {
    val recall = recallRequest.toRecall()
    val expectedPdf = "Some pdf".toByteArray()
    val expectedBase64Pdf = Base64.getEncoder().encodeToString(expectedPdf)

    every { revocationOrderService.getRevocationOrder(recall.recallId()) } returns Mono.just(expectedPdf)

    val result = underTest.getRevocationOrder(recall.recallId())

    StepVerifier
      .create(result)
      .assertNext {
        assertThat(it.body?.content, equalTo(expectedBase64Pdf))
      }
      .verifyComplete()
  }
}
