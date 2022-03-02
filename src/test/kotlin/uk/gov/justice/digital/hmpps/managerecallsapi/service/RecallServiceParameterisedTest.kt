package uk.gov.justice.digital.hmpps.managerecallsapi.service

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.micrometer.core.instrument.MeterRegistry
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Api
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.LocalDeliveryUnit
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UpdateRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CroNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonApiClient
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerOffenderSearchClient
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.stream.Stream

@TestInstance(PER_CLASS)
class RecallServiceParameterisedTest {
  private val recallRepository = mockk<RecallRepository>()
  private val bankHolidayService = mockk<BankHolidayService>()
  private val prisonerOffenderSearchClient = mockk<PrisonerOffenderSearchClient>()
  private val prisonApiClient = mockk<PrisonApiClient>()
  private val meterRegistry = mockk<MeterRegistry>()
  private val fixedClock = Clock.fixed(Instant.parse("2021-10-04T13:15:50.00Z"), ZoneId.of("UTC"))
  private val returnToCustodyUpdateThresholdMinutes = 60L
  private val underTest =
    RecallService(recallRepository, bankHolidayService, prisonerOffenderSearchClient, prisonApiClient, fixedClock, meterRegistry, returnToCustodyUpdateThresholdMinutes)

  private val recallId = ::RecallId.random()
  private val existingRecall = Recall(
    recallId,
    NomsNumber("A9876ZZ"),
    ::UserId.random(),
    OffsetDateTime.now(),
    FirstName("Barrie"),
    null,
    LastName("Badger"),
    CroNumber("ABC/1234A"),
    LocalDate.of(1999, 12, 1)
  )
  private val today = LocalDate.now()
  private val currentUserId = ::UserId.random()

  @Suppress("unused")
  private fun requestWithMissingMandatoryInfo(): Stream<UpdateRecallRequest>? {
    return Stream.of(
      recallRequestWithMandatorySentencingInfo(sentenceDate = null),
      recallRequestWithMandatorySentencingInfo(licenceExpiryDate = null),
      recallRequestWithMandatorySentencingInfo(sentenceExpiryDate = null),
      recallRequestWithMandatorySentencingInfo(sentencingCourt = null),
      recallRequestWithMandatorySentencingInfo(indexOffence = null),
      recallRequestWithMandatorySentencingInfo(sentenceLength = null),
      recallRequestWithProbationInfo(probationOfficerName = null),
      recallRequestWithProbationInfo(probationOfficerPhoneNumber = null),
      recallRequestWithProbationInfo(probationOfficerEmail = null),
      recallRequestWithProbationInfo(localDeliveryUnit = null),
      recallRequestWithProbationInfo(authorisingAssistantChiefOfficer = null)
    )
  }

  @ParameterizedTest
  @MethodSource("requestWithMissingMandatoryInfo")
  fun `should not update recall if a mandatory field is not populated`(request: UpdateRecallRequest) {
    assertUpdateRecallDoesNotUpdate(request)
  }

  private fun assertUpdateRecallDoesNotUpdate(request: UpdateRecallRequest) {
    every { recallRepository.getByRecallId(recallId) } returns existingRecall
    every { recallRepository.save(existingRecall, currentUserId) } returns existingRecall

    val response = underTest.updateRecall(recallId, request, currentUserId)

    assertThat(response, equalTo(existingRecall))
  }

  private fun recallRequestWithMandatorySentencingInfo(
    sentenceDate: LocalDate? = today,
    licenceExpiryDate: LocalDate? = today,
    sentenceExpiryDate: LocalDate? = today,
    sentencingCourt: CourtId? = CourtId("court"),
    indexOffence: String? = "index offence",
    sentenceLength: Api.SentenceLength? = Api.SentenceLength(10, 1, 1)
  ) = UpdateRecallRequest(
    sentenceDate = sentenceDate,
    licenceExpiryDate = licenceExpiryDate,
    sentenceExpiryDate = sentenceExpiryDate,
    sentencingCourt = sentencingCourt,
    indexOffence = indexOffence,
    sentenceLength = sentenceLength
  )

  private fun recallRequestWithProbationInfo(
    probationOfficerName: String? = "PON",
    probationOfficerPhoneNumber: String? = "07111111111",
    probationOfficerEmail: String? = "email@email.com",
    localDeliveryUnit: LocalDeliveryUnit? = LocalDeliveryUnit.PS_DURHAM,
    authorisingAssistantChiefOfficer: String? = "AACO"
  ) = UpdateRecallRequest(
    probationOfficerName = probationOfficerName,
    probationOfficerPhoneNumber = probationOfficerPhoneNumber,
    probationOfficerEmail = probationOfficerEmail,
    localDeliveryUnit = localDeliveryUnit,
    authorisingAssistantChiefOfficer = authorisingAssistantChiefOfficer
  )
}
