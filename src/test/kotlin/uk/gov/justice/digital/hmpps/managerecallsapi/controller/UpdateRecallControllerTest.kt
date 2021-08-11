package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.http.ResponseEntity
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength.FOURTEEN_DAYS
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength.TWENTY_EIGHT_DAYS
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType.FIXED
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.random.Random

class UpdateRecallControllerTest {
  // TODO:  MD Investigate using in memory RecallRepository, might be much cleaner throughout
  private val recallRepository = mockk<RecallRepository>()
  private val underTest = UpdateRecallController(recallRepository)

  private val nomsNumber = NomsNumber("A9876ZZ")
  private val recallLength = FOURTEEN_DAYS

  @Test
  fun `can update recall with recall type and length`() {
    val recallId = ::RecallId.random()
    val priorRecall = Recall(recallId, nomsNumber)
    every { recallRepository.getByRecallId(recallId) } returns priorRecall
    val updatedRecall = priorRecall.copy(recallType = FIXED, recallLength = recallLength)
    every { recallRepository.save(updatedRecall) } returns updatedRecall

    val updateRecallRequest = UpdateRecallRequest(recallLength = recallLength)
    val response = underTest.updateRecall(recallId, updateRecallRequest)

    assertThat(
      response,
      equalTo(
        ResponseEntity.ok(
          RecallResponse(recallId, nomsNumber, documents = emptyList(), recallLength = recallLength)
        )
      )
    )
  }

  @Test
  fun `can update recall with agreeWithRecallRecommendation without changing recallLength`() {
    val recallId = ::RecallId.random()
    val priorRecall = Recall(recallId, nomsNumber, recallLength = TWENTY_EIGHT_DAYS)
    every { recallRepository.getByRecallId(recallId) } returns priorRecall
    val updatedRecall = priorRecall.copy(recallType = FIXED, agreeWithRecallRecommendation = true)
    every { recallRepository.save(updatedRecall) } returns updatedRecall

    val noLengthRecall = updatedRecall.copy(recallLength = null)
    verify { recallRepository.save(noLengthRecall) wasNot Called }

    val updateRecallRequest = UpdateRecallRequest(null, true)
    val response = underTest.updateRecall(recallId, updateRecallRequest)

    assertThat(
      response,
      equalTo(
        ResponseEntity.ok(
          RecallResponse(recallId, nomsNumber, emptyList(), agreeWithRecallRecommendation = true, recallLength = TWENTY_EIGHT_DAYS)
        )
      )
    )
  }

  @Test
  fun `can update recall with recallEmailReceivedDateTime without changing other properties`() {
    val recallId = ::RecallId.random()
    val priorRecall = Recall(recallId, nomsNumber, recallLength = FOURTEEN_DAYS, agreeWithRecallRecommendation = false)
    every { recallRepository.getByRecallId(recallId) } returns priorRecall
    val recallEmailReceivedDateTime = ZonedDateTime.now()
    val updatedRecall = priorRecall.copy(recallType = FIXED, recallEmailReceivedDateTime = recallEmailReceivedDateTime)
    every { recallRepository.save(updatedRecall) } returns updatedRecall

    val response = underTest.updateRecall(recallId, UpdateRecallRequest(recallEmailReceivedDateTime = recallEmailReceivedDateTime))

    assertThat(
      response,
      equalTo(
        ResponseEntity.ok(
          RecallResponse(
            recallId, nomsNumber, emptyList(),
            agreeWithRecallRecommendation = false,
            recallLength = FOURTEEN_DAYS,
            recallEmailReceivedDateTime = recallEmailReceivedDateTime
          ),
        )
      )
    )
  }

  @Test
  fun `can update recall with localPoliceService without changing other properties`() {
    val recallId = ::RecallId.random()
    val priorRecall = Recall(recallId, nomsNumber, recallLength = FOURTEEN_DAYS, agreeWithRecallRecommendation = false)
    every { recallRepository.getByRecallId(recallId) } returns priorRecall
    val localPoliceService = "London"
    val updatedRecall = priorRecall.copy(recallType = FIXED, localPoliceService = localPoliceService)
    every { recallRepository.save(updatedRecall) } returns updatedRecall

    val response = underTest.updateRecall(recallId, UpdateRecallRequest(localPoliceService = localPoliceService))

    assertThat(
      response,
      equalTo(
        ResponseEntity.ok(
          RecallResponse(
            recallId, nomsNumber, emptyList(),
            agreeWithRecallRecommendation = false,
            recallLength = FOURTEEN_DAYS,
            localPoliceService = localPoliceService
          ),
        )
      )
    )
  }

