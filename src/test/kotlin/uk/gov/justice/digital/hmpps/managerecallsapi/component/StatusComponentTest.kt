package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.BookRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Status
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UpdateRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CroNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.WarrantReferenceNumber
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class StatusComponentTest : ComponentTestBase() {
  @MockkBean
  private lateinit var fixedClock: Clock

  private val nomsNumber = NomsNumber("123456")
  private val zone = ZoneId.of("UTC")
  private lateinit var fixedClockTime: OffsetDateTime

  @BeforeEach
  fun setUpFixedClock() {
    val instant = Instant.parse("2021-10-04T13:15:50.00Z")
    fixedClockTime = OffsetDateTime.ofInstant(instant, zone)
    every { fixedClock.instant() } returns instant
    every { fixedClock.zone } returns zone
  }

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
  }
}
