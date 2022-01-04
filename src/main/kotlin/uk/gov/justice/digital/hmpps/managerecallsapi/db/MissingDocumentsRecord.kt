package uk.gov.justice.digital.hmpps.managerecallsapi.db

import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.MissingDocumentsRecordId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.EnumType.STRING
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.JoinTable
import javax.persistence.Table

@Entity
@Table(name = "missing_documents_record")
data class MissingDocumentsRecord(
  @Id
  val id: UUID,

  @Column(name = "recall_id", nullable = false)
  val recallId: UUID,

  @ElementCollection(targetClass = DocumentCategory::class)
  @JoinTable(name = "missing_document_category", joinColumns = [JoinColumn(name = "missing_document_record_id")])
  @Enumerated(STRING)
  @Column(name = "document_category", nullable = false)
  val categories: Set<DocumentCategory>,

  @Column(nullable = false)
  @JoinColumn(name = "document_id", table = "document")
  val emailId: UUID,

  @Column(nullable = false)
  val details: String,

  @Column(nullable = false)
  val version: Int,

  @Column(nullable = false)
  val createdByUserId: UUID,

  @Column(nullable = false)
  val createdDateTime: OffsetDateTime
) {
  constructor(
    id: MissingDocumentsRecordId,
    recallId: RecallId,
    categories: Set<DocumentCategory>,
    emailId: DocumentId,
    details: String,
    version: Int,
    createdByUserId: UserId,
    createdDateTime: OffsetDateTime
  ) :
    this(
      id.value, recallId.value, categories, emailId.value, details, version, createdByUserId.value, createdDateTime
    )

  fun id() = MissingDocumentsRecordId(id)
  fun recallId() = RecallId(recallId)
  fun emailId() = DocumentId(emailId)
  fun createdByUserId() = UserId(createdByUserId)
}
