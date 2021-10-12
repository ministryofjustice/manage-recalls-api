package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.AgreeWithRecall
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Api
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ReasonForRecall.BREACH_EXCLUSION_ZONE
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength.TWENTY_EIGHT_DAYS
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UpdateRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentenceLength
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentencingInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.service.findDossierTargetDate
import java.time.LocalDate
import java.time.OffsetDateTime
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
    val response = authenticatedClient.updateRecall(recallId, UpdateRecallRequest(lastReleasePrison = PrisonId("MWI"), currentPrison = PrisonId("BMI")))

    assertThat(response, equalTo(RecallResponse(recallId, nomsNumber, lastReleasePrison = PrisonId("MWI"), currentPrison = PrisonId("BMI"))))
  }

  @Suppress("unused")
  private fun requestsWithInvalidEnumValues(): Stream<String> {
    return Stream.of(
      """{"mappaLevel":""}""",
      """{"mappaLevel":"INVALID"}""",
      """{"localDeliveryUnit":""}""",
      """{"localDeliveryUnit":"INVALID"}""",
      """{"reasonsForRecall": '"}""",
      """{"reasonsForRecall":["INVALID"]}""",
      """{"agreeWithRecall": '"}""",
      """{"agreeWithRecall":["INVALID"]}"""
    )
  }

  @ParameterizedTest
  @MethodSource("requestsWithInvalidEnumValues")
  fun `update a recall with invalid enum values returns 400`(jsonRequest: String) {
    authenticatedClient.patch(recallPath, jsonRequest)
      .expectStatus().isBadRequest
  }

  @Test
  fun `update a recall that does not exist returns 404`() {
    authenticatedClient.patch("/recalls/${::RecallId.random()}", "{}")
      .expectStatus().isNotFound
  }

  @Test
  fun `update a recall with sentence information will update recall length`() {
    val sentencingInfo = SentencingInfo(
      LocalDate.now(),
      LocalDate.now(),
      LocalDate.now(),
      CourtId("ACCRYC"),
      "index offence",
      SentenceLength(2, 5, 31)
    )

    val response = authenticatedClient.updateRecall(
      recallId,
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
        )
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
    val response = authenticatedClient.updateRecall(recallId, UpdateRecallRequest(bookingNumber = bookingNumber))

    assertThat(response, equalTo(RecallResponse(recallId, nomsNumber, bookingNumber = bookingNumber)))
  }

  @Test
  fun `update a recall with local police force`() {
    val policeForce = "London"
    val response = authenticatedClient.updateRecall(recallId, UpdateRecallRequest(localPoliceForce = policeForce))

    assertThat(response, equalTo(RecallResponse(recallId, nomsNumber, localPoliceForce = policeForce)))
  }

  @Test
  fun `update a recall with non empty reasons for recall list`() {
    val response = authenticatedClient.updateRecall(
      recallId,
      UpdateRecallRequest(
        licenceConditionsBreached = "Breached",
        reasonsForRecall = setOf(BREACH_EXCLUSION_ZONE),
        reasonsForRecallOtherDetail = "Other reasons"
      )
    )

    assertThat(
      response,
      equalTo(
        RecallResponse(
          recallId, nomsNumber,
          licenceConditionsBreached = "Breached",
          reasonsForRecall = listOf(BREACH_EXCLUSION_ZONE),
          reasonsForRecallOtherDetail = "Other reasons"
        )
      )
    )
  }

  @Test
  fun `update a recall with agreeWithRecall and detail`() {
    val response = authenticatedClient.updateRecall(
      recallId,
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
  fun `complete assessment of a recall updates recallNotificationEmailSentDateTime and assessedByUserId`() {
    val updateRecallRequest = UpdateRecallRequest(
      recallNotificationEmailSentDateTime = OffsetDateTime.now(),
      assessedByUserId = ::UserId.random()
    )
    val response = authenticatedClient.updateRecall(recallId, updateRecallRequest)

    assertThat(
      response,
      equalTo(
        RecallResponse(
          recallId, nomsNumber,
          recallNotificationEmailSentDateTime = updateRecallRequest.recallNotificationEmailSentDateTime,
          assessedByUserId = updateRecallRequest.assessedByUserId,
          dossierTargetDate = updateRecallRequest.findDossierTargetDate()
        )
      )
    )
  }

  @Test
  fun `create dossier updates dossierEmailSentDate and dossierCreatedByUserId`() {
    val updateRecallRequest = UpdateRecallRequest(
      dossierEmailSentDate = LocalDate.now(),
      dossierCreatedByUserId = ::UserId.random()
    )

    val response = authenticatedClient.updateRecall(recallId, updateRecallRequest)

    assertThat(
      response,
      equalTo(
        RecallResponse(
          recallId, nomsNumber,
          dossierEmailSentDate = updateRecallRequest.dossierEmailSentDate,
          dossierCreatedByUserId = updateRecallRequest.dossierCreatedByUserId
        )
      )
    )
  }

  @Test
  fun `updates a recall with 'letter to prison' info`() {
    val request = UpdateRecallRequest(
      additionalLicenceConditions = true,
      additionalLicenceConditionsDetail = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
      differentNomsNumber = false,
      differentNomsNumberDetail = "Lorem ipsum dolor sit amet, consectetur adipiscing."
    )
    val response = authenticatedClient.updateRecall(recallId, request)

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
    authenticatedClient.patch(recallPath, requestJson)
      .expectStatus().isBadRequest
      .expectBody().jsonPath("$.message").value(Matchers.startsWith("JSON parse error"))
  }

  @Suppress("unused")
  private fun invalidTimestampFields(): Stream<String> {
    return Stream.of(
      """{"recallEmailReceivedDateTime": "2222-99-99T99:99:00.000Z"}""",
      """{"recallNotificationEmailSentDateTime": "2222-99-99T99:99:00.000Z"}"""
    )
  }

  @ParameterizedTest
  @MethodSource("invalidTimestampFields")
  fun `validates timstamp fields`(requestJson: String) {
    authenticatedClient.patch(recallPath, requestJson)
      .expectStatus().isBadRequest
      .expectBody().jsonPath("$.message").value(Matchers.startsWith("JSON parse error"))
  }
}
