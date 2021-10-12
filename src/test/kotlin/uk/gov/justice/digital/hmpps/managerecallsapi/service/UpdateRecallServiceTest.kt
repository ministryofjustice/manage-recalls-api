package uk.gov.justice.digital.hmpps.managerecallsapi.service

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
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
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.fullyPopulatedInstance
import uk.gov.justice.digital.hmpps.managerecallsapi.random.fullyPopulatedRecall
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.stream.Stream

@TestInstance(PER_CLASS)
class UpdateRecallServiceTest {
  private val recallRepository = mockk<RecallRepository>()
  private val underTest = UpdateRecallService(recallRepository)

  private val recallId = ::RecallId.random()
  private val existingRecall = Recall(recallId, NomsNumber("A9876ZZ"))
  private val today = LocalDate.now()

  private val fullyPopulatedUpdateRecallRequest: UpdateRecallRequest = fullyPopulatedInstance()

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
  )

  @Test
  fun `can update recall with all UpdateRecallRequest fields populated`() {
    every { recallRepository.getByRecallId(recallId) } returns existingRecall
    every { recallRepository.save(fullyPopulatedRecallWithoutDocuments) } returns fullyPopulatedRecallWithoutDocuments

    val response = underTest.updateRecall(recallId, fullyPopulatedUpdateRecallRequest)

    assertThat(response, equalTo(fullyPopulatedRecallWithoutDocuments))
  }

  @Test
  fun `cannot reset recall properties to null with update recall`() {
    val fullyPopulatedRecall: Recall = fullyPopulatedRecall(recallId)
    every { recallRepository.getByRecallId(recallId) } returns fullyPopulatedRecall
    every { recallRepository.save(fullyPopulatedRecall) } returns fullyPopulatedRecall

    val emptyUpdateRecallRequest = UpdateRecallRequest()
    val response = underTest.updateRecall(recallId, emptyUpdateRecallRequest)

    assertThat(response, equalTo(fullyPopulatedRecall))
  }

  @Test
  fun `return dossierTargetDate when updating recallNotificationEmailSentDateTime`() {
    val emptyUpdateRecallRequest = UpdateRecallRequest(recallNotificationEmailSentDateTime = OffsetDateTime.parse("2021-10-08T12:00-06:00"))
    val recallWithDossier = existingRecall.copy(recallType = FIXED, recallNotificationEmailSentDateTime = OffsetDateTime.parse("2021-10-08T12:00-06:00"), dossierTargetDate = LocalDate.of(2021, 10, 11))
    every { recallRepository.getByRecallId(recallId) } returns existingRecall
    every { recallRepository.save(recallWithDossier) } returns recallWithDossier
    val response = underTest.updateRecall(recallId, emptyUpdateRecallRequest)

    assertThat(response.dossierTargetDate, equalTo(LocalDate.of(2021, 10, 11)))
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
    val recallWithType = existingRecall.copy(recallType = FIXED)
    every { recallRepository.save(recallWithType) } returns recallWithType

    val response = underTest.updateRecall(recallId, request)

    assertThat(response, equalTo(recallWithType))
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
