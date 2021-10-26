package uk.gov.justice.digital.hmpps.managerecallsapi.db

import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "versioned_document")
data class VersionedDocument(
  @Id
  val id: UUID,

  @Column(name = "recall_id", nullable = false)
  val recallId: UUID,

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  val category: RecallDocumentCategory,

  @Column(nullable = false)
  val fileName: String,

  @Column(nullable = false)
  val createdDateTime: OffsetDateTime
) {
  constructor(
    id: DocumentId,
    recallId: RecallId,
    category: RecallDocumentCategory,
    fileName: String,
    createdDateTime: OffsetDateTime
  ) : this(
    id.value, recallId.value, category, fileName, createdDateTime
  )

  fun toRecallDocument() = RecallDocument(id(), recallId(), category, fileName, createdDateTime)
  fun id() = DocumentId(id)
  fun recallId() = RecallId(recallId)
}
