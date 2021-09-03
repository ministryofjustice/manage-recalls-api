package uk.gov.justice.digital.hmpps.managerecallsapi.db

import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "recall_document")
data class RecallDocument(
  @Id
  val id: UUID,

  @Column(name = "recall_id", nullable = false)
  val recallId: UUID,

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  val category: RecallDocumentCategory,

  val fileName: String?
)
