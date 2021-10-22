package uk.gov.justice.digital.hmpps.managerecallsapi.db

import java.util.UUID

data class RecallDocument(
  val id: UUID,
  val recallId: UUID,
  val category: RecallDocumentCategory,
  val fileName: String?
)

fun VersionedDocument.toRecallDocument() =
  RecallDocument(this.id, this.recallId, this.category, this.fileName)

fun UnversionedDocument.toRecallDocument() =
  RecallDocument(this.id, this.recallId, this.category, this.fileName)
