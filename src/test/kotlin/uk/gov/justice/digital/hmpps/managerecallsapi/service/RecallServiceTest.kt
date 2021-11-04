package uk.gov.justice.digital.hmpps.managerecallsapi.service

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.AgreeWithRecall.NO_STOP
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Api
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.LocalDeliveryUnit
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType.FIXED
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UpdateRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.ProbationInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentenceLength
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentencingInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.fullyPopulatedInstance
import uk.gov.justice.digital.hmpps.managerecallsapi.random.fullyPopulatedRecall
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomNoms
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
  private val fixedClock = Clock.fixed(Instant.parse("2021-10-04T13:15:50.00Z"), ZoneId.of("UTC"))
  private val underTest = RecallService(recallRepository, bankHolidayService, fixedClock)

  private val recallId = ::RecallId.random()
  private val existingRecall = Recall(recallId, NomsNumber("A9876ZZ"), ::UserId.random(), OffsetDateTime.now())
  private val today = LocalDate.now()

  private val fullyPopulatedUpdateRecallRequest: UpdateRecallRequest = fullyPopulatedInstance<UpdateRecallRequest>().copy(recallNotificationEmailSentDateTime = OffsetDateTime.now(fixedClock))

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
    recallLength = fullyPopulatedRecallSentencingInfo.calculateRecallLength(),
    lastReleasePrison = fullyPopulatedUpdateRecallRequest.lastReleasePrison,
    lastReleaseDate = fullyPopulatedUpdateRecallRequest.lastReleaseDate,
    recallEmailReceivedDateTime = fullyPopulatedUpdateRecallRequest.recallEmailReceivedDateTime,
    localPoliceForce = fullyPopulatedUpdateRecallRequest.localPoliceForce,
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
    hasOtherPreviousConvictionMainName = fullyPopulatedUpdateRecallRequest.hasOtherPreviousConvictionMainName,
    hasDossierBeenChecked = fullyPopulatedUpdateRecallRequest.hasDossierBeenChecked,
    previousConvictionMainName = fullyPopulatedUpdateRecallRequest.previousConvictionMainName,
    assessedByUserId = fullyPopulatedUpdateRecallRequest.assessedByUserId!!.value,
    bookedByUserId = fullyPopulatedUpdateRecallRequest.bookedByUserId!!.value,
    dossierCreatedByUserId = fullyPopulatedUpdateRecallRequest.dossierCreatedByUserId!!.value,
    dossierTargetDate = LocalDate.of(2021, 10, 5)
  )

  @Test
  fun `can update recall with all UpdateRecallRequest fields populated`() {
    every { bankHolidayService.isHoliday(LocalDate.of(2021, 10, 5)) } returns false
    every { recallRepository.getByRecallId(recallId) } returns existingRecall
    val fixedClockTime = OffsetDateTime.now(fixedClock)
    val updatedRecallWithoutDocs = fullyPopulatedRecallWithoutDocuments.copy(lastUpdatedDateTime = fixedClockTime, recallNotificationEmailSentDateTime = fixedClockTime)
    every { recallRepository.save(updatedRecallWithoutDocs) } returns updatedRecallWithoutDocs

    val response = underTest.updateRecall(recallId, fullyPopulatedUpdateRecallRequest)

    assertThat(response, equalTo(updatedRecallWithoutDocs))
  }

  @Test
  fun `cannot reset recall properties to null with update recall`() {
    val fullyPopulatedRecall: Recall = fullyPopulatedRecall(recallId)
    val updatedRecall = fullyPopulatedRecall.copy(lastUpdatedDateTime = OffsetDateTime.now(fixedClock))
    every { recallRepository.getByRecallId(recallId) } returns fullyPopulatedRecall
    every { recallRepository.save(updatedRecall) } returns updatedRecall

    val emptyUpdateRecallRequest = UpdateRecallRequest()
    val response = underTest.updateRecall(recallId, emptyUpdateRecallRequest)

    assertThat(response, equalTo(updatedRecall))
  }

  @Test
  fun `assignee cleared when recall is stopped`() {
    val recall = existingRecall.copy(assignee = UUID.randomUUID())
    val updatedRecall = existingRecall.copy(agreeWithRecall = NO_STOP, lastUpdatedDateTime = OffsetDateTime.now(fixedClock), recallType = FIXED)
    every { recallRepository.getByRecallId(recallId) } returns recall
    every { recallRepository.save(updatedRecall) } returns updatedRecall

    val updateRequest = UpdateRecallRequest(agreeWithRecall = NO_STOP)
    val response = underTest.updateRecall(recallId, updateRequest)

    assertThat(response, equalTo(updatedRecall))
  }

  @Test
  fun `dossierTargetDate when recallNotificationEmailSentDateTime is on Wednesday should be Thursday`() {
    every { bankHolidayService.isHoliday(LocalDate.of(2021, 10, 7)) } returns false
    val dossierTargetDate = underTest.calculateDossierTargetDate(OffsetDateTime.parse("2021-10-06T12:00Z"))

    assertThat(dossierTargetDate, equalTo(LocalDate.of(2021, 10, 7)))
  }

  @Test
  fun `dossierTargetDate when recallNotificationEmailSentDateTime is on Friday should be Monday`() {
    every { bankHolidayService.isHoliday(LocalDate.of(2021, 10, 11)) } returns false
    val dossierTargetDate = underTest.calculateDossierTargetDate(OffsetDateTime.parse("2021-10-08T12:00Z"))

    assertThat(dossierTargetDate, equalTo(LocalDate.of(2021, 10, 11)))
  }

  @Test
  fun `dossierTargetDate when recallNotificationEmailSentDateTime is day before weekend and bank holidays should be first non-weekend and non-bank-holiday`() {
    every { bankHolidayService.isHoliday(LocalDate.of(2021, 12, 27)) } returns true
    every { bankHolidayService.isHoliday(LocalDate.of(2021, 12, 28)) } returns true
    every { bankHolidayService.isHoliday(LocalDate.of(2021, 12, 29)) } returns false
    val dossierTargetDate = underTest.calculateDossierTargetDate(OffsetDateTime.parse("2021-12-24T12:00Z"))

    assertThat(dossierTargetDate, equalTo(LocalDate.of(2021, 12, 29)))
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
    val updatedRecallWithType = existingRecall.copy(recallType = FIXED, lastUpdatedDateTime = OffsetDateTime.now(fixedClock))
    every { recallRepository.save(updatedRecallWithType) } returns updatedRecallWithType

    val response = underTest.updateRecall(recallId, request)

    assertThat(response, equalTo(updatedRecallWithType))
  }

  @Test
  fun `can assign a recall`() {
    val nomsNumber = randomNoms()
    val now = OffsetDateTime.now()
    val createdByUserId = ::UserId.random()

    val recall = Recall(recallId, nomsNumber, createdByUserId, now, now)
    val assignee = ::UserId.random()
    val expected = Recall(recallId, nomsNumber, createdByUserId, now, OffsetDateTime.now(fixedClock), assignee = assignee)

    every { recallRepository.getByRecallId(recallId) } returns recall
    every { recallRepository.save(expected) } returns expected

    val assignedRecall = underTest.assignRecall(recallId, assignee)
    assertThat(assignedRecall, equalTo(expected))
  }

  @Test
  fun `can unassign a recall`() {
    val nomsNumber = randomNoms()
    val now = OffsetDateTime.now()
    val createdByUserId = ::UserId.random()
    val recall = Recall(recallId, nomsNumber, createdByUserId, now, now)
    val assignee = ::UserId.random()
    val expected = recall.copy(lastUpdatedDateTime = OffsetDateTime.now(fixedClock))

    every { recallRepository.getByRecallId(recallId) } returns Recall(recallId, nomsNumber, createdByUserId, now, now, assignee = assignee)
    every { recallRepository.save(expected) } returns expected

    val assignedRecall = underTest.unassignRecall(recallId, assignee)
    assertThat(assignedRecall, equalTo(expected))
  }

  @Test
  @Throws(NotFoundException::class)
  fun `can't unassign a recall when assignee doesnt match`() {
    val assignee = ::UserId.random()
    val otherAssignee = ::UserId.random()
    val nomsNumber = randomNoms()
    val createdByUserId = ::UserId.random()

    every { recallRepository.getByRecallId(recallId) } returns Recall(recallId, nomsNumber, createdByUserId, OffsetDateTime.now(), OffsetDateTime.now(), assignee = assignee)

    assertThrows<NotFoundException> { underTest.unassignRecall(recallId, otherAssignee) }
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
