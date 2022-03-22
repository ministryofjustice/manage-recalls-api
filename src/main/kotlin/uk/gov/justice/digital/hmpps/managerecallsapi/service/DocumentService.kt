package uk.gov.justice.digital.hmpps.managerecallsapi.service

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managerecallsapi.config.ManageRecallsException
import uk.gov.justice.digital.hmpps.managerecallsapi.config.WrongDocumentTypeException
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Status
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.StopReason
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Document
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FileName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastKnownAddressId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RescindRecordId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.service.VirusScanResult.NoVirusFound
import uk.gov.justice.digital.hmpps.managerecallsapi.service.VirusScanResult.VirusFound
import uk.gov.justice.digital.hmpps.managerecallsapi.storage.S3Service
import java.lang.IllegalStateException
import java.time.Clock
import java.time.OffsetDateTime
import javax.transaction.Transactional

@Service
class DocumentService(
  @Autowired private val s3Service: S3Service,
  @Autowired private val recallRepository: RecallRepository,
  @Autowired private val documentRepository: DocumentRepository,
  @Autowired private val virusScanner: VirusScanner,
  @Autowired private val clock: Clock
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  fun scanAndStoreDocument(
    recallId: RecallId,
    currentUserId: UserId,
    documentBytes: ByteArray,
    documentCategory: DocumentCategory,
    fileName: FileName,
    details: String? = null
  ): Result<DocumentId, VirusScanResult> =
    forExistingRecall(recallId) {
      when (val virusScanResult = virusScanner.scan(documentBytes)) {
        NoVirusFound -> Success(storeDocument(recallId, currentUserId, documentBytes, documentCategory, fileName, details))
        is VirusFound -> {
          log.info(VirusFoundEvent(recallId, documentCategory, virusScanResult.foundViruses).toString())
          Failure(virusScanResult)
        }
      }
    }

  @Transactional
  fun storeDocument(
    recallId: RecallId,
    currentUserId: UserId,
    documentBytes: ByteArray,
    documentCategory: DocumentCategory,
    fileName: FileName,
    details: String? = null
  ): DocumentId =
    forExistingRecall(recallId) {
      uploadToS3AndSaveDocument(recallId, currentUserId, documentCategory, documentBytes, fileName, details)
    }

  private fun uploadToS3AndSaveDocument(
    recallId: RecallId,
    currentUserId: UserId,
    category: DocumentCategory,
    documentBytes: ByteArray,
    fileName: FileName,
    details: String? = null
  ): DocumentId {
    val version = if (category.versioned()) {
      (documentRepository.findLatestVersionedDocumentByRecallIdAndCategory(recallId, category)?.version ?: 0) + 1
    } else {
      null
    }
    val documentId = ::DocumentId.random()

    try {
      documentRepository.save(
        Document(
          documentId,
          recallId,
          category,
          fileName,
          version,
          details,
          OffsetDateTime.now(clock),
          currentUserId
        )
      )
    } catch (ex: DataIntegrityViolationException) {
      throw IllegalDocumentStateException(ex.message)
    }

    try {
      s3Service.uploadFile(documentId, documentBytes)
    } catch (ex: Exception) {
      documentRepository.deleteByDocumentId(documentId)
      throw ex
    }

    return documentId
  }

  fun getDocument(recallId: RecallId, documentId: DocumentId): Pair<Document, ByteArray> =
    forExistingRecall(recallId) {
      Pair(
        getRecallDocumentById(recallId, documentId),
        s3Service.downloadFile(documentId)
      )
    }

  fun getRecallDocumentById(
    recallId: RecallId,
    documentId: DocumentId
  ): Document = (
    documentRepository.getByRecallIdAndDocumentId(recallId, documentId)
    )

  fun getLatestVersionedDocumentContentWithCategory(recallId: RecallId, documentCategory: DocumentCategory): ByteArray =
    getLatestVersionedDocumentContentWithCategoryIfExists(recallId, documentCategory)
      ?: throw RecallDocumentWithCategoryNotFoundException(recallId, documentCategory)

  fun getLatestVersionedDocumentContentWithCategoryIfExists(
    recallId: RecallId,
    documentCategory: DocumentCategory
  ): ByteArray? =
    forExistingRecall(recallId) {
      documentRepository.findLatestVersionedDocumentByRecallIdAndCategory(recallId, documentCategory)?.let {
        s3Service.downloadFile(it.id())
      }
    }

  private inline fun <reified T : Any?> forExistingRecall(recallId: RecallId, fn: () -> T): T {
    recallRepository.getByRecallId(recallId)
    return fn()
  }

  @Transactional
  fun updateDocumentCategory(
    recallId: RecallId,
    documentId: DocumentId,
    newCategory: DocumentCategory
  ): Document {
    return forExistingRecall(recallId) {
      val existingDocument = getRecallDocumentById(recallId, documentId)
      val currentCategory = existingDocument.category
      if (currentCategory != DocumentCategory.UNCATEGORISED) {
        throw WrongDocumentTypeException(currentCategory)
      }
      documentRepository.save(
        existingDocument
          .copy(category = newCategory, version = if (newCategory.versioned()) 1 else null)
      )
    }
  }

  @Transactional
  fun deleteDocument(
    recallId: RecallId,
    documentId: DocumentId
  ) {
    val recall = recallRepository.getByRecallId(recallId)
    val document = documentRepository.getByRecallIdAndDocumentId(recallId, documentId)

    if (recall.status() == Status.BEING_BOOKED_ON && document.category.uploaded) {
      documentRepository.deleteByDocumentId(documentId)
    } else {
      throw DocumentDeleteException("Unable to delete document [Recall: $recallId, Document: $documentId]: Wrong status [${recall.status()}] and/or document category [${document.category}]")
    }
  }

  fun getAllDocumentsByCategory(recallId: RecallId, category: DocumentCategory): List<Document> {
    return documentRepository.getAllByRecallIdAndCategory(recallId, category)
  }
}

data class RecallNotFoundException(val recallId: RecallId) : NotFoundException()
data class DocumentNotFoundException(val recallId: RecallId, val documentId: DocumentId) : NotFoundException()
data class LastKnownAddressNotFoundException(val recallId: RecallId, val lastKnownAddressId: LastKnownAddressId) : NotFoundException()
data class RescindRecordNotFoundException(val recallId: RecallId, val rescindRecordId: RescindRecordId) : NotFoundException()
data class RescindRecordAlreadyDecidedException(val recallId: RecallId, val rescindRecordId: RescindRecordId) : IllegalStateException()
data class UndecidedRescindRecordAlreadyExistsException(val recallId: RecallId) : IllegalStateException()
data class InvalidStopReasonException(val recallId: RecallId, val stopReason: StopReason) : IllegalStateException()
data class RecallDocumentWithCategoryNotFoundException(
  val recallId: RecallId,
  val documentCategory: DocumentCategory
) : NotFoundException()

open class NotFoundException : ManageRecallsException()
class VirusFoundException : ManageRecallsException()
class MultiFileException(override val message: String, val failures: List<Pair<DocumentCategory, FileName>>) : Exception()
class DocumentDeleteException(override val message: String?) : ManageRecallsException(message)
class IllegalDocumentStateException(override val message: String?) : ManageRecallsException(message)

data class VirusFoundEvent(
  val recallId: RecallId,
  val documentCategory: DocumentCategory,
  val foundViruses: Map<String, Collection<String>>
)
