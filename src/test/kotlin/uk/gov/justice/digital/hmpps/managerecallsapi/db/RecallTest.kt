package uk.gov.justice.digital.hmpps.managerecallsapi.db

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.NameFormatCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.NameFormatCategory.FIRST_MIDDLE_LAST
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.NameFormatCategory.OTHER
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType.FIXED
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType.STANDARD
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Status
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CroNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FullName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.MiddleNames
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.WarrantReferenceNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomString
import java.time.LocalDate
import java.time.OffsetDateTime

class RecallTest {

  private val recall = Recall(
    ::RecallId.random(),
    NomsNumber("A12345AA"),
    ::UserId.random(),
    OffsetDateTime.now(),
    FirstName("Barrie"),
    MiddleNames("Barnie"),
    LastName("Badger"),
    CroNumber("ABC/1234A"),
    LocalDate.of(1999, 12, 1),
    licenceNameCategory = NameFormatCategory.FIRST_LAST,
  )

  @Test
  fun `Recall without assessedByUserId or bookedByUserId set returns BEING_BOOKED_ON status`() {
    assertThat(recall.status(), equalTo(Status.BEING_BOOKED_ON))
  }

  @Test
  fun `Recall with bookedByUserId set but without assessedByUserId set returns status BOOKED_ON`() {
    val recall = recall.copy(
      bookedByUserId = ::UserId.random().value
    )
    assertThat(recall.status(), equalTo(Status.BOOKED_ON))
  }

  @Test
  fun `Recall with bookedByUserId and assignee set but without assessedByUserId set returns status IN_ASSESSMENT`() {
    val recall = recall.copy(
      bookedByUserId = ::UserId.random().value,
      assignee = ::UserId.random().value
    )
    assertThat(recall.status(), equalTo(Status.IN_ASSESSMENT))
  }

  @Test
  fun `In Custody Recall with assessedByUserId set returns status AWAITING_DOSSIER_CREATION`() {
    val recall = recall.copy(
      inCustodyAtBooking = true,
      assessedByUserId = ::UserId.random().value,
    )

    assertThat(recall.status(), equalTo(Status.AWAITING_DOSSIER_CREATION))
  }

  @Test
  fun `NOT In Custody Recall with assessedByUserId set returns status ASSESSED_NOT_IN_CUSTODY`() {
    val recall = recall.copy(
      inCustodyAtBooking = false,
      inCustodyAtAssessment = false,
      assessedByUserId = ::UserId.random().value,
    )

    assertThat(recall.status(), equalTo(Status.ASSESSED_NOT_IN_CUSTODY))
  }

  @Test
  fun `NOT in Custody Recall with bookedByUserId, assessedByUserId & warrantReferenceNumber set returns status AWAITING_RETURN_TO_CUSTODY`() {
    val recall = recall.copy(
      inCustodyAtBooking = false,
      inCustodyAtAssessment = false,
      bookedByUserId = ::UserId.random().value,
      assessedByUserId = ::UserId.random().value,
      warrantReferenceNumber = WarrantReferenceNumber(randomString())
    )

    assertThat(recall.status(), equalTo(Status.AWAITING_RETURN_TO_CUSTODY))
  }

  @Test
  fun `NOT in Custody Recall with bookedByUserId, assessedByUserId & warrantReferenceNumber and returnedToCustody set returns status AWAITING_DOSSIER_CREATION`() {
    val recall = recall.copy(
      inCustodyAtBooking = false,
      inCustodyAtAssessment = false,
      bookedByUserId = ::UserId.random().value,
      assessedByUserId = ::UserId.random().value,
      warrantReferenceNumber = WarrantReferenceNumber(randomString()),
      returnedToCustody = ReturnedToCustodyRecord(
        OffsetDateTime.now(),
        OffsetDateTime.now(),
        ::UserId.random(),
        OffsetDateTime.now()
      )
    )

    assertThat(recall.status(), equalTo(Status.AWAITING_DOSSIER_CREATION))
  }

