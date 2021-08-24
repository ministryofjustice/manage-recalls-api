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
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentenceLength
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentencingInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallNotFoundException
import java.time.LocalDate

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

    val response = authenticatedPatchRequest("/recalls/$recallId", UpdateRecallRequest(agreeWithRecallRecommendation = true))

    assertThat(
      response,
      equalTo(RecallResponse(recallId, nomsNumber, emptyList(), agreeWithRecallRecommendation = true))
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

    val sentencingInfo = SentencingInfo(LocalDate.now(), LocalDate.now(), LocalDate.now(), "court", "index offence", SentenceLength(2, 5, 31))
    val updatedRecall = existingRecall.copy(sentencingInfo = sentencingInfo, recallType = FIXED, recallLength = TWENTY_EIGHT_DAYS)
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
        sentenceLength = Api.SentenceLength(sentencingInfo.sentenceLength.sentenceYears, sentencingInfo.sentenceLength.sentenceMonths, sentencingInfo.sentenceLength.sentenceDays),
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
          recallLength = TWENTY_EIGHT_DAYS
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

    assertThat(response, equalTo(RecallResponse(recallId, nomsNumber, emptyList(), bookingNumber = "BN12345")))
  }

  @Deprecated("Use localPoliceForce, delete this field once PUD-409 is complete in the UI")
  @Test
  fun `update a recall with local police service`() {
    val existingRecall = Recall(recallId, nomsNumber)
    every { recallRepository.getByRecallId(recallId) } returns existingRecall

    val policeForce = "London"
    val updatedRecall = existingRecall.copy(localPoliceForce = policeForce, recallType = FIXED)
    every { recallRepository.save(updatedRecall) } returns updatedRecall

    val response = authenticatedPatchRequest("/recalls/$recallId", UpdateRecallRequest(localPoliceService = policeForce))

    assertThat(response, equalTo(RecallResponse(recallId, nomsNumber, emptyList(), localPoliceService = policeForce, localPoliceForce = policeForce)))
  }

  @Test
  fun `update a recall with local police force`() {
    val existingRecall = Recall(recallId, nomsNumber)
    every { recallRepository.getByRecallId(recallId) } returns existingRecall

    val policeForce = "London"
    val updatedRecall = existingRecall.copy(localPoliceForce = policeForce, recallType = FIXED)
    every { recallRepository.save(updatedRecall) } returns updatedRecall

    val response = authenticatedPatchRequest("/recalls/$recallId", UpdateRecallRequest(localPoliceForce = policeForce))

    assertThat(response, equalTo(RecallResponse(recallId, nomsNumber, emptyList(), localPoliceService = policeForce, localPoliceForce = policeForce)))
  }

  private fun authenticatedPatchRequest(path: String, request: Any): RecallResponse =
    sendAuthenticatedPatchRequestWithBody(path, request)
      .expectStatus().isOk
      .expectBody(RecallResponse::class.java)
      .returnResult()
      .responseBody!!
}
