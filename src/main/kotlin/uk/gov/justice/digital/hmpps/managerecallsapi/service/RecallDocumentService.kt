package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocument
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.storage.S3Service
import java.util.UUID
import javax.persistence.EntityNotFoundException

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
    val recall = recallById(recallId)
    val fileS3key = s3Service.uploadFile(documentBytes)
    val document = RecallDocument(
      id = fileS3key,
      recallId = recallId.value,
      category = documentCategory
    )
    recallRepository.save(recall.copy(documents = recallById(recallId).documents.plus(document)))
    // TODO: [KF] delete the document from S3 if saving fails?
    return fileS3key
  }

  fun getDocument(recallId: RecallId, documentId: UUID): Pair<RecallDocument, ByteArray> {
    val document = recallById(recallId).documents.firstOrNull { it.id == documentId }
      ?: throw RecallDocumentNotFoundException("Document not found: '$documentId' (for recall '$recallId')")
    val bytes = s3Service.downloadFile(documentId)
    return Pair(document, bytes)
  }

  private fun recallById(recallId: RecallId) = try {
    recallRepository.getByRecallId(recallId)
  } catch (e: EntityNotFoundException) {
    throw RecallNotFoundException("Recall not found: '$recallId'", e)
  }
}

class RecallNotFoundException(message: String, e: Throwable) : NotFoundException(message, e)

class RecallDocumentNotFoundException(message: String, e: Throwable? = null) : NotFoundException(message, e)

open class NotFoundException(message: String, e: Throwable? = null) : Exception(message, e)
