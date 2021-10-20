package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import kotlin.jvm.Throws

class AssignRecallComponentTest : ComponentTestBase() {

  @Test
  fun `assign recall`() {
    val recallId = ::RecallId.random()
    val nomsNumber = NomsNumber("123456")
    val assignee = ::UserId.random()

    val recall = Recall(recallId, nomsNumber)
    recallRepository.save(recall)

    val response = authenticatedClient.assignRecall(recallId, assignee)

    assertThat(
      response, equalTo(RecallResponse(recallId, nomsNumber, assignee = assignee))
    )
  }

  @Test
  fun `unassign recall`() {
    val recallId = ::RecallId.random()
    val nomsNumber = NomsNumber("123456")
    val assignee = ::UserId.random()

    val recall = Recall(recallId, nomsNumber, assignee = assignee)
    recallRepository.save(recall)

    val response = authenticatedClient.unassignRecall(recallId, assignee)

    assertThat(
      response, equalTo(RecallResponse(recallId, nomsNumber))
    )
  }

  @Test
  fun `unassign recall with wrong assignee throws 404`() {
    val recallId = ::RecallId.random()
    val nomsNumber = NomsNumber("123456")
    val assignee = ::UserId.random()
    val otherAssignee = ::UserId.random()

    val recall = Recall(recallId, nomsNumber, assignee = assignee)
    recallRepository.save(recall)

    authenticatedClient.unassignRecall(recallId, otherAssignee, HttpStatus.NOT_FOUND)
  }

  @Test
  @Throws(MethodArgumentTypeMismatchException::class)
  fun `must pass a valid uuid as assignee`() {
    val recallId = ::RecallId.random()

    authenticatedClient.delete("/recalls/$recallId/assignee/notauuid", INTERNAL_SERVER_ERROR)
    authenticatedClient.post("/recalls/$recallId/assignee/notauuid", "").expectStatus().isEqualTo(INTERNAL_SERVER_ERROR)
  }
}
