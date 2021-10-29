package uk.gov.justice.digital.hmpps.managerecallsapi.db

import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import java.time.OffsetDateTime

data class RecallDocument(
  val documentId: DocumentId,
  val recallId: RecallId,
  val category: RecallDocumentCategory,
  val fileName: String,
  val version: Int?,
  val createdDateTime: OffsetDateTime
)

class WrongDocumentTypeException(val category: RecallDocumentCategory) : Exception()
