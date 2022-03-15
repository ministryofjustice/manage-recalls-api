package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.allOf
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.present
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.BookRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.NameFormatCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CroNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.MiddleNames
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import java.time.LocalDate

class BookRecallComponentTest : ComponentTestBase() {

  @Test
  fun `book a recall without middle names sets the licenceNameCategory to FIRST_LAST`() {
    val nomsNumber = NomsNumber("123456")
    val response = authenticatedClient.bookRecall(
      BookRecallRequest(
        nomsNumber,
        FirstName("Bobby"),
        null,
        LastName("Badger"),
        CroNumber("1234/56A"),
        LocalDate.now()
      )
    )

    assertThat(
      response,
      allOf(
        has(RecallResponse::recallId, present()),
        has(RecallResponse::createdByUserId, equalTo(authenticatedClient.userId)),
        has(RecallResponse::nomsNumber, equalTo(nomsNumber)),
        has(RecallResponse::createdDateTime, present()),
        has(RecallResponse::lastUpdatedDateTime, present()),
        has(RecallResponse::licenceNameCategory, equalTo(NameFormatCategory.FIRST_LAST)),
      )
    )
  }

  @Test
  fun `book a recall without a middle name doesnt set licenceNameCategory`() {
    val nomsNumber = NomsNumber("123456")
    val response = authenticatedClient.bookRecall(
      BookRecallRequest(
        nomsNumber,
        FirstName("Bobby"),
        MiddleNames("Barrie"),
        LastName("Badger"),
        CroNumber("1234/56A"),
        LocalDate.now()
      )
    )

    assertThat(
      response,
      allOf(
        has(RecallResponse::recallId, present()),
        has(RecallResponse::createdByUserId, equalTo(authenticatedClient.userId)),
        has(RecallResponse::nomsNumber, equalTo(nomsNumber)),
        has(RecallResponse::createdDateTime, present()),
        has(RecallResponse::lastUpdatedDateTime, present()),
        has(RecallResponse::licenceNameCategory, absent()),
      )
    )
  }

  @Test
  fun `book a recall with blank nomsNumber returns 400`() {
    authenticatedClient.post("/recalls", "{\"nomsNumber\":\"\"}\"}")
      .expectStatus().isBadRequest
  }
}
