package uk.gov.justice.digital.hmpps.managerecallsapi.db

import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RescindRecordId
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
@Table(name = "rescind_record")
data class RescindRecord(
  @Id
  val id: UUID,

  @Column(name = "recall_id", nullable = false)
  val recallId: UUID,

  @Column(nullable = false)
  val version: Int,

  @Column(nullable = false)
  val createdByUserId: UUID,

  @Column(nullable = false)
  val createdDateTime: OffsetDateTime,

  @Column(nullable = false)
  val lastUpdatedDateTime: OffsetDateTime,

  @Column(nullable = false)
  @JoinColumn(name = "document_id", table = "document")
  val requestEmailId: UUID,

  @Column(nullable = false)
  val requestEmailReceivedDate: LocalDate,

  @Column(nullable = false)
  val requestDetails: String,

  @Column
  val approved: Boolean? = null,

  @Column
  @JoinColumn(name = "document_id", table = "document")
  val decisionEmailId: UUID?,

  @Column
  val decisionEmailSentDate: LocalDate?,

  @Column
  val decisionDetails: String?,
) {
  constructor(
    id: RescindRecordId,
    recallId: RecallId,
    version: Int,
    createdByUserId: UserId,
    createdDateTime: OffsetDateTime,
    requestEmailId: DocumentId,
    requestDetails: String,
    requestEmailReceivedDate: LocalDate,
    approved: Boolean? = null,
    decisionEmailId: DocumentId? = null,
    decisionDetails: String? = null,
    decisionEmailSentDate: LocalDate? = null,
    lastUpdatedDateTime: OffsetDateTime = createdDateTime,
  ) :
    this(
      id.value,
      recallId.value,
      version,
      createdByUserId.value,
      createdDateTime,
      lastUpdatedDateTime,
      requestEmailId.value,
      requestEmailReceivedDate,
      requestDetails,
      approved,
      decisionEmailId?.value,
      decisionEmailSentDate,
      decisionDetails
    )

  fun id() = RescindRecordId(id)
  fun recallId() = RecallId(recallId)
  fun createdByUserId() = UserId(createdByUserId)
  fun requestEmailId() = DocumentId(requestEmailId)
  fun decisionEmailId() = decisionEmailId?.let(::DocumentId)
  fun hasBeenDecided(): Boolean = decisionEmailId != null
}