  @Test
  fun `can update recall with all update payload properties simultaneously`() {
    val recallId = ::RecallId.random()
    val priorRecall = Recall(recallId, nomsNumber)
    every { recallRepository.getByRecallId(recallId) } returns priorRecall

    val newRecallEmailReceivedDateTime = ZonedDateTime.now()
    val newRecallLength = TWENTY_EIGHT_DAYS
    val newAgreeWithRecallRecommendation = true

    val updatedRecall = priorRecall.copy(
      recallType = FIXED,
      recallLength = newRecallLength,
      agreeWithRecallRecommendation = newAgreeWithRecallRecommendation,
      recallEmailReceivedDateTime = newRecallEmailReceivedDateTime
    )
    every { recallRepository.save(updatedRecall) } returns updatedRecall

    val response = underTest.updateRecall(
      recallId,
      UpdateRecallRequest(
        agreeWithRecallRecommendation = newAgreeWithRecallRecommendation,
        recallLength = newRecallLength,
        recallEmailReceivedDateTime = newRecallEmailReceivedDateTime
      )
    )

    assertThat(
      response,
      equalTo(
        ResponseEntity.ok(
          RecallResponse(
            recallId, nomsNumber, emptyList(),
            agreeWithRecallRecommendation = newAgreeWithRecallRecommendation,
            recallLength = newRecallLength,
            recallEmailReceivedDateTime = newRecallEmailReceivedDateTime
          ),
        )
      )
    )
  }

  @Test
  fun `cannot reset recall properties to null with update recall`() {
    val recallId = ::RecallId.random()
    val priorRecallEmailReceivedDateTime = ZonedDateTime.now()
    val priorRecallLength = TWENTY_EIGHT_DAYS
    val priorAgreeWithRecallRecommendation = true

    val priorRecall = Recall(
      recallId, nomsNumber,
      recallLength = priorRecallLength,
      agreeWithRecallRecommendation = priorAgreeWithRecallRecommendation,
      recallEmailReceivedDateTime = priorRecallEmailReceivedDateTime
    )
    every { recallRepository.getByRecallId(recallId) } returns priorRecall

    val updatedRecall = priorRecall.copy(recallType = FIXED)
    every { recallRepository.save(updatedRecall) } returns updatedRecall

    val response = underTest.updateRecall(
      recallId,
      UpdateRecallRequest(
        agreeWithRecallRecommendation = null,
        recallLength = null,
        recallEmailReceivedDateTime = null
      )
    )

    assertThat(response, equalTo(ResponseEntity.ok(updatedRecall.toResponse())))
  }

  @Test
  fun `can update recall with lastReleaseDateTime and lastReleasePrison without changing other properties, for example agreeWithRecallRecommendation`() {
    val recallId = ::RecallId.random()
    val agreeWithRecallRecommendation = Random.nextBoolean()
    val priorRecall = Recall(recallId, nomsNumber, recallLength = FOURTEEN_DAYS, agreeWithRecallRecommendation = agreeWithRecallRecommendation)
    every { recallRepository.getByRecallId(recallId) } returns priorRecall
    val lastReleaseDateTime = ZonedDateTime.now()
    val lastReleasePrison = UUID.randomUUID().toString()
    val updatedRecall = priorRecall.copy(recallType = FIXED, lastReleasePrison = lastReleasePrison, lastReleaseDateTime = lastReleaseDateTime)
    every { recallRepository.save(updatedRecall) } returns updatedRecall

    val response = underTest.updateRecall(recallId, UpdateRecallRequest(lastReleasePrison = lastReleasePrison, lastReleaseDateTime = lastReleaseDateTime))

    assertThat(
      response,
      equalTo(
        ResponseEntity.ok(
          RecallResponse(
            recallId, nomsNumber, emptyList(),
            agreeWithRecallRecommendation = agreeWithRecallRecommendation,
            recallLength = FOURTEEN_DAYS,
            lastReleasePrison = lastReleasePrison,
            lastReleaseDateTime = lastReleaseDateTime
          ),
        )
      )
    )
  }
}
