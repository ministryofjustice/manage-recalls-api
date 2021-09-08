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
    documentCategory: RecallDocumentCategory,
    fileName: String?
  ): UUID {
    if (recallRepository.existsById(recallId.value)) {
      return s3Service.uploadFile(documentBytes).also { documentId ->
        addDocumentToRecall(recallId, documentId, documentCategory, fileName)
      }
    } else {
      throw RecallNotFoundException(recallId)
    }
  }

  private fun addDocumentToRecall(
    recallId: RecallId,
    documentId: UUID,
    documentCategory: RecallDocumentCategory,
    fileName: String?
  ) {
    with(recallRepository.getByRecallId(recallId)) {
      val document = RecallDocument(documentId, recallId.value, documentCategory, fileName)
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

  fun getDocumentWithCategory(recallId: RecallId, documentCategory: RecallDocumentCategory): Pair<RecallDocument, ByteArray> {
    // For any occurrence of > 1 doc matching recallId and category the actual returned doc here is undefined
    val document = recallRepository.getByRecallId(recallId).documents.firstOrNull { it.category == documentCategory }
      ?: throw RecallDocumentWithCategoryNotFoundException(recallId, documentCategory)
    val bytes = s3Service.downloadFile(document.id)
    return Pair(document, bytes)
  }
}

data class RecallNotFoundException(val recallId: RecallId) : NotFoundException()
data class RecallDocumentNotFoundException(val recallId: RecallId, val documentId: UUID) : NotFoundException()
data class RecallDocumentWithCategoryNotFoundException(val recallId: RecallId, val documentCategory: RecallDocumentCategory) : NotFoundException()
open class NotFoundException : Exception()
