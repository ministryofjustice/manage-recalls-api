package uk.gov.justice.digital.hmpps.managerecallsapi.db

import uk.gov.justice.digital.hmpps.managerecallsapi.config.WrongDocumentTypeException
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
@Table(name = "document")
data class Document(
  @Id
  val id: UUID,

  @Column(name = "recall_id", nullable = false)
  val recallId: UUID,

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  val category: RecallDocumentCategory,

  @Column(nullable = false)
  val fileName: String,

  @Column(nullable = true)
  val version: Int?,

  @Column(nullable = false)
  val createdDateTime: OffsetDateTime
) {
  constructor(
    id: DocumentId,
    recallId: RecallId,
    category: RecallDocumentCategory,
    fileName: String,
    version: Int?,
    createdDateTime: OffsetDateTime
  ) :
    this(
      id.value, recallId.value, category, fileName, version, createdDateTime
    ) {
      if ((category.versioned && version == null) || (!category.versioned && version != null)) throw WrongDocumentTypeException(category)
    }

  fun id() = DocumentId(id)
  fun recallId() = RecallId(recallId)
}
