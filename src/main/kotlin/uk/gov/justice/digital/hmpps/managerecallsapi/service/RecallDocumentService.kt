package uk.gov.justice.digital.hmpps.managerecallsapi.service

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocument
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.service.VirusScanResult.NoVirusFound
import uk.gov.justice.digital.hmpps.managerecallsapi.service.VirusScanResult.VirusFound
import uk.gov.justice.digital.hmpps.managerecallsapi.storage.S3Service
import java.util.UUID

@Service
class RecallDocumentService(
  @Autowired private val s3Service: S3Service,
  @Autowired private val recallRepository: RecallRepository,
  @Autowired private val recallDocumentRepository: RecallDocumentRepository,
  @Autowired private val virusScanner: VirusScanner
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  fun scanAndStoreDocument(
    recallId: RecallId,
    documentBytes: ByteArray,
    documentCategory: RecallDocumentCategory,
    fileName: String? = null
  ): Result<UUID, VirusScanResult> =
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
    fileName: String? = null
  ): UUID =
    forExistingRecall(recallId) {
      uploadToS3AndSaveDocument(recallId, documentCategory, documentBytes, fileName)
    }

  private fun uploadToS3AndSaveDocument(
    recallId: RecallId,
    documentCategory: RecallDocumentCategory,
    documentBytes: ByteArray,
    fileName: String?
  ): UUID {
    val documentId = recallDocumentRepository.findByRecallIdAndCategory(recallId.value, documentCategory)?.id
      ?: UUID.randomUUID()
    s3Service.uploadFile(documentId, documentBytes)
    // TODO: [KF] delete the document from S3 if saving fails?
    recallDocumentRepository.save(RecallDocument(documentId, recallId.value, documentCategory, fileName))
    return documentId
  }

  fun getDocument(recallId: RecallId, documentId: UUID): Pair<RecallDocument, ByteArray> =
    forExistingRecall(recallId) {
      Pair(
        recallDocumentRepository.getByRecallIdAndDocumentId(recallId, documentId),
        s3Service.downloadFile(documentId)
      )
    }

  fun getDocumentContentWithCategory(recallId: RecallId, documentCategory: RecallDocumentCategory): ByteArray =
    getDocumentContentWithCategoryIfExists(recallId, documentCategory)
      ?: throw RecallDocumentWithCategoryNotFoundException(recallId, documentCategory)

  fun getDocumentContentWithCategoryIfExists(recallId: RecallId, documentCategory: RecallDocumentCategory): ByteArray? =
    forExistingRecall(recallId) {
      recallDocumentRepository.findByRecallIdAndCategory(recallId.value, documentCategory)?.let {
        s3Service.downloadFile(it.id)
      }
    }

  private inline fun <reified T : Any?> forExistingRecall(recallId: RecallId, fn: () -> T): T {
    recallRepository.getByRecallId(recallId)
    return fn()
  }
}

data class RecallNotFoundException(val recallId: RecallId) : NotFoundException()
data class RecallDocumentNotFoundException(val recallId: RecallId, val documentId: UUID) : NotFoundException()
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
