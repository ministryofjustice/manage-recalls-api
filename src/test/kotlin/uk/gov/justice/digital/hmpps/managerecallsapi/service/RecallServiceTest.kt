package uk.gov.justice.digital.hmpps.managerecallsapi.service

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.AgreeWithRecall.NO_STOP
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Api
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.LocalDeliveryUnit
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType.FIXED
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Status
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UpdateRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.ProbationInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.ReturnedToCustodyRecord
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentenceLength
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentencingInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CroNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.WarrantReferenceNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.fullyPopulatedInstance
import uk.gov.justice.digital.hmpps.managerecallsapi.random.fullyPopulatedRecall
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomNoms
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Movement
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonApiClient
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerOffenderSearchClient
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.UUID
import java.util.stream.Stream

@TestInstance(PER_CLASS)
class RecallServiceTest {
  private val recallRepository = mockk<RecallRepository>()
  private val bankHolidayService = mockk<BankHolidayService>()
  private val prisonerOffenderSearchClient = mockk<PrisonerOffenderSearchClient>()
  private val prisonApiClient = mockk<PrisonApiClient>()
  private val fixedClock = Clock.fixed(Instant.parse("2021-10-04T13:15:50.00Z"), ZoneId.of("UTC"))
  private val returnToCustodyUpdateThresholdMinutes = 60L
  private val underTest =
    RecallService(recallRepository, bankHolidayService, prisonerOffenderSearchClient, prisonApiClient, fixedClock, returnToCustodyUpdateThresholdMinutes)

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

  private val nextWorkingDate = LocalDate.of(2021, 10, 5)

  private val fullyPopulatedUpdateRecallRequest: UpdateRecallRequest =
    fullyPopulatedInstance<UpdateRecallRequest>().copy(
      inCustodyAtBooking = true,
      recallNotificationEmailSentDateTime = OffsetDateTime.now(fixedClock)
    )

  private val fullyPopulatedRecallSentencingInfo = SentencingInfo(
    fullyPopulatedUpdateRecallRequest.sentenceDate!!,
    fullyPopulatedUpdateRecallRequest.licenceExpiryDate!!,
    fullyPopulatedUpdateRecallRequest.sentenceExpiryDate!!,
    fullyPopulatedUpdateRecallRequest.sentencingCourt!!,
    fullyPopulatedUpdateRecallRequest.indexOffence!!,
    SentenceLength(
      fullyPopulatedUpdateRecallRequest.sentenceLength!!.years,
      fullyPopulatedUpdateRecallRequest.sentenceLength!!.months,
      fullyPopulatedUpdateRecallRequest.sentenceLength!!.days
    ),
    fullyPopulatedUpdateRecallRequest.conditionalReleaseDate
  )

