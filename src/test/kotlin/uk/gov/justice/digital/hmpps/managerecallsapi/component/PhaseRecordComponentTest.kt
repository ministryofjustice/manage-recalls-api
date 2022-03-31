package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.BAD_REQUEST
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.BookRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Phase
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CroNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomNoms
import java.time.LocalDate
import java.time.OffsetDateTime

class PhaseRecordComponentTest : ComponentTestBase() {
  private val nomsNumber = randomNoms()
  private val bookRecallRequest = BookRecallRequest(
    nomsNumber,
    FirstName("Barrie"),
    null,
    LastName("Badger"),
    CroNumber("1234/56A"),
    LocalDate.now()
  )

  @Test
  fun `start the BOOK Phase for recall returns BAD_REQUEST`() {
    val recall = authenticatedClient.bookRecall(bookRecallRequest)

    authenticatedClient.startPhase(recall.recallId, Phase.BOOK, BAD_REQUEST)
  }

  @Test
  fun `starting a Phase (other than BOOK) for recall creates new record`() {
    val phase = Phase.ASSESS
    val recall = authenticatedClient.bookRecall(bookRecallRequest)

    val record = authenticatedClient.startPhase(recall.recallId, phase)

    assertThat(record.recallId(), equalTo(recall.recallId))
    assertThat(record.phase, equalTo(phase))
    assertThat(record.startedByUserId(), equalTo(authenticatedClient.userId))
    assertThat(record.startedDateTime, !equalTo<OffsetDateTime>(null))
    assertThat(record.endedByUserId, equalTo(null))
    assertThat(record.endedDateTime, equalTo(null))
  }

  @Test
  fun `starting a phase twice for a recall overwrites first record`() {
    val phase = Phase.DOSSIER
    val recall = authenticatedClient.bookRecall(bookRecallRequest)

    val record1 = authenticatedClient.startPhase(recall.recallId, phase)
    val record2 = authenticatedClient.startPhase(recall.recallId, phase)

    assertThat(record1.id(), equalTo(record2.id()))
    assertThat(record2.recallId(), equalTo(recall.recallId))
    assertThat(record2.phase, equalTo(phase))
    assertThat(record2.startedDateTime.isAfter(record1.startedDateTime), equalTo(true))
    assertThat(record1.startedByUserId(), equalTo(authenticatedClient.userId))
    assertThat(record1.endedByUserId, equalTo(null))
    assertThat(record1.endedDateTime, equalTo(null))
  }

  @Test
  fun `ending a phase updates existing record and unassigns when indicated`() {
    val phase = Phase.ASSESS
    val recall = authenticatedClient.bookRecall(bookRecallRequest)
    val assignedRecall = authenticatedClient.assignRecall(recall.recallId, authenticatedClient.userId)

    assertThat(assignedRecall.assignee, equalTo(authenticatedClient.userId))

    val startRecord = authenticatedClient.startPhase(recall.recallId, phase)
    val endRecord = authenticatedClient.endPhase(recall.recallId, phase, true)

    val unassignedRecall = authenticatedClient.getRecall(recall.recallId)

    assertThat(unassignedRecall.assignee, equalTo(null))

    assertThat(endRecord.id(), equalTo(startRecord.id()))
    assertThat(endRecord.phase, equalTo(startRecord.phase))
    assertThat(endRecord.phase, equalTo(phase))
    assertOffsetDateTimesEqual(endRecord.startedDateTime, startRecord.startedDateTime)
    assertThat(endRecord.startedByUserId(), equalTo(startRecord.startedByUserId()))
    assertThat(endRecord.endedDateTime!!.isAfter(endRecord.startedDateTime), equalTo(true))
    assertThat(endRecord.endedByUserId(), equalTo(authenticatedClient.userId))
  }

  @Test
  fun `ending a phase twice updates existing record`() {
    val phase = Phase.DOSSIER
    val recall = authenticatedClient.bookRecall(bookRecallRequest)
    val assignedRecall = authenticatedClient.assignRecall(recall.recallId, authenticatedClient.userId)

    assertThat(assignedRecall.assignee, equalTo(authenticatedClient.userId))

    val startRecord = authenticatedClient.startPhase(recall.recallId, phase)
    val endRecord1 = authenticatedClient.endPhase(recall.recallId, phase, false)
    val stillAssignedRecall = authenticatedClient.getRecall(recall.recallId)

    val endRecord2 = authenticatedClient.endPhase(recall.recallId, phase, true)
    val unassignedRecall = authenticatedClient.getRecall(recall.recallId)

    assertThat(stillAssignedRecall.assignee, equalTo(authenticatedClient.userId))
    assertThat(unassignedRecall.assignee, equalTo(null))

    assertThat(endRecord2.id(), equalTo(startRecord.id()))
    assertThat(endRecord2.phase, equalTo(startRecord.phase))
    assertThat(endRecord2.phase, equalTo(phase))
    assertOffsetDateTimesEqual(endRecord2.startedDateTime, startRecord.startedDateTime)
    assertThat(endRecord2.startedByUserId(), equalTo(startRecord.startedByUserId()))
    assertThat(endRecord2.endedDateTime!!.isAfter(endRecord1.endedDateTime!!), equalTo(true))
    assertThat(endRecord2.endedByUserId(), equalTo(authenticatedClient.userId))
  }
}
