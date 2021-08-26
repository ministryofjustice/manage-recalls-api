package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Api
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength.TWENTY_EIGHT_DAYS
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UpdateRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.ReasonForRecall.BREACH_EXCLUSION_ZONE
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentenceLength
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentencingInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import java.time.LocalDate
import java.util.stream.Stream

class UpdateRecallComponentTest : ComponentTestBase() {

  private val nomsNumber = NomsNumber("123456")

  private lateinit var recallId: RecallId

  @BeforeEach
  fun setupExistingRecall() {
    recallId = ::RecallId.random()
    recallRepository.save(Recall(recallId, nomsNumber))
  }

  @Test
  fun `update a recall returns updated recall`() {
    val response = authenticatedPatchRequest(
      "/recalls/$recallId", UpdateRecallRequest(agreeWithRecallRecommendation = true)
    )

    assertThat(response, equalTo(RecallResponse(recallId, nomsNumber, agreeWithRecallRecommendation = true)))
  }

  @Suppress("unused")
  private fun requestsWithInvalidEnumValues(): Stream<String> {
    return Stream.of(
      "{\"mappaLevel\":\"\"}",
      "{\"mappaLevel\":\"INVALID\"}",
      "{\"probationDivision\":\"\"}",
      "{\"probationDivision\":\"INVALID\"}",
      "{\"reasonsForRecall\": \'\"}",
      "{\"reasonsForRecall\":[\"INVALID\"]}"
    )
  }

  @ParameterizedTest
  @MethodSource("requestsWithInvalidEnumValues")
  fun `update a recall with invalid enum values returns 400`(jsonRequest: String) {
    sendAuthenticatedPatchRequestWithBody("/recalls/$recallId", jsonRequest)
      .expectStatus().isBadRequest
  }

  @Test
  fun `update a recall that does not exist returns 404`() {
    sendAuthenticatedPatchRequestWithBody("/recalls/${::RecallId.random()}", UpdateRecallRequest())
      .expectStatus().isNotFound
  }

  @Test
  fun `update a recall with sentence information will update recall length`() {
    val sentencingInfo = SentencingInfo(
      LocalDate.now(),
      LocalDate.now(),
      LocalDate.now(),
      "court",
      "index offence",
      SentenceLength(2, 5, 31)
    )

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
    val bookingNumber = "BN12345"
    val response = authenticatedPatchRequest("/recalls/$recallId", UpdateRecallRequest(bookingNumber = bookingNumber))

    assertThat(response, equalTo(RecallResponse(recallId, nomsNumber, bookingNumber = bookingNumber)))
  }

  @Test
  fun `update a recall with local police force`() {
    val policeForce = "London"
    val response = authenticatedPatchRequest("/recalls/$recallId", UpdateRecallRequest(localPoliceForce = policeForce))

    assertThat(response, equalTo(RecallResponse(recallId, nomsNumber, localPoliceForce = policeForce)))
  }

  @Test
  fun `update a recall with non empty reasons for recall list`() {
    val recallReason = BREACH_EXCLUSION_ZONE

    val response = authenticatedPatchRequest(
      "/recalls/$recallId",
      UpdateRecallRequest(
        licenceConditionsBreached = "Breached",
        reasonsForRecall = setOf(recallReason),
        reasonsForRecallOtherDetail = "Other reasons"
      )
    )

    assertThat(
      response,
      equalTo(
        RecallResponse(
          recallId, nomsNumber,
          licenceConditionsBreached = "Breached",
          reasonsForRecall = listOf(recallReason),
          reasonsForRecallOtherDetail = "Other reasons"
        )
      )
    )
  }

  private fun authenticatedPatchRequest(path: String, request: Any): RecallResponse =
    sendAuthenticatedPatchRequestWithBody(path, request)
      .expectStatus().isOk
      .expectBody(RecallResponse::class.java)
      .returnResult()
      .responseBody!!
}