  private val fullyPopulatedRecallWithoutDocuments = existingRecall.copy(
    recallType = FIXED,
    licenceNameCategory = fullyPopulatedUpdateRecallRequest.licenceNameCategory!!,
    recallLength = fullyPopulatedRecallSentencingInfo.calculateRecallLength(),
    lastReleasePrison = fullyPopulatedUpdateRecallRequest.lastReleasePrison,
    lastReleaseDate = fullyPopulatedUpdateRecallRequest.lastReleaseDate,
    recallEmailReceivedDateTime = fullyPopulatedUpdateRecallRequest.recallEmailReceivedDateTime,
    localPoliceForceId = fullyPopulatedUpdateRecallRequest.localPoliceForceId,
    inCustodyAtBooking = fullyPopulatedUpdateRecallRequest.inCustodyAtBooking,
    inCustodyAtAssessment = fullyPopulatedUpdateRecallRequest.inCustodyAtAssessment,
    contraband = fullyPopulatedUpdateRecallRequest.contraband,
    contrabandDetail = fullyPopulatedUpdateRecallRequest.contrabandDetail,
    vulnerabilityDiversity = fullyPopulatedUpdateRecallRequest.vulnerabilityDiversity,
    vulnerabilityDiversityDetail = fullyPopulatedUpdateRecallRequest.vulnerabilityDiversityDetail,
    mappaLevel = fullyPopulatedUpdateRecallRequest.mappaLevel,
    sentencingInfo = fullyPopulatedRecallSentencingInfo,
    bookingNumber = fullyPopulatedUpdateRecallRequest.bookingNumber,
    probationInfo = ProbationInfo(
      fullyPopulatedUpdateRecallRequest.probationOfficerName!!,
      fullyPopulatedUpdateRecallRequest.probationOfficerPhoneNumber!!,
      fullyPopulatedUpdateRecallRequest.probationOfficerEmail!!,
      fullyPopulatedUpdateRecallRequest.localDeliveryUnit!!,
      fullyPopulatedUpdateRecallRequest.authorisingAssistantChiefOfficer!!,
    ),
    licenceConditionsBreached = fullyPopulatedUpdateRecallRequest.licenceConditionsBreached,
    reasonsForRecall = fullyPopulatedUpdateRecallRequest.reasonsForRecall!!.toSet(),
    reasonsForRecallOtherDetail = fullyPopulatedUpdateRecallRequest.reasonsForRecallOtherDetail,
    agreeWithRecall = fullyPopulatedUpdateRecallRequest.agreeWithRecall,
    agreeWithRecallDetail = fullyPopulatedUpdateRecallRequest.agreeWithRecallDetail,
    currentPrison = fullyPopulatedUpdateRecallRequest.currentPrison,
    additionalLicenceConditions = fullyPopulatedUpdateRecallRequest.additionalLicenceConditions,
    additionalLicenceConditionsDetail = fullyPopulatedUpdateRecallRequest.additionalLicenceConditionsDetail,
    differentNomsNumber = fullyPopulatedUpdateRecallRequest.differentNomsNumber,
    differentNomsNumberDetail = fullyPopulatedUpdateRecallRequest.differentNomsNumberDetail,
    recallNotificationEmailSentDateTime = fullyPopulatedUpdateRecallRequest.recallNotificationEmailSentDateTime,
    dossierEmailSentDate = fullyPopulatedUpdateRecallRequest.dossierEmailSentDate,
    previousConvictionMainNameCategory = fullyPopulatedUpdateRecallRequest.previousConvictionMainNameCategory,
    hasDossierBeenChecked = fullyPopulatedUpdateRecallRequest.hasDossierBeenChecked,
    previousConvictionMainName = fullyPopulatedUpdateRecallRequest.previousConvictionMainName,
    assessedByUserId = fullyPopulatedUpdateRecallRequest.assessedByUserId!!.value,
    bookedByUserId = fullyPopulatedUpdateRecallRequest.bookedByUserId!!.value,
    dossierCreatedByUserId = fullyPopulatedUpdateRecallRequest.dossierCreatedByUserId!!.value,
    dossierTargetDate = nextWorkingDate,
    lastKnownAddressOption = fullyPopulatedUpdateRecallRequest.lastKnownAddressOption,
    arrestIssues = fullyPopulatedUpdateRecallRequest.arrestIssues,
    arrestIssuesDetail = fullyPopulatedUpdateRecallRequest.arrestIssuesDetail,
    warrantReferenceNumber = fullyPopulatedUpdateRecallRequest.warrantReferenceNumber,
  )

  @Test
  fun `can update recall with all UpdateRecallRequest fields populated`() {
    every { bankHolidayService.nextWorkingDate(LocalDate.of(2021, 10, 4)) } returns nextWorkingDate
    every { recallRepository.getByRecallId(recallId) } returns existingRecall
    val updatedRecallWithoutDocs = fullyPopulatedRecallWithoutDocuments.copy(
      recallNotificationEmailSentDateTime = OffsetDateTime.now(fixedClock),
      returnedToCustody = null
    )
    every { recallRepository.save(updatedRecallWithoutDocs, currentUserId) } returns updatedRecallWithoutDocs

    val response = underTest.updateRecall(recallId, fullyPopulatedUpdateRecallRequest, currentUserId)

    assertThat(response, equalTo(updatedRecallWithoutDocs))
  }

  @Test
  fun `cannot reset recall properties to null with update recall`() {
    val fullyPopulatedRecall: Recall = fullyPopulatedRecall(recallId)
    every { recallRepository.getByRecallId(recallId) } returns fullyPopulatedRecall
    every { recallRepository.save(fullyPopulatedRecall, currentUserId) } returns fullyPopulatedRecall

    val emptyUpdateRecallRequest = UpdateRecallRequest()
    val response = underTest.updateRecall(recallId, emptyUpdateRecallRequest, currentUserId)

    assertThat(response, equalTo(fullyPopulatedRecall))
  }

  @Test
  fun `assignee cleared when recall is stopped`() {
    val recall = existingRecall.copy(assignee = UUID.randomUUID())
    val updatedRecall = existingRecall.copy(agreeWithRecall = NO_STOP, recallType = FIXED)
    every { recallRepository.getByRecallId(recallId) } returns recall
    every { recallRepository.save(updatedRecall, currentUserId) } returns updatedRecall

    val updateRequest = UpdateRecallRequest(agreeWithRecall = NO_STOP)
    val response = underTest.updateRecall(recallId, updateRequest, currentUserId)

    assertThat(response, equalTo(updatedRecall))
  }

