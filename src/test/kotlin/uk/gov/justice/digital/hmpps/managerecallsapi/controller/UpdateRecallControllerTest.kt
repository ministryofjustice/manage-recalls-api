package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.http.ResponseEntity
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.service.PrisonValidationService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.UpdateRecallService

class UpdateRecallControllerTest {
  private val updateRecallService = mockk<UpdateRecallService>()
  private val prisonValidateService = mockk<PrisonValidationService>()
  private val underTest = UpdateRecallController(updateRecallService, prisonValidateService)

  private val recallId = ::RecallId.random()
  private val nomsNumber = NomsNumber("A9876ZZ")

  private val updateRecallRequest = UpdateRecallRequest(lastReleasePrison = PrisonId("ABC"), currentPrison = PrisonId("DEF"))

  private val recall = Recall(recallId, nomsNumber)

  @Test
  fun `can update recall and return a response with all fields populated`() {
    every { prisonValidateService.isPrisonValidAndActive(updateRecallRequest.currentPrison) } returns true
    every { prisonValidateService.isPrisonValid(updateRecallRequest.lastReleasePrison) } returns true
    every { updateRecallService.updateRecall(recallId, updateRecallRequest) } returns recall

    val response = underTest.updateRecall(recallId, updateRecallRequest)

    assertThat(response, equalTo(ResponseEntity.ok(recall.toResponse())))
  }

  @Test
  fun `can't update recall when prison is not valid`() {
    every { prisonValidateService.isPrisonValidAndActive(updateRecallRequest.currentPrison) } returns false
    every { updateRecallService.updateRecall(recallId, updateRecallRequest) } returns recall

    val response = underTest.updateRecall(recallId, updateRecallRequest)

    assertThat(response, equalTo(ResponseEntity.badRequest().build()))
  }

  @Test
  fun `can't update recall when last release prison is not valid`() {
    every { prisonValidateService.isPrisonValid(updateRecallRequest.lastReleasePrison) } returns false
    every { prisonValidateService.isPrisonValidAndActive(updateRecallRequest.currentPrison) } returns true
    every { updateRecallService.updateRecall(recallId, updateRecallRequest) } returns recall

    val response = underTest.updateRecall(recallId, updateRecallRequest)

    assertThat(response, equalTo(ResponseEntity.badRequest().build()))
  }
}
