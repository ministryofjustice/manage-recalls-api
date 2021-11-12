package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.allOf
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.present
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.BookRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber

class BookRecallComponentTest : ComponentTestBase() {

  @Test
  fun `book a recall creates a recall and returns recall details`() {
    val nomsNumber = NomsNumber("123456")
    val response = authenticatedClient.bookRecall(BookRecallRequest(nomsNumber))

    assertThat(
      response,
      allOf(
        has(RecallResponse::recallId, present()),
        has(RecallResponse::createdByUserId, equalTo(authenticatedClient.userId)),
        has(RecallResponse::nomsNumber, equalTo(nomsNumber)),
        has(RecallResponse::createdDateTime, present()),
        has(RecallResponse::lastUpdatedDateTime, present()),
      )
    )
  }

  @Test
  fun `book a recall with blank nomsNumber returns 400`() {
    authenticatedClient.post("/recalls", "{\"nomsNumber\":\"\"}\"}")
      .expectStatus().isBadRequest
  }
}