  @Test
  fun `dossierTargetDate set when in custody`() {
    every { bankHolidayService.nextWorkingDate(LocalDate.of(2021, 10, 6)) } returns LocalDate.of(2021, 10, 7)
    val dossierTargetDate = underTest.calculateDossierTargetDate(
      UpdateRecallRequest(
        inCustodyAtBooking = true,
        recallNotificationEmailSentDateTime = OffsetDateTime.parse("2021-10-06T12:00Z")
      ),
      existingRecall
    )

    assertThat(dossierTargetDate, equalTo(LocalDate.of(2021, 10, 7)))
  }

  @Test
  fun `dossierTargetDate not set when not in custody`() {
    val dossierTargetDate = underTest.calculateDossierTargetDate(
      UpdateRecallRequest(recallNotificationEmailSentDateTime = OffsetDateTime.parse("2021-10-06T12:00Z")),
      existingRecall
    )

    assertThat(dossierTargetDate, equalTo(null))
  }

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
    // TODO: don't set the recallType unless we need to
    val updatedRecallWithType = existingRecall.copy(recallType = FIXED)
    every { recallRepository.save(updatedRecallWithType, currentUserId) } returns updatedRecallWithType

    val response = underTest.updateRecall(recallId, request, currentUserId)

    assertThat(response, equalTo(updatedRecallWithType))
  }

  @Test
  fun `can assign a recall`() {
    val nomsNumber = randomNoms()
    val now = OffsetDateTime.now()
    val createdByUserId = ::UserId.random()

    val recall = Recall(
      recallId,
      nomsNumber,
      createdByUserId,
      now,
      FirstName("Barrie"),
      null,
      LastName("Badger"),
      CroNumber("ABC/1234A"),
      LocalDate.of(1999, 12, 1)
    )
    val assignee = ::UserId.random()
    val expected = Recall(
      recallId,
      nomsNumber,
      createdByUserId,
      now,
      FirstName("Barrie"),
      null,
      LastName("Badger"),
      CroNumber("ABC/1234A"),
      LocalDate.of(1999, 12, 1),
      assignee = assignee
    )

    every { recallRepository.getByRecallId(recallId) } returns recall
    every { recallRepository.save(expected, currentUserId) } returns expected

    val assignedRecall = underTest.assignRecall(recallId, assignee, currentUserId)
    assertThat(assignedRecall, equalTo(expected))
  }

  @Test
  fun `can unassign a recall`() {
    val nomsNumber = randomNoms()
    val now = OffsetDateTime.now()
    val createdByUserId = ::UserId.random()
    val recall = Recall(
      recallId,
      nomsNumber,
      createdByUserId,
      now,
      FirstName("Barrie"),
      null,
      LastName("Badger"),
      CroNumber("ABC/1234A"),
      LocalDate.of(1999, 12, 1)
    )
    val assignee = ::UserId.random()

    every { recallRepository.getByRecallId(recallId) } returns Recall(
      recallId,
      nomsNumber,
      createdByUserId,
      now,
      FirstName("Barrie"),
      null,
      LastName("Badger"),
      CroNumber("ABC/1234A"),
      LocalDate.of(1999, 12, 1),
      assignee = assignee
    )
    every { recallRepository.save(recall, currentUserId) } returns recall

    val assignedRecall = underTest.unassignRecall(recallId, assignee, currentUserId)
    assertThat(assignedRecall, equalTo(recall))
  }

  @Test
  fun `can't unassign a recall when assignee doesnt match`() {
    val assignee = ::UserId.random()
    val otherAssignee = ::UserId.random()
    val nomsNumber = randomNoms()
    val createdByUserId = ::UserId.random()

    every { recallRepository.getByRecallId(recallId) } returns Recall(
      recallId,
      nomsNumber,
      createdByUserId,
      OffsetDateTime.now(),
      FirstName("Barrie"),
      null,
      LastName("Badger"),
      CroNumber("ABC/1234A"),
      LocalDate.of(1999, 12, 1),
      assignee = assignee
    )

    assertThrows<NotFoundException> { underTest.unassignRecall(recallId, otherAssignee, currentUserId) }
  }

  @Test
  fun `manually update returned to custody`() {
    val returnedToCustodyDateTime = OffsetDateTime.now().minusHours(3)
    val returnedToCustodyNotificationDateTime = OffsetDateTime.now().minusMinutes(10)
    val returnedToCustodyRecord = ReturnedToCustodyRecord(
      returnedToCustodyDateTime,
      returnedToCustodyNotificationDateTime,
      OffsetDateTime.now(fixedClock),
      currentUserId
    )
    val updatedRecall = existingRecall.copy(returnedToCustody = returnedToCustodyRecord, dossierTargetDate = LocalDate.now().plusDays(1))

    every { recallRepository.getByRecallId(recallId) } returns existingRecall
    every { recallRepository.save(updatedRecall, currentUserId) } returns updatedRecall
    every { bankHolidayService.nextWorkingDate(returnedToCustodyDateTime.toLocalDate()) } returns LocalDate.now().plusDays(1)

    underTest.manuallyReturnedToCustody(recallId, returnedToCustodyDateTime, returnedToCustodyNotificationDateTime, currentUserId)

    verify { recallRepository.getByRecallId(recallId) }
    verify { recallRepository.save(updatedRecall, currentUserId) }
    verify { bankHolidayService.nextWorkingDate(returnedToCustodyDateTime.toLocalDate()) }
  }

  @Test
  fun `automatically update Awaiting RTC recalls for offenders that are now in custody and last updated before the threshold`() {
    val inCustodyRecall = mockk<Recall>()
    every { inCustodyRecall.status() } returns Status.AWAITING_DOSSIER_CREATION

    val nicStillUalRecentlyUpdatedNoms = NomsNumber("ZYX9876W")
    val nicStillUalRecentlyUpdatedRecall = mockk<Recall>()
    every { nicStillUalRecentlyUpdatedRecall.status() } returns Status.AWAITING_RETURN_TO_CUSTODY
    every { nicStillUalRecentlyUpdatedRecall.nomsNumber } returns nicStillUalRecentlyUpdatedNoms
    every { nicStillUalRecentlyUpdatedRecall.lastUpdatedDateTime } returns OffsetDateTime.now(fixedClock).minusMinutes(10)

    val nicStillUalPreviouslyUpdatedNoms = NomsNumber("ZYX9876W")
    val nicStillUalPreviouslyUpdatedRecall = mockk<Recall>()
    every { nicStillUalPreviouslyUpdatedRecall.status() } returns Status.AWAITING_RETURN_TO_CUSTODY
    every { nicStillUalPreviouslyUpdatedRecall.nomsNumber } returns nicStillUalPreviouslyUpdatedNoms
    every { nicStillUalPreviouslyUpdatedRecall.lastUpdatedDateTime } returns OffsetDateTime.now(fixedClock).minusMinutes(returnToCustodyUpdateThresholdMinutes - 10)

    val nicStillUalRecentlyUpdatedPrisoner = mockk<Prisoner>()
    every { nicStillUalRecentlyUpdatedPrisoner.status } returns "NOT ACTIVE"

    val nicRtcRecall = existingRecall.copy(
      lastUpdatedDateTime = OffsetDateTime.now(fixedClock).minusMinutes(returnToCustodyUpdateThresholdMinutes + 10),
      assessedByUserId = UUID.randomUUID(),
      inCustodyAtAssessment = false,
      warrantReferenceNumber = WarrantReferenceNumber("ABC12345/AB")
    )
    val rtcNoms = nicRtcRecall.nomsNumber
    val nicRtcPrisoner = mockk<Prisoner>()
    every { nicRtcPrisoner.status } returns "ACTIVE IN"

    val recallList = listOf(nicRtcRecall, nicStillUalRecentlyUpdatedRecall, nicStillUalPreviouslyUpdatedRecall, inCustodyRecall)

    val returnedToCustodyDateTime = OffsetDateTime.now().minusHours(15)
    val movementDate = returnedToCustodyDateTime.toLocalDate()
    val movementTime = returnedToCustodyDateTime.toLocalTime()
    val dossierTargetDate = OffsetDateTime.now(fixedClock).plusDays(1).toLocalDate()
    val expectedRecall = nicRtcRecall.copy(
      returnedToCustody = ReturnedToCustodyRecord(
        returnedToCustodyDateTime,
        OffsetDateTime.now(fixedClock),
        OffsetDateTime.now(fixedClock),
        RecallService.SYSTEM_USER_ID
      ),
      dossierTargetDate = dossierTargetDate
    )

    every { bankHolidayService.nextWorkingDate(OffsetDateTime.now(fixedClock).toLocalDate()) } returns dossierTargetDate
    every { recallRepository.findAll() } returns recallList
    every { prisonerOffenderSearchClient.prisonerByNomsNumber(nicStillUalPreviouslyUpdatedNoms) } returns Mono.just(nicStillUalRecentlyUpdatedPrisoner)
    every { prisonerOffenderSearchClient.prisonerByNomsNumber(rtcNoms) } returns Mono.just(nicRtcPrisoner)

    every { prisonApiClient.latestInboundMovements(setOf(rtcNoms)) } returns listOf(Movement(rtcNoms.value, movementDate, movementTime))
    every { recallRepository.save(expectedRecall, RecallService.SYSTEM_USER_ID) } returns expectedRecall

    underTest.updateCustodyStatus(currentUserId)

    verify { recallRepository.save(expectedRecall, RecallService.SYSTEM_USER_ID) }
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
