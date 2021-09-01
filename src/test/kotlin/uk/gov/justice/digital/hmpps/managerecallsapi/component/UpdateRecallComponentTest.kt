package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.startsWith
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.AgreeWithRecall
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Api
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ReasonForRecall
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength.TWENTY_EIGHT_DAYS
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UpdateRecallRequest
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
  private lateinit var recallPath: String

  @BeforeEach
  fun setupExistingRecall() {
    recallId = ::RecallId.random()
    recallPath = "/recalls/$recallId"
    recallRepository.save(Recall(recallId, nomsNumber))
  }

  @Test
  fun `update a recall returns updated recall`() {
    val response = authenticatedPatchRequest(
      recallPath, UpdateRecallRequest(lastReleasePrison = "BEL")
    )

    assertThat(response, equalTo(RecallResponse(recallId, nomsNumber, lastReleasePrison = "BEL")))
  }

  @Suppress("unused")
  private fun requestsWithInvalidEnumValues(): Stream<String> {
    return Stream.of(
      """{"mappaLevel":""}""",
      """{"mappaLevel":"INVALID"}""",
      """{"probationDivision":""}""",
      """{"probationDivision":"INVALID"}""",
      """{"reasonsForRecall": '"}""",
      """{"reasonsForRecall":["INVALID"]}""",
      """{"agreeWithRecall": '"}""",
      """{"agreeWithRecall":["INVALID"]}"""
    )
  }

  @ParameterizedTest
  @MethodSource("requestsWithInvalidEnumValues")
  fun `update a recall with invalid enum values returns 400`(jsonRequest: String) {
    sendAuthenticatedPatchRequestWithBody(recallPath, jsonRequest)
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
      recallPath,
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
          recallLength = TWENTY_EIGHT_DAYS,
          sentenceDate = sentencingInfo.sentenceDate,
          licenceExpiryDate = sentencingInfo.licenceExpiryDate,
          sentenceExpiryDate = sentencingInfo.sentenceExpiryDate,
          sentencingCourt = sentencingInfo.sentencingCourt,
          indexOffence = sentencingInfo.indexOffence,
          sentenceLength = Api.SentenceLength(2, 5, 31)
        )
      )
    )
  }

  @Test
  fun `update a recall with booking number`() {
    val bookingNumber = "BN12345"
    val response = authenticatedPatchRequest(recallPath, UpdateRecallRequest(bookingNumber = bookingNumber))

    assertThat(response, equalTo(RecallResponse(recallId, nomsNumber, bookingNumber = bookingNumber)))
  }

  @Test
  fun `update a recall with local police force`() {
    val policeForce = "London"
    val response = authenticatedPatchRequest(recallPath, UpdateRecallRequest(localPoliceForce = policeForce))

    assertThat(response, equalTo(RecallResponse(recallId, nomsNumber, localPoliceForce = policeForce)))
  }

  @Test
  fun `update a recall with non empty reasons for recall list`() {
    val recallReason = ReasonForRecall.BREACH_EXCLUSION_ZONE

    val response = authenticatedPatchRequest(
      recallPath,
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

  @Test
  fun `update a recall with agreeWithRecall and detail`() {
    val response = authenticatedPatchRequest(
      recallPath,
      UpdateRecallRequest(
        agreeWithRecall = AgreeWithRecall.YES,
        agreeWithRecallDetail = "Other reasons"
      )
    )

    assertThat(
      response,
      equalTo(
        RecallResponse(
          recallId, nomsNumber,
          agreeWithRecall = AgreeWithRecall.YES,
          agreeWithRecallDetail = "Other reasons"
        )
      )
    )
  }

  @Test
  fun `updates a recall with "letter to prison" info`() {
    val request = UpdateRecallRequest(
      additionalLicenceConditions = true,
      additionalLicenceConditionsDetail = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
      differentNomsNumber = false,
      differentNomsNumberDetail = "Lorem ipsum dolor sit amet, consectetur adipiscing."
    )
    val response = authenticatedPatchRequest(recallPath, request)

    assertThat(
      response,
      equalTo(
        RecallResponse(
          recallId = recallId,
          nomsNumber = nomsNumber,
          additionalLicenceConditions = request.additionalLicenceConditions,
          additionalLicenceConditionsDetail = request.additionalLicenceConditionsDetail,
          differentNomsNumber = request.differentNomsNumber,
          differentNomsNumberDetail = request.differentNomsNumberDetail
        )
      )
    )
  }

  @Suppress("unused")
  private fun invalidLetterToPrisonFields(): Stream<String> {
    return Stream.of(
      """{"additionalLicenceConditions": "yes"}""",
      """{"differentNomsNumber": "yes"}""",
    )
  }

  @ParameterizedTest
  @MethodSource("invalidLetterToPrisonFields")
  fun `validates the 'letter to prison' boolean fields`(requestJson: String) {
    webTestClient
      .patch()
      .uri(recallPath)
      .bodyValue(requestJson)
      .headers {
        it.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        it.withBearerAuthToken(testJwt("ROLE_MANAGE_RECALLS"))
      }
      .exchange()
      .expectStatus().isBadRequest
      .expectBody().jsonPath("$.message").value(Matchers.startsWith("JSON parse error"))
  }
}
