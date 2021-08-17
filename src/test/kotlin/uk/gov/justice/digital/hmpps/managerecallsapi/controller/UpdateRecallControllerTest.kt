package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.http.ResponseEntity
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength.FOURTEEN_DAYS
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType.FIXED
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentenceLength
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentencingInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import java.time.LocalDate
import java.time.ZonedDateTime

class UpdateRecallControllerTest {
  private val recallRepository = mockk<RecallRepository>()
  private val underTest = UpdateRecallController(recallRepository)

  private val nomsNumber = NomsNumber("A9876ZZ")
  private val recallId = ::RecallId.random()
  private val existingRecall = Recall(recallId, nomsNumber)

  private val recallLength = FOURTEEN_DAYS

  private val fullyPopulatedUpdateRecallRequest = UpdateRecallRequest(
    agreeWithRecallRecommendation = true,
    recallLength = recallLength,
    recallEmailReceivedDateTime = ZonedDateTime.now(),
    lastReleasePrison = "Andys house",
    lastReleaseDate = LocalDate.now(),
    localPoliceService = "Oxford",
    contrabandDetail = "Dodgy hat",
    vulnerabilityDiversityDetail = "Lots",
    mappaLevel = MappaLevel.CONFIRMATION_REQUIRED,
    sentenceDate = LocalDate.now(),
    licenceExpiryDate = LocalDate.now(),
    sentenceExpiryDate = LocalDate.now(),
    sentencingCourt = "court",
    indexOffence = "offence",
    conditionalReleaseDate = LocalDate.now(),
    sentenceLength = Api.SentenceLength(10, 1, 1)
  )

  private val fullyPopulatedRecall = existingRecall.copy(
    recallType = FIXED,
    agreeWithRecallRecommendation = fullyPopulatedUpdateRecallRequest.agreeWithRecallRecommendation,
    recallLength = fullyPopulatedUpdateRecallRequest.recallLength,
    recallEmailReceivedDateTime = fullyPopulatedUpdateRecallRequest.recallEmailReceivedDateTime,
    lastReleasePrison = fullyPopulatedUpdateRecallRequest.lastReleasePrison,
    lastReleaseDate = fullyPopulatedUpdateRecallRequest.lastReleaseDate,
    localPoliceService = fullyPopulatedUpdateRecallRequest.localPoliceService,
    contrabandDetail = fullyPopulatedUpdateRecallRequest.contrabandDetail,
    vulnerabilityDiversityDetail = fullyPopulatedUpdateRecallRequest.vulnerabilityDiversityDetail,
    mappaLevel = fullyPopulatedUpdateRecallRequest.mappaLevel,
    sentencingInfo = SentencingInfo(
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
  )

  private val fullyPopulatedRecallResponse = RecallResponse(
    recallId, nomsNumber, documents = emptyList(),
    agreeWithRecallRecommendation = fullyPopulatedUpdateRecallRequest.agreeWithRecallRecommendation,
    recallLength = fullyPopulatedUpdateRecallRequest.recallLength,
    recallEmailReceivedDateTime = fullyPopulatedUpdateRecallRequest.recallEmailReceivedDateTime,
    lastReleasePrison = fullyPopulatedUpdateRecallRequest.lastReleasePrison,
    lastReleaseDate = fullyPopulatedUpdateRecallRequest.lastReleaseDate,
    localPoliceService = fullyPopulatedUpdateRecallRequest.localPoliceService,
    contrabandDetail = fullyPopulatedUpdateRecallRequest.contrabandDetail,
    vulnerabilityDiversityDetail = fullyPopulatedUpdateRecallRequest.vulnerabilityDiversityDetail,
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
    )
  )

  @Test
  fun `can update recall all fields populated`() {
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
}
