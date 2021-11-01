package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import java.time.OffsetDateTime

class RecallResponseTest {

  private val recallResponse = RecallResponse(
    ::RecallId.random(), NomsNumber("A12345AA"), ::UserId.random(), OffsetDateTime.now(), OffsetDateTime.now()
  )

  @Test
  fun `RecallResponse without recallNotificationEmailSentDateTime or bookedByUserId set returns null status`() {
    assertThat(recallResponse.status, equalTo(null))
  }

  @Test
  fun `RecallResponse with bookedByUserId set but without recallNotificationEmailSentDateTime set returns status BOOKED_ON`() {
    val recall = recallResponse.copy(
      bookedByUserId = ::UserId.random()
    )
    assertThat(recall.status, equalTo(Status.BOOKED_ON))
  }

  @Test
  fun `RecallResponse with bookedByUserId and assignee set but without recallNotificationEmailSentDateTime set returns status IN_ASSESSMENT`() {
    val recall = recallResponse.copy(
      bookedByUserId = ::UserId.random(),
      assignee = ::UserId.random()
    )
    assertThat(recall.status, equalTo(Status.IN_ASSESSMENT))
  }

  @Test
  fun `RecallResponse with recallNotificationEmailSentDateTime set returns status RECALL_NOTIFICATION_ISSUED`() {
    val recall = recallResponse.copy(
      recallNotificationEmailSentDateTime = OffsetDateTime.now()
    )

    assertThat(recall.status, equalTo(Status.RECALL_NOTIFICATION_ISSUED))
  }

  @Test
  fun `RecallResponse with recallNotificationEmailSentDateTime set and bookedByUserId set returns status RECALL_NOTIFICATION_ISSUED`() {
    val recall = recallResponse.copy(
      recallNotificationEmailSentDateTime = OffsetDateTime.now(),
      bookedByUserId = ::UserId.random()
    )

    assertThat(recall.status, equalTo(Status.RECALL_NOTIFICATION_ISSUED))
  }

  @Test
  fun `RecallResponse with recallNotificationEmailSentDateTime set and bookedByUserId set and assignee populated returns status DOSSIER_IN_PROGRESS`() {
    val recall = recallResponse.copy(
      recallNotificationEmailSentDateTime = OffsetDateTime.now(),
      bookedByUserId = ::UserId.random(),
      assignee = ::UserId.random()
    )

    assertThat(recall.status, equalTo(Status.DOSSIER_IN_PROGRESS))
  }

  @Test
  fun `RecallResponse with dossierCreatedByUserId set returns status DOSSIER_ISSUED`() {
    val recall = recallResponse.copy(
      dossierCreatedByUserId = ::UserId.random()
    )

    assertThat(recall.status, equalTo(Status.DOSSIER_ISSUED))
  }

  @Test
  fun `RecallResponse with recallNotificationEmailSentDateTime set and bookedByUserId set and assignee and dossierCreatedByUserId set returns status DOSSIER_ISSUED`() {
    val recall = recallResponse.copy(
      recallNotificationEmailSentDateTime = OffsetDateTime.now(),
      bookedByUserId = ::UserId.random(),
      assignee = ::UserId.random(),
      dossierCreatedByUserId = ::UserId.random()
    )

    assertThat(recall.status, equalTo(Status.DOSSIER_ISSUED))
  }

  @Test
  fun `RecallResponse with dossierCreatedByUserId set and recallNotificationEmailSentDateTime set and bookedByUserId set returns status DOSSIER_ISSUED`() {
    val recall = recallResponse.copy(
      recallNotificationEmailSentDateTime = OffsetDateTime.now(),
      bookedByUserId = ::UserId.random(),
      dossierCreatedByUserId = ::UserId.random(),
    )

    assertThat(recall.status, equalTo(Status.DOSSIER_ISSUED))
  }

  @Test
  fun `RecallResponse with recallEmailReceivedDateTime set returns recallAssessmentDueDateTime as 24 hours later`() {
    val dateTime = OffsetDateTime.now()
    val recall = recallResponse.copy(
      recallEmailReceivedDateTime = dateTime
    )

    assertThat(recall.recallAssessmentDueDateTime, equalTo(dateTime.plusHours(24)))
  }

  @Test
  fun `RecallResponse without recallEmailReceivedDateTime set returns recallAssessmentDueDateTime as null`() {
    assertThat(recallResponse.recallAssessmentDueDateTime, equalTo(null))
  }
}
