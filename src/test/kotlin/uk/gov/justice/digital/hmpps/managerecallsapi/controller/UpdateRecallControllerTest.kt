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
          RecallResponse(recallId, nomsNumber, null, emptyList(), true, TWENTY_EIGHT_DAYS)
        )
      )
    )
  }
}
