package uk.gov.justice.digital.hmpps.managerecallsapi.db

import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NoteId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.Table

@Entity
@Table(name = "note")
data class Note(
  @Id
  val id: UUID,

  @Column(name = "recall_id", nullable = false)
  val recallId: UUID,

  @Column(nullable = false)
  val subject: String,

  @Column(nullable = false)
  val details: String,

  @Column(nullable = false)
  val index: Int,

  @Column
  @JoinColumn(name = "document_id", table = "document")
  val documentId: UUID?,

  @Column(nullable = false)
  val createdByUserId: UUID,

  @Column(nullable = false)
  val createdDateTime: OffsetDateTime,
) {
  constructor(
    id: NoteId,
    recallId: RecallId,
    subject: String,
    details: String,
    index: Int,
    documentId: DocumentId? = null,
    createdByUserId: UserId,
    createdDateTime: OffsetDateTime,
  ) :
    this(
      id.value,
      recallId.value,
      subject,
      details,
      index,
      documentId?.value,
      createdByUserId.value,
      createdDateTime,
    )

  fun id() = NoteId(id)
  fun recallId() = RecallId(recallId)
  fun createdByUserId() = UserId(createdByUserId)
  fun documentId() = documentId?.let(::DocumentId)
}
