package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.BookRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Status
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UpdateRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CroNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.WarrantReferenceNumber
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

class StatusComponentTest : ComponentTestBase() {

  private val nomsNumber = NomsNumber("123456")

  @Test
  fun `not in custody recall with returnedToCustody set returns status of AWAITING_DOSSIER_CREATION`() {
    val recall = authenticatedClient.bookRecall(
      BookRecallRequest(
        nomsNumber,
        FirstName("Barrie"),
        null,
        LastName("Badger"),
        CroNumber("ABC/1234A"),
        LocalDate.of(1999, 12, 1),
      )
    )
    val updatedRecallResponse = authenticatedClient.updateRecall(
      recall.recallId,
      UpdateRecallRequest(
        assessedByUserId = authenticatedClient.userId,
        inCustodyAtBooking = false,
        inCustodyAtAssessment = false,
        warrantReferenceNumber = WarrantReferenceNumber("ABC/1234567890Z")
      )
    )
    assertThat(updatedRecallResponse.status, equalTo(Status.AWAITING_RETURN_TO_CUSTODY))

    authenticatedClient.returnedToCustody(recall.recallId, OffsetDateTime.now().minusHours(6), OffsetDateTime.of(2022, 2, 14, 10, 11, 12, 123, ZoneOffset.UTC))

    val rtcRecall = authenticatedClient.getRecall(recall.recallId)
    assertThat(rtcRecall.status, equalTo(Status.AWAITING_DOSSIER_CREATION))
    assertThat(rtcRecall.dossierTargetDate, equalTo(LocalDate.of(2022, 2, 15)))
    assertOffsetDateTimesEqual(rtcRecall.lastUpdatedDateTime, OffsetDateTime.now(fixedClock))
  }
}
