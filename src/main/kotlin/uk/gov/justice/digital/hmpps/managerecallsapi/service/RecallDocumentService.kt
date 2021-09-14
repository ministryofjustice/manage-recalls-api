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
    recallRepository.findByRecallId(recallId)?.let { recall ->
      val documentId = recall.documents.firstOrNull { it.category == documentCategory }?.id ?: UUID.randomUUID()
      s3Service.uploadFile(documentId, documentBytes)
      // TODO: [KF] delete the document from S3 if saving fails?
      recallRepository.addDocumentToRecall(recallId, RecallDocument(documentId, recallId.value, documentCategory, fileName))
      return documentId
    } ?: throw RecallNotFoundException(recallId)
  }

  fun getDocument(recallId: RecallId, documentId: UUID): Pair<RecallDocument, ByteArray> {
    val document = recallRepository.getByRecallId(recallId).documents.firstOrNull { it.id == documentId }
      ?: throw RecallDocumentNotFoundException(recallId, documentId)
    val bytes = s3Service.downloadFile(documentId)
    return Pair(document, bytes)
  }

  fun getDocumentContentWithCategory(recallId: RecallId, documentCategory: RecallDocumentCategory): ByteArray {
    // DB constraint ("UNIQUE (recall_id, category)") disallows > 1 doc matching recallId and category
    val document = recallRepository.getByRecallId(recallId).documents.firstOrNull { it.category == documentCategory }
      ?: throw RecallDocumentWithCategoryNotFoundException(recallId, documentCategory)
    return s3Service.downloadFile(document.id)
  }
}

data class RecallNotFoundException(val recallId: RecallId) : NotFoundException()
data class RecallDocumentNotFoundException(val recallId: RecallId, val documentId: UUID) : NotFoundException()
data class RecallDocumentWithCategoryNotFoundException(val recallId: RecallId, val documentCategory: RecallDocumentCategory) : NotFoundException()
open class NotFoundException : Exception()
