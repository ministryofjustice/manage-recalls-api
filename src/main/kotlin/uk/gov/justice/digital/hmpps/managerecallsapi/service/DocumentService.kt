package uk.gov.justice.digital.hmpps.managerecallsapi.service

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managerecallsapi.config.ManageRecallsException
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Status
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Document
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.service.VirusScanResult.NoVirusFound
import uk.gov.justice.digital.hmpps.managerecallsapi.service.VirusScanResult.VirusFound
import uk.gov.justice.digital.hmpps.managerecallsapi.storage.S3Service
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
    documentBytes: ByteArray,
    documentCategory: RecallDocumentCategory,
    fileName: String
  ): Result<DocumentId, VirusScanResult> =
    forExistingRecall(recallId) {
      when (val virusScanResult = virusScanner.scan(documentBytes)) {
        NoVirusFound -> Success(storeDocument(recallId, documentBytes, documentCategory, fileName))
        is VirusFound -> {
          log.info(VirusFoundEvent(recallId, documentCategory, virusScanResult.foundViruses).toString())
          Failure(virusScanResult)
        }
      }
    }

  fun storeDocument(
    recallId: RecallId,
    documentBytes: ByteArray,
    documentCategory: RecallDocumentCategory,
    fileName: String
  ): DocumentId =
    forExistingRecall(recallId) {
      uploadToS3AndSaveDocument(recallId, documentCategory, documentBytes, fileName)
    }

  private fun uploadToS3AndSaveDocument(
    recallId: RecallId,
    category: RecallDocumentCategory,
    documentBytes: ByteArray,
    fileName: String
  ): DocumentId {
    val version = if (category.versioned) {
      (documentRepository.findLatestVersionedDocumentByRecallIdAndCategory(recallId, category)?.version ?: 0) + 1
    } else {
      null
    }
    val documentId = ::DocumentId.random()

    documentRepository.save(Document(documentId, recallId, category, fileName, version, OffsetDateTime.now(clock)))
    // TODO: [KF] delete the document from repository if saving fails?
    s3Service.uploadFile(documentId, documentBytes)
    return documentId
  }

  fun getDocument(recallId: RecallId, documentId: DocumentId): Pair<Document, ByteArray> =
    forExistingRecall(recallId) {
      Pair(
        getRecallDocumentById(recallId, documentId),
        s3Service.downloadFile(documentId)
      )
    }

  private fun getRecallDocumentById(
    recallId: RecallId,
    documentId: DocumentId
  ): Document = (
    documentRepository.getByRecallIdAndDocumentId(recallId, documentId)
    )

  fun getLatestVersionedDocumentContentWithCategory(recallId: RecallId, documentCategory: RecallDocumentCategory): ByteArray =
    getLatestVersionedDocumentContentWithCategoryIfExists(recallId, documentCategory)
      ?: throw RecallDocumentWithCategoryNotFoundException(recallId, documentCategory)

  fun getLatestVersionedDocumentContentWithCategoryIfExists(
    recallId: RecallId,
    documentCategory: RecallDocumentCategory
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
    newCategory: RecallDocumentCategory
  ): Document {
    return forExistingRecall(recallId) {
      documentRepository.save(
        getRecallDocumentById(recallId, documentId)
          .copy(category = newCategory, version = if (newCategory.versioned) 1 else null)
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
      throw DocumentDeleteException("Unable to delete document: Wrong status [${recall.status()}] and/or document category [${document.category}]")
    }
  }
}

data class RecallNotFoundException(val recallId: RecallId) : NotFoundException()
data class DocumentNotFoundException(val recallId: RecallId, val documentId: DocumentId) : NotFoundException()
data class RecallDocumentWithCategoryNotFoundException(
  val recallId: RecallId,
  val documentCategory: RecallDocumentCategory
) : NotFoundException()

open class NotFoundException : ManageRecallsException()
class VirusFoundException : ManageRecallsException()
class DocumentDeleteException(override val message: String?) : ManageRecallsException(message)

data class VirusFoundEvent(
  val recallId: RecallId,
  val documentCategory: RecallDocumentCategory,
  val foundViruses: Map<String, Collection<String>>
)
