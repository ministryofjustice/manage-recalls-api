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
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random

class UpdateRecallControllerTest {
  // TODO:  MD Investigate using in memory RecallRepository, might be much cleaner throughout
  private val recallRepository = mockk<RecallRepository>()
  private val underTest = UpdateRecallController(recallRepository)

  private val recallId = ::RecallId.random()
  private val nomsNumber = NomsNumber("A9876ZZ")
  private val recallLength = FOURTEEN_DAYS
  private val updateRecallRequest = UpdateRecallRequest(recallLength)

  @Test
  fun `can update recall with recall type and length`() {
    every { recallRepository.getByRecallId(recallId) } returns Recall(recallId, nomsNumber)
    val updatedRecall = Recall(recallId, nomsNumber, recallType = FIXED, recallLength = recallLength)
    every { recallRepository.save(updatedRecall) } returns updatedRecall

    val response = underTest.updateRecall(recallId, updateRecallRequest)

    assertThat(
      response,
      equalTo(
        ResponseEntity.ok(
          RecallResponse(recallId, nomsNumber, null, emptyList(), recallLength)
        )
      )
    )
  }
}
