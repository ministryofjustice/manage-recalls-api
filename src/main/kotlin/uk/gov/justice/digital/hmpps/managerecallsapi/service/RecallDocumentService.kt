package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocument
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.storage.S3Service
import java.util.UUID

@Service
class RecallDocumentService(
  @Autowired private val s3Service: S3Service,
  @Autowired private val recallRepository: RecallRepository,
  @Autowired private val recallDocumentRepository: RecallDocumentRepository
) {

  fun uploadAndAddDocumentForRecall(
    recallId: RecallId,
    documentBytes: ByteArray,
    documentCategory: RecallDocumentCategory,
    fileName: String? = null
  ): UUID {
    recallRepository.findByRecallId(recallId) ?: throw RecallNotFoundException(recallId)
    val documentId = recallDocumentRepository.findByRecallIdAndCategory(recallId.value, documentCategory)?.id ?: UUID.randomUUID()
    s3Service.uploadFile(documentId, documentBytes)
    // TODO: [KF] delete the document from S3 if saving fails?
    recallDocumentRepository.save(RecallDocument(documentId, recallId.value, documentCategory, fileName))
    return documentId
  }

  fun getDocument(recallId: RecallId, documentId: UUID): Pair<RecallDocument, ByteArray> {
    val document = recallRepository.getByRecallId(recallId).documents.firstOrNull { it.id == documentId }
      ?: throw RecallDocumentNotFoundException(recallId, documentId)
    val bytes = s3Service.downloadFile(documentId)
    return Pair(document, bytes)
  }

  fun getDocumentContentWithCategory(recallId: RecallId, documentCategory: RecallDocumentCategory): ByteArray {
    return getDocumentContentWithCategoryIfExists(recallId, documentCategory)
      ?: throw RecallDocumentWithCategoryNotFoundException(recallId, documentCategory)
  }

  fun getDocumentContentWithCategoryIfExists(recallId: RecallId, documentCategory: RecallDocumentCategory): ByteArray? {
    // DB constraint ("UNIQUE (recall_id, category)") disallows > 1 doc matching recallId and category
    return recallRepository.getByRecallId(recallId).documents
      .firstOrNull { it.category == documentCategory }?.let { document ->
        s3Service.downloadFile(document.id)
      }
  }
}

data class RecallNotFoundException(val recallId: RecallId) : NotFoundException()
data class RecallDocumentNotFoundException(val recallId: RecallId, val documentId: UUID) : NotFoundException()
data class RecallDocumentWithCategoryNotFoundException(
  val recallId: RecallId,
  val documentCategory: RecallDocumentCategory
) : NotFoundException()

open class NotFoundException : Exception()
