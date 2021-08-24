package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.ResponseEntity
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType.FIXED
import uk.gov.justice.digital.hmpps.managerecallsapi.db.ProbationInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentenceLength
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentencingInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.stream.Stream

@TestInstance(PER_CLASS)
class UpdateRecallControllerTest {
  private val recallRepository = mockk<RecallRepository>()
  private val underTest = UpdateRecallController(recallRepository)

  private val recallId = ::RecallId.random()
  private val existingRecall = Recall(recallId, NomsNumber("A9876ZZ"))

  private val existingRecallResponse = RecallResponse(
    existingRecall.recallId(), existingRecall.nomsNumber, documents = emptyList()
  )

  private val fullyPopulatedUpdateRecallRequest = UpdateRecallRequest(
    agreeWithRecallRecommendation = true,
    lastReleasePrison = "Andys house",
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
    authorisingAssistantChiefOfficer = "Authorising Assistant Chief Officer"
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
  private val fullyPopulatedRecall = existingRecall.copy(
    recallType = FIXED,
    agreeWithRecallRecommendation = fullyPopulatedUpdateRecallRequest.agreeWithRecallRecommendation,
    recallLength = fullyPopulatedRecallSentencingInfo.calculateRecallLength(),
    recallEmailReceivedDateTime = fullyPopulatedUpdateRecallRequest.recallEmailReceivedDateTime,
    lastReleasePrison = fullyPopulatedUpdateRecallRequest.lastReleasePrison,
    lastReleaseDate = fullyPopulatedUpdateRecallRequest.lastReleaseDate,
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
    )
  )

  private val fullyPopulatedRecallResponse = existingRecallResponse.copy(
    agreeWithRecallRecommendation = fullyPopulatedUpdateRecallRequest.agreeWithRecallRecommendation,
    recallLength = fullyPopulatedRecallSentencingInfo.calculateRecallLength(),
    recallEmailReceivedDateTime = fullyPopulatedUpdateRecallRequest.recallEmailReceivedDateTime,
    lastReleasePrison = fullyPopulatedUpdateRecallRequest.lastReleasePrison,
    lastReleaseDate = fullyPopulatedUpdateRecallRequest.lastReleaseDate,
    localPoliceForce = fullyPopulatedUpdateRecallRequest.localPoliceForce,
    contrabandDetail = fullyPopulatedUpdateRecallRequest.contrabandDetail,
    vulnerabilityDiversityDetail = fullyPopulatedUpdateRecallRequest.vulnerabilityDiversityDetail,
    mappaLevel = fullyPopulatedUpdateRecallRequest.mappaLevel,
    sentenceDate = fullyPopulatedUpdateRecallRequest.sentenceDate!!,
    licenceExpiryDate = fullyPopulatedUpdateRecallRequest.licenceExpiryDate!!,
    sentenceExpiryDate = fullyPopulatedUpdateRecallRequest.sentenceExpiryDate!!,
    sentencingCourt = fullyPopulatedUpdateRecallRequest.sentencingCourt!!,
    indexOffence = fullyPopulatedUpdateRecallRequest.indexOffence!!,
    sentenceLength = Api.SentenceLength(
      fullyPopulatedUpdateRecallRequest.sentenceLength!!.years,
      fullyPopulatedUpdateRecallRequest.sentenceLength!!.months,
      fullyPopulatedUpdateRecallRequest.sentenceLength!!.days
    ),
    conditionalReleaseDate = fullyPopulatedUpdateRecallRequest.conditionalReleaseDate,
    bookingNumber = fullyPopulatedUpdateRecallRequest.bookingNumber,
    probationOfficerName = fullyPopulatedUpdateRecallRequest.probationOfficerName!!,
    probationOfficerPhoneNumber = fullyPopulatedUpdateRecallRequest.probationOfficerPhoneNumber!!,
    probationOfficerEmail = fullyPopulatedUpdateRecallRequest.probationOfficerEmail!!,
    probationDivision = fullyPopulatedUpdateRecallRequest.probationDivision!!,
    authorisingAssistantChiefOfficer = fullyPopulatedUpdateRecallRequest.authorisingAssistantChiefOfficer!!,
  )

