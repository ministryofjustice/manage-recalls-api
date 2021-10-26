package uk.gov.justice.digital.hmpps.managerecallsapi.db

import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import java.time.OffsetDateTime

data class RecallDocument(
  val id: DocumentId,
  val recallId: RecallId,
  val category: RecallDocumentCategory,
  val fileName: String,
  val createdDateTime: OffsetDateTime
) {
  fun toVersionedDocument() =
    if (category.versioned) {
      VersionedDocument(id, recallId, category, fileName, createdDateTime)
    } else {
      throw WrongDocumentTypeException(category)
    }

  fun toUnversionedDocument() =
    if (!category.versioned) {
      UnversionedDocument(id, recallId, category, fileName, createdDateTime)
    } else {
      throw WrongDocumentTypeException(category)
    }
}

class WrongDocumentTypeException(val category: RecallDocumentCategory) : Exception()
