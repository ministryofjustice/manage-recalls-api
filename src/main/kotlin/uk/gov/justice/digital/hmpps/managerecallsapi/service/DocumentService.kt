package uk.gov.justice.digital.hmpps.managerecallsapi.service

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocument
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UnversionedDocument
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UnversionedDocumentRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.VersionedDocument
import uk.gov.justice.digital.hmpps.managerecallsapi.db.VersionedDocumentRepository
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
  @Autowired private val versionedDocumentRepository: VersionedDocumentRepository,
  @Autowired private val unversionedDocumentRepository: UnversionedDocumentRepository,
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
    documentCategory: RecallDocumentCategory,
    documentBytes: ByteArray,
    fileName: String
  ): DocumentId {
    val documentId = if (documentCategory.versioned) {
      versionedDocumentRepository.findByRecallIdAndCategory(recallId.value, documentCategory)?.id() ?: ::DocumentId.random()
    } else {
      ::DocumentId.random()
    }

    s3Service.uploadFile(documentId, documentBytes)
    // TODO: [KF] delete the document from S3 if saving fails?
    saveDocument(documentId, recallId, documentCategory, fileName)
    return documentId
  }

  private fun saveDocument(recallDocument: RecallDocument): RecallDocument {
    return saveDocument(recallDocument.documentId, recallDocument.recallId, recallDocument.category, recallDocument.fileName)
  }

  private fun saveDocument(documentId: DocumentId, recallId: RecallId, category: RecallDocumentCategory, fileName: String): RecallDocument {
    return if (category.versioned) {
      versionedDocumentRepository.save(
        VersionedDocument(documentId, recallId, category, fileName, OffsetDateTime.now(clock))
      ).toRecallDocument()
    } else {
      unversionedDocumentRepository.save(
        UnversionedDocument(documentId, recallId, category, fileName, OffsetDateTime.now(clock))
      ).toRecallDocument()
    }
  }

  fun getDocument(recallId: RecallId, documentId: DocumentId): Pair<RecallDocument, ByteArray> =
    forExistingRecall(recallId) {
      Pair(
        getRecallDocumentById(recallId, documentId),
        s3Service.downloadFile(documentId)
      )
    }

  private fun getRecallDocumentById(
    recallId: RecallId,
    documentId: DocumentId
  ): RecallDocument = (
    versionedDocumentRepository.findByRecallIdAndDocumentId(recallId, documentId)?.toRecallDocument()
      ?: unversionedDocumentRepository.getByRecallIdAndDocumentId(recallId, documentId).toRecallDocument()
    )

  fun getVersionedDocumentContentWithCategory(recallId: RecallId, documentCategory: RecallDocumentCategory): ByteArray =
    getVersionedDocumentContentWithCategoryIfExists(recallId, documentCategory)
      ?: throw RecallDocumentWithCategoryNotFoundException(recallId, documentCategory)

  fun getVersionedDocumentContentWithCategoryIfExists(recallId: RecallId, documentCategory: RecallDocumentCategory): ByteArray? =
    forExistingRecall(recallId) {
      versionedDocumentRepository.findByRecallIdAndCategory(recallId.value, documentCategory)?.let {
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
  ): RecallDocument {
    return forExistingRecall(recallId) {
      getRecallDocumentById(recallId, documentId).let { existing ->
        return when (Pair(existing.category.versioned, newCategory.versioned)) {
          Pair(true, true), Pair(false, false) -> {
            existing.copy(category = newCategory).let { copy ->
              saveDocument(copy)
            }
          }
          Pair(true, false) ->
            {
              copyAndDelete(existing, newCategory) {
                versionedDocumentRepository.delete(existing.toVersionedDocument())
                existing
              }
            }
          Pair(false, true) ->
            {
              copyAndDelete(existing, newCategory) {
                unversionedDocumentRepository.delete(existing.toUnversionedDocument())
                existing
              }
            }
          else -> throw IllegalStateException("Existing category (${existing.category}) and NewCategory(${newCategory.versioned}) should both have a versioned property")
        }
      }
    }
  }

  private fun <T> copyAndDelete(
    existing: RecallDocument,
    newCategory: RecallDocumentCategory,
    fn: () -> T
  ): RecallDocument =
    existing.copy(category = newCategory).let { copy ->
      fn()
      saveDocument(copy)
    }
}

data class RecallNotFoundException(val recallId: RecallId) : NotFoundException()
data class RecallDocumentNotFoundException(val recallId: RecallId, val documentId: DocumentId) : NotFoundException()
data class RecallDocumentWithCategoryNotFoundException(
  val recallId: RecallId,
  val documentCategory: RecallDocumentCategory
) : NotFoundException()

open class NotFoundException : Exception()

data class VirusFoundEvent(
  val recallId: RecallId,
  val documentCategory: RecallDocumentCategory,
  val foundViruses: Map<String, Collection<String>>
)
