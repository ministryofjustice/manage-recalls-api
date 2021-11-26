package uk.gov.justice.digital.hmpps.managerecallsapi.db

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.AgreeWithRecall
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.NameFormatCategory.FIRST_MIDDLE_LAST
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.NameFormatCategory.OTHER
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Status
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FullName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.MiddleNames
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import java.time.OffsetDateTime

class RecallTest {

  private val recall = Recall(
    ::RecallId.random(), NomsNumber("A12345AA"), ::UserId.random(), OffsetDateTime.now(), FirstName("Barrie"), MiddleNames("Barnie"), LastName("Badger")
  )

  @Test
  fun `Recall without recallNotificationEmailSentDateTime or bookedByUserId set returns BEING_BOOKED_ON status`() {
    assertThat(recall.status(), equalTo(Status.BEING_BOOKED_ON))
  }

  @Test
  fun `Recall with bookedByUserId set but without recallNotificationEmailSentDateTime set returns status BOOKED_ON`() {
    val recall = recall.copy(
      bookedByUserId = ::UserId.random().value
    )
    assertThat(recall.status(), equalTo(Status.BOOKED_ON))
  }

  @Test
  fun `Recall with bookedByUserId and assignee set but without recallNotificationEmailSentDateTime set returns status IN_ASSESSMENT`() {
    val recall = recall.copy(
      bookedByUserId = ::UserId.random().value,
      assignee = ::UserId.random().value
    )
    assertThat(recall.status(), equalTo(Status.IN_ASSESSMENT))
  }

  @Test
  fun `Recall with bookedByUserId and assignee set and AgreeWithRecall=NO_STOP returns status STOPPED`() {
    val recall = recall.copy(
      bookedByUserId = ::UserId.random().value,
      assignee = ::UserId.random().value,
      agreeWithRecall = AgreeWithRecall.NO_STOP
    )

    assertThat(recall.status(), equalTo(Status.STOPPED))
  }

  @Test
  fun `Recall with bookedByUserId set, assignee not set and AgreeWithRecall=NO_STOP returns status STOPPED`() {
    val recall = recall.copy(
      bookedByUserId = ::UserId.random().value,
      assignee = ::UserId.random().value,
      agreeWithRecall = AgreeWithRecall.NO_STOP
    )

    assertThat(recall.status(), equalTo(Status.STOPPED))
  }

  @Test
  fun `Recall with bookedByUserId and assignee set, but with AgreeWithRecall=YES returns status IN_ASSESSMENT`() {
    val recall = recall.copy(
      bookedByUserId = ::UserId.random().value,
      assignee = ::UserId.random().value,
      agreeWithRecall = AgreeWithRecall.YES
    )

    assertThat(recall.status(), equalTo(Status.IN_ASSESSMENT))
  }

  @Test
  fun `Recall with recallNotificationEmailSentDateTime set returns status RECALL_NOTIFICATION_ISSUED`() {
    val recall = recall.copy(
      recallNotificationEmailSentDateTime = OffsetDateTime.now()
    )

    assertThat(recall.status(), equalTo(Status.RECALL_NOTIFICATION_ISSUED))
  }

  @Test
  fun `Recall with bookedByUserId and recallNotificationEmailSentDateTime returns status RECALL_NOTIFICATION_ISSUED`() {
    val recall = recall.copy(
      bookedByUserId = ::UserId.random().value,
      recallNotificationEmailSentDateTime = OffsetDateTime.now()
    )

    assertThat(recall.status(), equalTo(Status.RECALL_NOTIFICATION_ISSUED))
  }

  @Test
  fun `Recall with bookedByUserId, recallNotificationEmailSentDateTime & assignee set returns status DOSSIER_IN_PROGRESS`() {
    val recall = recall.copy(
      bookedByUserId = ::UserId.random().value,
      recallNotificationEmailSentDateTime = OffsetDateTime.now(),
      assignee = ::UserId.random().value
    )

    assertThat(recall.status(), equalTo(Status.DOSSIER_IN_PROGRESS))
  }

  @Test
  fun `Recall with dossierCreatedByUserId set returns status DOSSIER_ISSUED`() {
    val recall = recall.copy(
      dossierCreatedByUserId = ::UserId.random().value
    )

    assertThat(recall.status(), equalTo(Status.DOSSIER_ISSUED))
  }

  @Test
  fun `Recall with bookedByUserId, recallNotificationEmailSentDateTime, dossierCreatedByUserId and assignee set returns status DOSSIER_ISSUED`() {
    val recall = recall.copy(
      bookedByUserId = ::UserId.random().value,
      recallNotificationEmailSentDateTime = OffsetDateTime.now(),
      assignee = ::UserId.random().value,
      dossierCreatedByUserId = ::UserId.random().value
    )

    assertThat(recall.status(), equalTo(Status.DOSSIER_ISSUED))
  }

  @Test
  fun `Recall with bookedByUserId, recallNotificationEmailSentDateTime and dossierCreatedByUserId set returns status DOSSIER_ISSUED`() {
    val recall = recall.copy(
      bookedByUserId = ::UserId.random().value,
      recallNotificationEmailSentDateTime = OffsetDateTime.now(),
      dossierCreatedByUserId = ::UserId.random().value,
    )

    assertThat(recall.status(), equalTo(Status.DOSSIER_ISSUED))
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
    assertThat(recall.prisonerNameOnLicense(), equalTo(FullName("Barrie Badger")))
  }

  @Test
  fun `Recall with FIRST_MIDDLE_LAST licenceNameCategory returns correctly formatted name`() {
    assertThat(recall.copy(licenceNameCategory = FIRST_MIDDLE_LAST).prisonerNameOnLicense(), equalTo(FullName("Barrie Barnie Badger")))
  }

  @Test
  fun `Recall with FIRST_MIDDLE_LAST licenceNameCategory but no middle name returns correctly formatted name`() {
    assertThat(recall.copy(middleNames = null, licenceNameCategory = FIRST_MIDDLE_LAST).prisonerNameOnLicense(), equalTo(FullName("Barrie Badger")))
  }

  @Test
  fun `Recall with OTHER licenceNameCategory throws exception`() {
    assertThrows<IllegalStateException> {
      recall.copy(licenceNameCategory = OTHER).prisonerNameOnLicense()
    }
  }
}
