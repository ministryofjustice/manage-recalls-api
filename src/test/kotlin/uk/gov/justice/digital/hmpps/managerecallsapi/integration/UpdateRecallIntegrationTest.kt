package uk.gov.justice.digital.hmpps.managerecallsapi.integration

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Api
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength.TWENTY_EIGHT_DAYS
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType.FIXED
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UpdateRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.ReasonForRecall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallReason
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentenceLength
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentencingInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallNotFoundException
import java.time.LocalDate
import java.util.UUID

class UpdateRecallIntegrationTest : IntegrationTestBase() {

  @MockkBean
  private lateinit var recallRepository: RecallRepository

  private val nomsNumber = NomsNumber("123456")
  private val recallId = ::RecallId.random()

  @Test
  fun `update a recall returns updated recall including with recallType of FIXED`() {
    val priorRecall = Recall(recallId, nomsNumber)
    every { recallRepository.getByRecallId(recallId) } returns priorRecall
    val expectedRecall = priorRecall.copy(agreeWithRecallRecommendation = true, recallType = FIXED)
    every { recallRepository.save(expectedRecall) } returns expectedRecall

    val response =
      authenticatedPatchRequest("/recalls/$recallId", UpdateRecallRequest(agreeWithRecallRecommendation = true))

    assertThat(
      response,
      equalTo(
        RecallResponse(
          recallId, nomsNumber, emptyList(), agreeWithRecallRecommendation = true,
          reasonsForRecall = emptyList()
        )
      )
    )
  }

  @Test
  fun `update a recall with blank mappaLevel returns 400`() {
    sendAuthenticatedPatchRequestWithBody("/recalls/$recallId", "{\"mappaLevel\":\"\"}")
      .expectStatus().isBadRequest
  }

  @Test
  fun `update a recall that does not exist returns 404`() {
    every { recallRepository.getByRecallId(recallId) } throws RecallNotFoundException("blah", Exception())

    sendAuthenticatedPatchRequestWithBody("/recalls/$recallId", UpdateRecallRequest())
      .expectStatus().isNotFound
  }

  @Test
  fun `update a recall with sentence information will update recall length`() {
    val existingRecall = Recall(recallId, nomsNumber)
    every { recallRepository.getByRecallId(recallId) } returns existingRecall

    val sentencingInfo = SentencingInfo(
      LocalDate.now(),
      LocalDate.now(),
      LocalDate.now(),
      "court",
      "index offence",
      SentenceLength(2, 5, 31)
    )
    val updatedRecall =
      existingRecall.copy(sentencingInfo = sentencingInfo, recallType = FIXED, recallLength = TWENTY_EIGHT_DAYS)
    every { recallRepository.save(updatedRecall) } returns updatedRecall

    val response = authenticatedPatchRequest(
      "/recalls/$recallId",
      UpdateRecallRequest(
        sentenceDate = sentencingInfo.sentenceDate,
        licenceExpiryDate = sentencingInfo.licenceExpiryDate,
        sentenceExpiryDate = sentencingInfo.sentenceExpiryDate,
        sentencingCourt = sentencingInfo.sentencingCourt,
        indexOffence = sentencingInfo.indexOffence,
        conditionalReleaseDate = sentencingInfo.conditionalReleaseDate,
        sentenceLength = Api.SentenceLength(
          sentencingInfo.sentenceLength.sentenceYears,
          sentencingInfo.sentenceLength.sentenceMonths,
          sentencingInfo.sentenceLength.sentenceDays
        ),
      )
    )

    assertThat(
      response,
      equalTo(
        RecallResponse(
          recallId,
          nomsNumber,
          emptyList(),
          sentenceDate = sentencingInfo.sentenceDate,
          licenceExpiryDate = sentencingInfo.licenceExpiryDate,
          sentenceExpiryDate = sentencingInfo.sentenceExpiryDate,
          sentencingCourt = sentencingInfo.sentencingCourt,
          indexOffence = sentencingInfo.indexOffence,
          sentenceLength = Api.SentenceLength(2, 5, 31),
          recallLength = TWENTY_EIGHT_DAYS,
          reasonsForRecall = emptyList()
        )
      )
    )
  }

  @Test
  fun `update a recall with booking number`() {
    val existingRecall = Recall(recallId, nomsNumber)
    every { recallRepository.getByRecallId(recallId) } returns existingRecall

    val updatedRecall = existingRecall.copy(bookingNumber = "BN12345", recallType = FIXED)
    every { recallRepository.save(updatedRecall) } returns updatedRecall

    val response = authenticatedPatchRequest("/recalls/$recallId", UpdateRecallRequest(bookingNumber = "BN12345"))

    assertThat(
      response,
      equalTo(
        RecallResponse(
          recallId, nomsNumber, emptyList(),
          reasonsForRecall = emptyList(), bookingNumber = "BN12345"
        )
      )
    )
  }

  @Test
  fun `update a recall with local police force`() {
    val existingRecall = Recall(recallId, nomsNumber)
    every { recallRepository.getByRecallId(recallId) } returns existingRecall

    val policeForce = "London"
    val updatedRecall = existingRecall.copy(localPoliceForce = policeForce, recallType = FIXED)
    every { recallRepository.save(updatedRecall) } returns updatedRecall

    val response = authenticatedPatchRequest("/recalls/$recallId", UpdateRecallRequest(localPoliceForce = policeForce))

    assertThat(
      response,
      equalTo(
        RecallResponse(
          recallId, nomsNumber, emptyList(),
          reasonsForRecall = emptyList(), localPoliceForce = policeForce
        )
      )
    )
  }

  @Test
  fun `update a recall with non empty reasons for recall list`() {
    val existingRecall = Recall(recallId, nomsNumber)
    every { recallRepository.getByRecallId(recallId) } returns existingRecall

    val recallReason = Api.RecallReason(UUID.randomUUID(), ReasonForRecall.BREACH_EXCLUSION_ZONE)
    val updatedRecall = existingRecall.copy(
      reasonsForRecall = setOf(
        RecallReason(
          recallReason.reasonId,
          recallId.value,
          recallReason.reasonForRecall
        )
      ),
      recallType = FIXED
    )
    every { recallRepository.save(updatedRecall) } returns updatedRecall

    val response = authenticatedPatchRequest("/recalls/$recallId", UpdateRecallRequest(reasonsForRecall = setOf(recallReason)))

    assertThat(
      response,
      equalTo(RecallResponse(recallId, nomsNumber, emptyList(), reasonsForRecall = listOf(recallReason)))
    )
  }

  private fun authenticatedPatchRequest(path: String, request: Any): RecallResponse =
    sendAuthenticatedPatchRequestWithBody(path, request)
      .expectStatus().isOk
      .expectBody(RecallResponse::class.java)
      .returnResult()
      .responseBody!!
}
