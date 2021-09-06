package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import java.time.OffsetDateTime
import java.util.UUID

class RecallResponseTest {

  @Test
  fun `RecallResponse without recallNotificationEmailSentDateTime or documents set returns null status`() {
    val recall = RecallResponse(RecallId(UUID.randomUUID()), NomsNumber("A12345AA"))

    assertThat(recall.status, equalTo(null))
  }

  @Test
  fun `RecallResponse with at least 1 document and without recallNotificationEmailSentDateTime set returns status BOOKED_ON`() {
    val recall = RecallResponse(
      RecallId(UUID.randomUUID()), NomsNumber("A12345AA"),
      documents = listOf(
        ApiRecallDocument(UUID.randomUUID(), RecallDocumentCategory.LICENCE, "blah")
      )
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
  fun `RecallResponse with recallNotificationEmailSentDateTime set and at least 1 document returns status RECALL_NOTIFICATION_ISSUED`() {
    val recall = RecallResponse(
      RecallId(UUID.randomUUID()), NomsNumber("A12345AA"),
      documents = listOf(
        ApiRecallDocument(UUID.randomUUID(), RecallDocumentCategory.LICENCE, "blah")
      ),
      recallNotificationEmailSentDateTime = OffsetDateTime.now()
    )

    assertThat(recall.status, equalTo(Status.RECALL_NOTIFICATION_ISSUED))
  }
}
