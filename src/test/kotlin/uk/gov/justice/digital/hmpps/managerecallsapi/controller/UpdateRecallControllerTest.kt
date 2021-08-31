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
  private val underTest = UpdateRecallController(updateRecallService)

  private val recallId = ::RecallId.random()
  private val nomsNumber = NomsNumber("A9876ZZ")

  private val fullyPopulatedUpdateRecallRequest = UpdateRecallRequest(
    lastReleasePrison = "WIN",
    lastReleaseDate = LocalDate.now(),
    recallEmailReceivedDateTime = OffsetDateTime.now(),
    localPoliceForce = "Oxford",
    contrabandDetail = "Dodgy hat",
    vulnerabilityDiversityDetail = "Lots",
    mappaLevel = MappaLevel.CONFIRMATION_REQUIRED,
    sentenceDate = LocalDate.now(),
    licenceExpiryDate = LocalDate.now(),
    sentenceExpiryDate = LocalDate.now(),
    sentencingCourt = "court",
    indexOffence = "offence",
    conditionalReleaseDate = LocalDate.now(),
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
    currentPrison = "MWI"
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
    currentPrison = fullyPopulatedUpdateRecallRequest.currentPrison
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
  )

  @Test
  fun `can update recall and return a response with all fields populated`() {
    every { updateRecallService.updateRecall(recallId, fullyPopulatedUpdateRecallRequest) } returns fullyPopulatedRecall

    val response = underTest.updateRecall(recallId, fullyPopulatedUpdateRecallRequest)

    assertThat(response, equalTo(ResponseEntity.ok(fullyPopulatedRecallResponse)))
  }
}
