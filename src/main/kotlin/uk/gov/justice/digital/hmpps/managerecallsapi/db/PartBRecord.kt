package uk.gov.justice.digital.hmpps.managerecallsapi.db

import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PartBRecordId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.Table

@Entity
@Table(name = "part_b_record")
data class PartBRecord(
  @Id
  val id: UUID,

  @Column(name = "recall_id", nullable = false)
  val recallId: UUID,

  @Column(nullable = false)
  val details: String,

  @Column(name = "part_b_received_date", nullable = false)
  val partBReceivedDate: LocalDate,

  @Column(name = "part_b_document_id", nullable = false)
  @JoinColumn(name = "document_id", table = "document")
  val partBDocumentId: UUID,

  @Column(nullable = false)
  @JoinColumn(name = "document_id", table = "document")
  val emailId: UUID,

  @Column(nullable = true)
  @JoinColumn(name = "document_id", table = "document")
  val oasysDocumentId: UUID?,

  @Column(nullable = false)
  val version: Int,

  @Column(nullable = false)
  val createdByUserId: UUID,

  @Column(nullable = false)
  val createdDateTime: OffsetDateTime,
) {
  constructor(
    id: PartBRecordId,
    recallId: RecallId,
    details: String,
    partBReceivedDate: LocalDate,
    partBDocumentId: DocumentId,
    emailId: DocumentId,
    oasysDocumentId: DocumentId? = null,
    version: Int,
    createdByUserId: UserId,
    createdDateTime: OffsetDateTime,
  ) :
    this(
      id.value,
      recallId.value,
      details,
      partBReceivedDate,
      partBDocumentId.value,
      emailId.value,
      oasysDocumentId?.value,
      version,
      createdByUserId.value,
      createdDateTime
    )

  fun id() = PartBRecordId(id)
  fun recallId() = RecallId(recallId)
  fun createdByUserId() = UserId(createdByUserId)
  fun partBDocumentId() = DocumentId(partBDocumentId)
  fun emailId() = DocumentId(emailId)
  fun oasysDocumentId() = oasysDocumentId?.let(::DocumentId)
}