  @Test
  fun `can update recall with all fields populated`() {
    every { recallRepository.getByRecallId(recallId) } returns existingRecall
    every { recallRepository.save(fullyPopulatedRecall) } returns fullyPopulatedRecall

    val response = underTest.updateRecall(recallId, fullyPopulatedUpdateRecallRequest)

    assertThat(response, equalTo(ResponseEntity.ok(fullyPopulatedRecallResponse)))
  }

  @Test
  fun `cannot reset recall properties to null with update recall`() {
    every { recallRepository.getByRecallId(recallId) } returns fullyPopulatedRecall
    every { recallRepository.save(fullyPopulatedRecall) } returns fullyPopulatedRecall

    val emptyUpdateRecallRequest = UpdateRecallRequest()
    val response = underTest.updateRecall(recallId, emptyUpdateRecallRequest)

    assertThat(response, equalTo(ResponseEntity.ok(fullyPopulatedRecallResponse)))
  }

  @Suppress("unused")
  private fun requestWithMissingMandatorySentencingInfo(): Stream<UpdateRecallRequest>? {
    return Stream.of(
      recallRequestWithMandatorySentencingInfo(sentenceDate = null),
      recallRequestWithMandatorySentencingInfo(licenceExpiryDate = null),
      recallRequestWithMandatorySentencingInfo(sentenceExpiryDate = null),
      recallRequestWithMandatorySentencingInfo(sentencingCourt = null),
      recallRequestWithMandatorySentencingInfo(indexOffence = null),
      recallRequestWithMandatorySentencingInfo(sentenceLength = null),
    )
  }

  @ParameterizedTest
  @MethodSource("requestWithMissingMandatorySentencingInfo")
  fun `should not update recall if a mandatory field is not populated`(request: UpdateRecallRequest) {
    assertUpdateRecallDoesNotUpdate(request)
  }

  private fun assertUpdateRecallDoesNotUpdate(request: UpdateRecallRequest) {
    every { recallRepository.getByRecallId(recallId) } returns existingRecall
    // TODO: don't set the recallType unless we need to
    val recallWithType = existingRecall.copy(recallType = FIXED)
    every { recallRepository.save(recallWithType) } returns recallWithType

    val response = underTest.updateRecall(recallId, request)

    assertThat(response, equalTo(ResponseEntity.ok(existingRecallResponse)))
  }

  private fun recallRequestWithMandatorySentencingInfo(
    sentenceDate: LocalDate? = LocalDate.now(),
    licenceExpiryDate: LocalDate? = LocalDate.now(),
    sentenceExpiryDate: LocalDate? = LocalDate.now(),
    sentencingCourt: String? = "court",
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

  @Suppress("unused")
  private fun requestWithMissingMandatoryProbationInfo(): Stream<UpdateRecallRequest>? {
    return Stream.of(
      recallRequestWithProbationInfo(probationOfficerName = null),
      recallRequestWithProbationInfo(probationOfficerPhoneNumber = null),
      recallRequestWithProbationInfo(probationOfficerEmail = null),
      recallRequestWithProbationInfo(probationDivision = null),
      recallRequestWithProbationInfo(authorisingAssistantChiefOfficer = null),
    )
  }

  @ParameterizedTest
  @MethodSource("requestWithMissingMandatoryProbationInfo")
  fun `should not update recall probation information if a mandatory field is not populated`(request: UpdateRecallRequest) {
    assertUpdateRecallDoesNotUpdate(request)
  }

  private fun recallRequestWithProbationInfo(
    probationOfficerName: String? = "PON",
    probationOfficerPhoneNumber: String? = "07111111111",
    probationOfficerEmail: String? = "email@email.com",
    probationDivision: ProbationDivision? = ProbationDivision.NORTH_EAST,
    authorisingAssistantChiefOfficer: String? = "AACO"
  ) = UpdateRecallRequest(
    probationOfficerName = probationOfficerName,
    probationOfficerPhoneNumber = probationOfficerPhoneNumber,
    probationOfficerEmail = probationOfficerEmail,
    probationDivision = probationDivision,
    authorisingAssistantChiefOfficer = authorisingAssistantChiefOfficer
  )
}
