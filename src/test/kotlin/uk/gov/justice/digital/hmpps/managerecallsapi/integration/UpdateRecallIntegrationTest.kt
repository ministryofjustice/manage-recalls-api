package uk.gov.justice.digital.hmpps.managerecallsapi.integration

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength.TWENTY_EIGHT_DAYS
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType.FIXED
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UpdateRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallNotFoundException

class UpdateRecallIntegrationTest : IntegrationTestBase() {

  @MockkBean
  private lateinit var recallRepository: RecallRepository

  private val nomsNumber = NomsNumber("123456")
  private val recallId = ::RecallId.random()
  private val recallLength = TWENTY_EIGHT_DAYS

  @Test
  fun `update a recall returns updated recall`() {
    every { recallRepository.getByRecallId(recallId) } returns Recall(recallId, nomsNumber)
    val expectedRecall = Recall(recallId, nomsNumber, recallType = FIXED, recallLength = recallLength)
    every { recallRepository.save(expectedRecall) } returns expectedRecall

    val response = authenticatedPatchRequest("/recalls/$recallId", UpdateRecallRequest(recallLength))

    assertThat(
      response,
      equalTo(RecallResponse(recallId, nomsNumber, null, emptyList(), null))
    )
  }

  @Test
  fun `update a recall with no recall length returns 400`() {
    sendAuthenticatedPatchRequestWithBody("/recalls/$recallId", "{\"recallLength\":\"\"}")
      .expectStatus().isBadRequest
  }

  @Test
  fun `update a recall that does not exist returns 404`() {
    every { recallRepository.getByRecallId(recallId) } throws RecallNotFoundException("blah", Exception())

    sendAuthenticatedPatchRequestWithBody("/recalls/$recallId", UpdateRecallRequest(recallLength))
      .expectStatus().isNotFound
  }

  private fun authenticatedPatchRequest(path: String, request: Any): RecallResponse =
    sendAuthenticatedPatchRequestWithBody(path, request)
      .expectStatus().isOk
      .expectBody(RecallResponse::class.java)
      .returnResult()
      .responseBody!!
}
