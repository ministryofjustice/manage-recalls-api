package uk.gov.justice.digital.hmpps.managerecallsapi.db

import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Phase
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhaseRecordId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType.STRING
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table
data class PhaseRecord(
  @Id
  val id: UUID,

  @Column(nullable = false)
  val recallId: UUID,

  @Column(nullable = false)
  @Enumerated(STRING)
  val phase: Phase,

  @Column(nullable = false)
  val startedByUserId: UUID,
  @Column(nullable = false)
  val startedDateTime: OffsetDateTime,

  val endedByUserId: UUID?,
  val endedDateTime: OffsetDateTime?,
) {
  constructor(
    id: PhaseRecordId,
    recallId: RecallId,
    phase: Phase,
    startedByUserId: UserId,
    startedDateTime: OffsetDateTime,
    endedByUserId: UserId? = null,
    endedDateTime: OffsetDateTime? = null
  ) :
    this(
      id.value,
      recallId.value,
      phase,
      startedByUserId.value,
      startedDateTime,
      endedByUserId?.value,
      endedDateTime,
    )
  fun id() = PhaseRecordId(id)
  fun recallId() = RecallId(recallId)
  fun startedByUserId() = UserId(startedByUserId)
  fun endedByUserId() = endedByUserId?.let(::UserId)
}
