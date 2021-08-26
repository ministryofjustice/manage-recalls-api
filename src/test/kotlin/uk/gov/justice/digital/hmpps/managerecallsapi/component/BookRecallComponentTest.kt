package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.allOf
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.present
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.BAD_REQUEST
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.BookRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber

class BookRecallComponentTest : ComponentTestBase() {

  @Test
  fun `book a recall`() {
    val nomsNumber = NomsNumber("123456")
    val response = authenticatedPostRequest("/recalls", BookRecallRequest(nomsNumber))

    assertThat(
      response,
      allOf(
        has(RecallResponse::recallId, present()),
        has(RecallResponse::nomsNumber, equalTo(nomsNumber)),
      )
    )
  }

  @Test
  fun `book a recall with blank nomsNumber returns 400`() {
    authenticatedPostRequest("/recalls", "{\"nomsNumber\":\"\"}", BAD_REQUEST)
  }
}
