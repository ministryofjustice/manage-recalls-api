package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.springframework.http.ResponseEntity
import uk.gov.justice.digital.hmpps.managerecallsapi.db.ProbationInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentenceLength
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentencingInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import java.time.LocalDate
import java.time.OffsetDateTime

@TestInstance(PER_CLASS)
class UpdateRecallControllerTest {
  private val updateRecallService = mockk<UpdateRecallService>()
  private val prisonValidateService = mockk<PrisonValidationService>()
  private val underTest = UpdateRecallController(updateRecallService, prisonValidateService)

  private val recallId = ::RecallId.random()
  private val nomsNumber = NomsNumber("A9876ZZ")
  private val today = LocalDate.now()

  private val fullyPopulatedUpdateRecallRequest = UpdateRecallRequest(
    lastReleasePrison = "WIN",
    lastReleaseDate = today,
    recallEmailReceivedDateTime = OffsetDateTime.now(),
    localPoliceForce = "Oxford",
    contrabandDetail = "Dodgy hat",
    vulnerabilityDiversityDetail = "Lots",
    mappaLevel = MappaLevel.CONFIRMATION_REQUIRED,
    sentenceDate = today,
    licenceExpiryDate = today,
    sentenceExpiryDate = today,
    sentencingCourt = "court",
    indexOffence = "offence",
    conditionalReleaseDate = today,
    sentenceLength = Api.SentenceLength(10, 1, 1),
    bookingNumber = "B12345",
    probationOfficerName = "Probation officer name",
    probationOfficerPhoneNumber = "+44(0)111111111",
    probationOfficerEmail = "probationOfficer@email.com",
    probationDivision = ProbationDivision.LONDON,
    authorisingAssistantChiefOfficer = "Authorising Assistant Chief Officer",
    licenceConditionsBreached = "Breached on .... by ...",
    reasonsForRecall = setOf(
      ReasonForRecall.BREACH_EXCLUSION_ZONE,
      ReasonForRecall.ELM_BREACH_NON_CURFEW_CONDITION
    ),
    reasonsForRecallOtherDetail = "Because of something else...",
    currentPrison = "MWI",
    additionalLicenceConditions = true,
    additionalLicenceConditionsDetail = "Ut enim ad minima veniam, quis nostrum exercitationem",
    differentNomsNumber = false,
    differentNomsNumberDetail = "Nam libero tempore, cum soluta nobis est eligendi",
    recallNotificationEmailSentDateTime = OffsetDateTime.now(),
    dossierEmailSentDate = today
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

  private val fullyPopulatedRecall = Recall(
    recallId = recallId,
    nomsNumber = nomsNumber,
    recallType = RecallType.FIXED,
    recallLength = fullyPopulatedRecallSentencingInfo.calculateRecallLength(),
    lastReleasePrison = fullyPopulatedUpdateRecallRequest.lastReleasePrison,
    lastReleaseDate = fullyPopulatedUpdateRecallRequest.lastReleaseDate,
    recallEmailReceivedDateTime = fullyPopulatedUpdateRecallRequest.recallEmailReceivedDateTime,
    localPoliceForce = fullyPopulatedUpdateRecallRequest.localPoliceForce,
    contrabandDetail = fullyPopulatedUpdateRecallRequest.contrabandDetail,
    vulnerabilityDiversityDetail = fullyPopulatedUpdateRecallRequest.vulnerabilityDiversityDetail,
    mappaLevel = fullyPopulatedUpdateRecallRequest.mappaLevel,
    sentencingInfo = fullyPopulatedRecallSentencingInfo,
    bookingNumber = fullyPopulatedUpdateRecallRequest.bookingNumber,
    probationInfo = ProbationInfo(
      fullyPopulatedUpdateRecallRequest.probationOfficerName!!,
      fullyPopulatedUpdateRecallRequest.probationOfficerPhoneNumber!!,
      fullyPopulatedUpdateRecallRequest.probationOfficerEmail!!,
      fullyPopulatedUpdateRecallRequest.probationDivision!!,
      fullyPopulatedUpdateRecallRequest.authorisingAssistantChiefOfficer!!,
    ),
    licenceConditionsBreached = fullyPopulatedUpdateRecallRequest.licenceConditionsBreached,
    reasonsForRecall = fullyPopulatedUpdateRecallRequest.reasonsForRecall!!.toSet(),
    reasonsForRecallOtherDetail = fullyPopulatedUpdateRecallRequest.reasonsForRecallOtherDetail,
    currentPrison = fullyPopulatedUpdateRecallRequest.currentPrison,
    additionalLicenceConditions = fullyPopulatedUpdateRecallRequest.additionalLicenceConditions,
    additionalLicenceConditionsDetail = fullyPopulatedUpdateRecallRequest.additionalLicenceConditionsDetail,
    differentNomsNumber = fullyPopulatedUpdateRecallRequest.differentNomsNumber,
    differentNomsNumberDetail = fullyPopulatedUpdateRecallRequest.differentNomsNumberDetail,
    recallNotificationEmailSentDateTime = fullyPopulatedUpdateRecallRequest.recallNotificationEmailSentDateTime,
    dossierEmailSentDate = fullyPopulatedUpdateRecallRequest.dossierEmailSentDate
  )

  private val fullyPopulatedRecallResponse = RecallResponse(
    recallId = recallId,
    nomsNumber = nomsNumber,
    recallLength = fullyPopulatedRecallSentencingInfo.calculateRecallLength(),
    lastReleasePrison = fullyPopulatedUpdateRecallRequest.lastReleasePrison,
    lastReleaseDate = fullyPopulatedUpdateRecallRequest.lastReleaseDate,
    recallEmailReceivedDateTime = fullyPopulatedUpdateRecallRequest.recallEmailReceivedDateTime,
    localPoliceForce = fullyPopulatedUpdateRecallRequest.localPoliceForce,
    vulnerabilityDiversityDetail = fullyPopulatedUpdateRecallRequest.vulnerabilityDiversityDetail,
    contrabandDetail = fullyPopulatedUpdateRecallRequest.contrabandDetail,
    mappaLevel = fullyPopulatedUpdateRecallRequest.mappaLevel,
    sentenceDate = fullyPopulatedUpdateRecallRequest.sentenceDate!!,
    licenceExpiryDate = fullyPopulatedUpdateRecallRequest.licenceExpiryDate!!,
    sentenceExpiryDate = fullyPopulatedUpdateRecallRequest.sentenceExpiryDate!!,
    sentencingCourt = fullyPopulatedUpdateRecallRequest.sentencingCourt!!,
    indexOffence = fullyPopulatedUpdateRecallRequest.indexOffence!!,
    conditionalReleaseDate = fullyPopulatedUpdateRecallRequest.conditionalReleaseDate,
    sentenceLength = Api.SentenceLength(
      fullyPopulatedUpdateRecallRequest.sentenceLength!!.years,
      fullyPopulatedUpdateRecallRequest.sentenceLength!!.months,
      fullyPopulatedUpdateRecallRequest.sentenceLength!!.days
    ),
    bookingNumber = fullyPopulatedUpdateRecallRequest.bookingNumber,
    probationOfficerName = fullyPopulatedUpdateRecallRequest.probationOfficerName!!,
    probationOfficerPhoneNumber = fullyPopulatedUpdateRecallRequest.probationOfficerPhoneNumber!!,
    probationOfficerEmail = fullyPopulatedUpdateRecallRequest.probationOfficerEmail!!,
    probationDivision = fullyPopulatedUpdateRecallRequest.probationDivision!!,
    authorisingAssistantChiefOfficer = fullyPopulatedUpdateRecallRequest.authorisingAssistantChiefOfficer!!,
    licenceConditionsBreached = fullyPopulatedUpdateRecallRequest.licenceConditionsBreached,
    reasonsForRecall = fullyPopulatedUpdateRecallRequest.reasonsForRecall!!.toList(),
    reasonsForRecallOtherDetail = fullyPopulatedUpdateRecallRequest.reasonsForRecallOtherDetail,
    currentPrison = fullyPopulatedUpdateRecallRequest.currentPrison!!,
    additionalLicenceConditions = fullyPopulatedUpdateRecallRequest.additionalLicenceConditions,
    additionalLicenceConditionsDetail = fullyPopulatedUpdateRecallRequest.additionalLicenceConditionsDetail,
    differentNomsNumber = fullyPopulatedUpdateRecallRequest.differentNomsNumber,
    differentNomsNumberDetail = fullyPopulatedUpdateRecallRequest.differentNomsNumberDetail,
    recallNotificationEmailSentDateTime = fullyPopulatedUpdateRecallRequest.recallNotificationEmailSentDateTime,
    dossierEmailSentDate = fullyPopulatedUpdateRecallRequest.dossierEmailSentDate
  )

  @Test
  fun `can update recall and return a response with all fields populated`() {
    every { prisonValidateService.isPrisonValidAndActive("MWI") } returns true
    every { prisonValidateService.isPrisonValid("WIN") } returns true
    every { updateRecallService.updateRecall(recallId, fullyPopulatedUpdateRecallRequest) } returns fullyPopulatedRecall

    val response = underTest.updateRecall(recallId, fullyPopulatedUpdateRecallRequest)

    assertThat(response, equalTo(ResponseEntity.ok(fullyPopulatedRecallResponse)))
  }

  @Test
  fun `can't update recall when prison is not valid`() {
    every { prisonValidateService.isPrisonValidAndActive("MWI") } returns false
    every { updateRecallService.updateRecall(recallId, fullyPopulatedUpdateRecallRequest) } returns fullyPopulatedRecall

    val response = underTest.updateRecall(recallId, fullyPopulatedUpdateRecallRequest)

    assertThat(response, equalTo(ResponseEntity.badRequest().build()))
  }

  @Test
  fun `can't update recall when last release prison is not valid`() {
    every { prisonValidateService.isPrisonValid("WIN") } returns false
    every { prisonValidateService.isPrisonValidAndActive("MWI") } returns true
    every { updateRecallService.updateRecall(recallId, fullyPopulatedUpdateRecallRequest) } returns fullyPopulatedRecall

    val response = underTest.updateRecall(recallId, fullyPopulatedUpdateRecallRequest)

    assertThat(response, equalTo(ResponseEntity.badRequest().build()))
  }
}
