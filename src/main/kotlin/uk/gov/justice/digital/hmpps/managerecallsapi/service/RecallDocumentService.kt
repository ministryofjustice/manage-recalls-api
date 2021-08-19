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
    val documentId = s3Service.uploadFile(documentBytes)
    val document = RecallDocument(
      id = documentId,
      recallId = recallId.value,
      category = documentCategory
    )
    with(recallRepository.getByRecallId(recallId)) {
      recallRepository.save(this.copy(documents = this.documents + document))
    }
    // TODO: [KF] delete the document from S3 if saving fails?
    return documentId
  }

  fun getDocument(recallId: RecallId, documentId: UUID): Pair<RecallDocument, ByteArray> {
    val document = recallRepository.getByRecallId(recallId).documents.firstOrNull { it.id == documentId }
      ?: throw RecallDocumentNotFoundException("Document not found: '$documentId' (for recall '$recallId')")
    val bytes = s3Service.downloadFile(documentId)
    return Pair(document, bytes)
  }
}

class RecallNotFoundException(message: String, e: Throwable) : NotFoundException(message, e)

class RecallDocumentNotFoundException(message: String, e: Throwable? = null) : NotFoundException(message, e)

open class NotFoundException(message: String, e: Throwable? = null) : Exception(message, e)
