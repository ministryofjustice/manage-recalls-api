package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.AgreeWithRecall
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Api
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.NameFormatCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ReasonForRecall.BREACH_EXCLUSION_ZONE
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength.TWENTY_EIGHT_DAYS
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Status
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UpdateRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentenceLength
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentencingInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PoliceForceId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.stream.Stream

class UpdateRecallComponentTest : ComponentTestBase() {
  @MockkBean
  private lateinit var fixedClock: Clock

  private val nomsNumber = NomsNumber("123456")
  private val zone = ZoneId.of("UTC")
  private val now = OffsetDateTime.ofInstant(Instant.parse("2021-10-04T14:15:43.682078Z"), zone)
  private lateinit var fixedClockTime: OffsetDateTime

  private lateinit var recallId: RecallId
  private lateinit var recallPath: String
  private lateinit var createdByUserId: UserId

  @BeforeEach
  fun setupExistingRecall() {
    recallId = ::RecallId.random()
    recallPath = "/recalls/$recallId"
    createdByUserId = authenticatedClient.userId
    recallRepository.save(
      Recall(
        recallId,
        nomsNumber,
        createdByUserId,
        now,
        FirstName("Barrie"),
        null,
        LastName("Badger")
      )
    )
    val instant = Instant.parse("2021-10-04T13:15:50.00Z")
    fixedClockTime = OffsetDateTime.ofInstant(instant, zone)
    every { fixedClock.instant() } returns instant
    every { fixedClock.zone } returns zone
  }

  @Test
  fun `update a recall returns updated recall`() {
    val response = authenticatedClient.updateRecall(
      recallId,
      UpdateRecallRequest(
        licenceNameCategory = NameFormatCategory.FIRST_MIDDLE_LAST,
        lastReleasePrison = PrisonId("MWI"),
        currentPrison = PrisonId("BMI")
      )
    )

    assertThat(
      response,
      equalTo(
        RecallResponse(
          recallId,
          nomsNumber,
          createdByUserId,
          now,
          fixedClockTime, FirstName("Barrie"), null, LastName("Badger"),
          NameFormatCategory.FIRST_MIDDLE_LAST,
          Status.BEING_BOOKED_ON,
          lastReleasePrison = PrisonId("MWI"),
          currentPrison = PrisonId("BMI")
        )
      )
    )
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
          createdByUserId,
          now,
          fixedClockTime, FirstName("Barrie"), null, LastName("Badger"),
          NameFormatCategory.FIRST_LAST,
          Status.BEING_BOOKED_ON,
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

    assertThat(
      response,
      equalTo(
        RecallResponse(
          recallId,
          nomsNumber,
          createdByUserId,
          now,
          fixedClockTime, FirstName("Barrie"), null, LastName("Badger"),
          NameFormatCategory.FIRST_LAST,
          Status.BEING_BOOKED_ON,
          bookingNumber = bookingNumber
        )
      )
    )
  }

  @Test
  fun `update a recall with local police force`() {
    val policeForce = "Greater Manchester Police"
    val policeForceId = PoliceForceId("greater-manchester")

    val response = authenticatedClient.updateRecall(
      recallId,
      UpdateRecallRequest(
        localPoliceForce = policeForce,
        localPoliceForceId = policeForceId
      )
    )

    assertThat(
      response,
      equalTo(
        RecallResponse(
          recallId,
          nomsNumber,
          createdByUserId,
          now,
          fixedClockTime, FirstName("Barrie"), null, LastName("Badger"),
          NameFormatCategory.FIRST_LAST,
          Status.BEING_BOOKED_ON,
          localPoliceForce = policeForce,
          localPoliceForceId = policeForceId
        )
      )
    )
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
          recallId,
          nomsNumber,
          createdByUserId,
          now,
          fixedClockTime, FirstName("Barrie"), null, LastName("Badger"),
          NameFormatCategory.FIRST_LAST,
          Status.BEING_BOOKED_ON,
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
          recallId,
          nomsNumber,
          createdByUserId,
          now,
          fixedClockTime, FirstName("Barrie"), null, LastName("Badger"),
          NameFormatCategory.FIRST_LAST,
          Status.BEING_BOOKED_ON,
          agreeWithRecall = AgreeWithRecall.YES,
          agreeWithRecallDetail = "Other reasons"
        )
      )
    )
  }

  @Test
  fun `complete assessment of a recall updates recallNotificationEmailSentDateTime and assessedByUserId and calculates dossierTargetDate as after weekend and bank holidays`() {
    val assessedByUserId = ::UserId.random()
    setupUserDetailsFor(assessedByUserId)
    val updateRecallRequest = UpdateRecallRequest(
      recallNotificationEmailSentDateTime = OffsetDateTime.of(2021, 12, 24, 12, 0, 0, 0, ZoneOffset.UTC),
      assessedByUserId = assessedByUserId
    )
    val response = authenticatedClient.updateRecall(recallId, updateRecallRequest)

    assertThat(
      response,
      equalTo(
        RecallResponse(
          recallId,
          nomsNumber,
          createdByUserId,
          now,
          fixedClockTime, FirstName("Barrie"), null, LastName("Badger"),
          NameFormatCategory.FIRST_LAST,
          Status.RECALL_NOTIFICATION_ISSUED,
          recallNotificationEmailSentDateTime = updateRecallRequest.recallNotificationEmailSentDateTime,
          assessedByUserId = assessedByUserId,
          dossierTargetDate = LocalDate.parse("2021-12-29")
        )
      )
    )
  }

  @Test
  fun `create dossier updates dossierEmailSentDate and dossierCreatedByUserId`() {
    val dossierCreatedByUserId = ::UserId.random()
    setupUserDetailsFor(dossierCreatedByUserId)
    val updateRecallRequest = UpdateRecallRequest(
      dossierEmailSentDate = LocalDate.now(),
      dossierCreatedByUserId = dossierCreatedByUserId
    )

    val response = authenticatedClient.updateRecall(recallId, updateRecallRequest)

    assertThat(
      response,
      equalTo(
        RecallResponse(
          recallId,
          nomsNumber,
          createdByUserId,
          now,
          fixedClockTime, FirstName("Barrie"), null, LastName("Badger"),
          NameFormatCategory.FIRST_LAST,
          status = Status.DOSSIER_ISSUED,
          dossierEmailSentDate = updateRecallRequest.dossierEmailSentDate,
          dossierCreatedByUserId = dossierCreatedByUserId
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
          recallId,
          nomsNumber,
          createdByUserId,
          now,
          fixedClockTime, FirstName("Barrie"), null, LastName("Badger"),
          NameFormatCategory.FIRST_LAST,
          Status.BEING_BOOKED_ON,
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
