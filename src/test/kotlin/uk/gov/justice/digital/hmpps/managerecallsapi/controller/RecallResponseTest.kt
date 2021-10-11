package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomNoms
import java.time.OffsetDateTime
import java.util.UUID

class RecallResponseTest {

  @Test
  fun `RecallResponse without recallNotificationEmailSentDateTime or bookedByUserId set returns null status`() {
    val recall = RecallResponse(RecallId(UUID.randomUUID()), NomsNumber("A12345AA"))

    assertThat(recall.status, equalTo(null))
  }

  @Test
  fun `RecallResponse with bookedByUserId set but without recallNotificationEmailSentDateTime set returns status BOOKED_ON`() {
    val recall = RecallResponse(
      RecallId(UUID.randomUUID()), NomsNumber("A12345AA"), bookedByUserId = UserId(UUID.randomUUID())
    )
    assertThat(recall.status, equalTo(Status.BOOKED_ON))
  }

  @Test
  fun `RecallResponse with recallNotificationEmailSentDateTime set returns status RECALL_NOTIFICATION_ISSUED`() {
    val recall = RecallResponse(
      RecallId(UUID.randomUUID()),
      NomsNumber("A12345AA"),
      recallNotificationEmailSentDateTime = OffsetDateTime.now()
    )

    assertThat(recall.status, equalTo(Status.RECALL_NOTIFICATION_ISSUED))
  }

  @Test
  fun `RecallResponse with recallNotificationEmailSentDateTime set and bookedByUserId set returns status RECALL_NOTIFICATION_ISSUED`() {
    val recall = RecallResponse(
      RecallId(UUID.randomUUID()), NomsNumber("A12345AA"), bookedByUserId = UserId(UUID.randomUUID()),
      recallNotificationEmailSentDateTime = OffsetDateTime.now()
    )

    assertThat(recall.status, equalTo(Status.RECALL_NOTIFICATION_ISSUED))
  }

  @Test
  fun `RecallResponse with dossierCreatedByUserId set returns status DOSSIER_ISSUED`() {
    val recall = RecallResponse(
      RecallId(UUID.randomUUID()), randomNoms(),
      dossierCreatedByUserId = UserId(UUID.randomUUID())
    )

    assertThat(recall.status, equalTo(Status.DOSSIER_ISSUED))
  }

  @Test
  fun `RecallResponse with dossierCreatedByUserId set and recallNotificationEmailSentDateTime set and bookedByUserId set returns status DOSSIER_ISSUED`() {
    val recall = RecallResponse(
      RecallId(UUID.randomUUID()), randomNoms(),
      dossierCreatedByUserId = UserId(UUID.randomUUID()),
      recallNotificationEmailSentDateTime = OffsetDateTime.now(),
      bookedByUserId = UserId(UUID.randomUUID()),
    )

    assertThat(recall.status, equalTo(Status.DOSSIER_ISSUED))
  }

  @Test
  fun `RecallResponse with recallEmailReceivedDateTime set returns recallAssessmentDueDateTime as 24 hours later`() {
    val dateTime = OffsetDateTime.now()
    val recall = RecallResponse(
      RecallId(UUID.randomUUID()), randomNoms(),
      recallEmailReceivedDateTime = dateTime
    )

    assertThat(recall.recallAssessmentDueDateTime, equalTo(dateTime.plusHours(24)))
  }

  @Test
  fun `RecallResponse without recallEmailReceivedDateTime set returns recallAssessmentDueDateTime as null`() {
    val recall = RecallResponse(
      RecallId(UUID.randomUUID()), randomNoms()
    )

    assertThat(recall.recallAssessmentDueDateTime, equalTo(null))
  }
}