  @Test
  fun `In Custody Recall with bookedByUserId, assessedByUserId ignores warrantReferenceNumber and returns status AWAITING_DOSSIER_CREATION`() {
    val recall = recall.copy(
      inCustodyAtBooking = true,
      bookedByUserId = ::UserId.random().value,
      assessedByUserId = ::UserId.random().value,
      warrantReferenceNumber = WarrantReferenceNumber(randomString())
    )

    assertThat(recall.status(), equalTo(Status.AWAITING_DOSSIER_CREATION))
  }

  @Test
  fun `In Custody Recall with bookedByUserId and assessedByUserId but no assignee returns status AWAITING_DOSSIER_CREATION`() {
    val recall = recall.copy(
      inCustodyAtBooking = true,
      bookedByUserId = ::UserId.random().value,
      assessedByUserId = ::UserId.random().value,
    )

    assertThat(recall.status(), equalTo(Status.AWAITING_DOSSIER_CREATION))
  }

  @Test
  fun `In Custody Recall with bookedByUserId, assessedByUserId & assignee set returns status DOSSIER_IN_PROGRESS`() {
    val recall = recall.copy(
      inCustodyAtBooking = true,
      bookedByUserId = ::UserId.random().value,
      assessedByUserId = ::UserId.random().value,
      assignee = ::UserId.random().value
    )

    assertThat(recall.status(), equalTo(Status.DOSSIER_IN_PROGRESS))
  }

  @Test
  fun `NOT In Custody Recall with bookedByUserId, assessedByUserId, returnedToCustody & assignee set returns status DOSSIER_IN_PROGRESS`() {
    val recall = recall.copy(
      inCustodyAtAssessment = false,
      bookedByUserId = ::UserId.random().value,
      assessedByUserId = ::UserId.random().value,
      assignee = ::UserId.random().value,
      returnedToCustody = ReturnedToCustodyRecord(
        OffsetDateTime.now(),
        OffsetDateTime.now(),
        ::UserId.random(),
        OffsetDateTime.now()
      ),
    )

    assertThat(recall.status(), equalTo(Status.DOSSIER_IN_PROGRESS))
  }

  @Test
  fun `Fixed term recall with dossierCreatedByUserId set returns status DOSSIER_ISSUED`() {
    val recall = recall.copy(
      dossierCreatedByUserId = ::UserId.random().value,
      recommendedRecallType = FIXED,
      confirmedRecallType = FIXED
    )

    assertThat(recall.status(), equalTo(Status.DOSSIER_ISSUED))
  }

  @Test
  fun `Fixed term Recall with bookedByUserId, assessedByUserId, dossierCreatedByUserId and assignee set returns status DOSSIER_ISSUED`() {
    val recall = recall.copy(
      bookedByUserId = ::UserId.random().value,
      assessedByUserId = ::UserId.random().value,
      assignee = ::UserId.random().value,
      dossierCreatedByUserId = ::UserId.random().value,
      recommendedRecallType = FIXED,
      confirmedRecallType = FIXED
    )

    assertThat(recall.status(), equalTo(Status.DOSSIER_ISSUED))
  }

  @Test
  fun `Fixed term Recall with bookedByUserId, assessedByUserId and dossierCreatedByUserId set returns status DOSSIER_ISSUED`() {
    val recall = recall.copy(
      bookedByUserId = ::UserId.random().value,
      assessedByUserId = ::UserId.random().value,
      dossierCreatedByUserId = ::UserId.random().value,
      recommendedRecallType = FIXED,
      confirmedRecallType = FIXED
    )

    assertThat(recall.status(), equalTo(Status.DOSSIER_ISSUED))
  }

