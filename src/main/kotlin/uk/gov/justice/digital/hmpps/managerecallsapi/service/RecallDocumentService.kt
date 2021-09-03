package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocument
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.storage.S3Service
import java.util.UUID

@Service
class RecallDocumentService(
  @Autowired private val s3Service: S3Service,
  @Autowired private val recallRepository: RecallRepository
) {

  fun addDocumentToRecall(
    recallId: RecallId,
    documentBytes: ByteArray,
    documentCategory: RecallDocumentCategory
  ): UUID {
    if (recallRepository.existsById(recallId.value)) {
      return s3Service.uploadFile(documentBytes).also { documentId ->
        addDocumentToRecall(recallId, documentId, documentCategory)
      }
    } else {
      throw RecallNotFoundException(recallId)
    }
  }

  private fun addDocumentToRecall(
    recallId: RecallId,
    documentId: UUID,
    documentCategory: RecallDocumentCategory
  ) {
    with(recallRepository.getByRecallId(recallId)) {
      val document = RecallDocument(documentId, recallId.value, documentCategory)
      // TODO: [KF] delete the document from S3 if saving fails?
      recallRepository.save(this.copy(documents = this.documents + document))
    }
  }

  fun getDocument(recallId: RecallId, documentId: UUID): Pair<RecallDocument, ByteArray> {
    val document = recallRepository.getByRecallId(recallId).documents.firstOrNull { it.id == documentId }
      ?: throw RecallDocumentNotFoundException(recallId, documentId)
    val bytes = s3Service.downloadFile(documentId)
    return Pair(document, bytes)
  }
}

data class RecallNotFoundException(val recallId: RecallId) : NotFoundException()
data class RecallDocumentNotFoundException(val recallId: RecallId, val documentId: UUID) : NotFoundException()
open class NotFoundException : Exception()