  @Test
  fun `Standard recall with bookedByUserId, assessedByUserId and dossierCreatedByUserId set returns status AWAITING_PART_B`() {
    val recall = recall.copy(
      bookedByUserId = ::UserId.random().value,
      assessedByUserId = ::UserId.random().value,
      dossierCreatedByUserId = ::UserId.random().value,
      recommendedRecallType = STANDARD,
      confirmedRecallType = STANDARD
    )

    assertThat(recall.status(), equalTo(Status.AWAITING_PART_B))
  }

  @Test
  fun `Standard recall with bookedByUserId, assessedByUserId, dossierCreatedByUserId set and rereleaseSupported = true returns status AWAITING_PART_B`() {
    val recall = recall.copy(
      bookedByUserId = ::UserId.random().value,
      assessedByUserId = ::UserId.random().value,
      dossierCreatedByUserId = ::UserId.random().value,
      recommendedRecallType = STANDARD,
      confirmedRecallType = STANDARD,
      rereleaseSupported = true
    )

    assertThat(recall.status(), equalTo(Status.AWAITING_PART_B))
  }

  @Test
  fun `Standard recall with bookedByUserId, assessedByUserId, dossierCreatedByUserId set and rereleaseSupported = false returns status AWAITING_SECONDARY_DOSSIER_CREATION`() {
    val recall = recall.copy(
      bookedByUserId = ::UserId.random().value,
      assessedByUserId = ::UserId.random().value,
      dossierCreatedByUserId = ::UserId.random().value,
      recommendedRecallType = STANDARD,
      confirmedRecallType = STANDARD,
      rereleaseSupported = false
    )

    assertThat(recall.status(), equalTo(Status.AWAITING_SECONDARY_DOSSIER_CREATION))
  }

  @Test
  fun `Recall with recallEmailReceivedDateTime set returns recallAssessmentDueDateTime as 24 hours later`() {
    val dateTime = OffsetDateTime.now()
    val recall = recall.copy(
      recallEmailReceivedDateTime = dateTime
    )

    assertThat(recall.recallAssessmentDueDateTime(), equalTo(dateTime.plusHours(24)))
  }

  @Test
  fun `Recall without recallEmailReceivedDateTime set returns recallAssessmentDueDateTime as null`() {
    assertThat(recall.recallAssessmentDueDateTime(), equalTo(null))
  }

  @Test
  fun `Recall with FIRST_LAST licenceNameCategory returns correctly formatted name`() {
    assertThat(recall.prisonerNameOnLicence(), equalTo(FullName("Barrie Badger")))
  }

  @Test
  fun `Recall with FIRST_MIDDLE_LAST licenceNameCategory returns correctly formatted name`() {
    assertThat(recall.copy(licenceNameCategory = FIRST_MIDDLE_LAST).prisonerNameOnLicence(), equalTo(FullName("Barrie Barnie Badger")))
  }

  @Test
  fun `Recall with FIRST_MIDDLE_LAST licenceNameCategory but no middle name returns correctly formatted name`() {
    assertThat(recall.copy(middleNames = null, licenceNameCategory = FIRST_MIDDLE_LAST).prisonerNameOnLicence(), equalTo(FullName("Barrie Badger")))
  }

  @Test
  fun `Recall with OTHER licenceNameCategory throws exception`() {
    assertThrows<IllegalStateException> {
      recall.copy(licenceNameCategory = OTHER).prisonerNameOnLicence()
    }
  }

  @Test
  fun `Recall with recommendedRecallType only returns recommendedRecallType as recallType`() {
    val recall = recall.copy(
      recommendedRecallType = STANDARD
    )

    assertThat(recall.recallType(), equalTo(STANDARD))
  }

  @Test
  fun `Recall with recommendedRecallType and confirmedRecallType returns confirmedRecallType as recallType`() {
    val recall = recall.copy(
      recommendedRecallType = STANDARD,
      confirmedRecallType = FIXED
    )

    assertThat(recall.recallType(), equalTo(FIXED))
  }
}
