package uk.gov.justice.digital.hmpps.managerecallsapi.service

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.result4k.valueOrNull
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.PartBRecordController.PartBRecordRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.OASYS_RISK_ASSESSMENT
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.PART_B_EMAIL_FROM_PROBATION
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.PART_B_RISK_REPORT
import uk.gov.justice.digital.hmpps.managerecallsapi.db.PartBRecord
import uk.gov.justice.digital.hmpps.managerecallsapi.db.PartBRecordRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.toBase64DecodedByteArray
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FileName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PartBRecordId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import java.time.OffsetDateTime
import javax.transaction.Transactional

@Service
class PartBRecordService(
  @Autowired private val partBRecordRepository: PartBRecordRepository,
  @Autowired private val recallRepository: RecallRepository,
  @Autowired private val documentService: DocumentService,
) {

  @Transactional
  fun createRecord(
    recallId: RecallId,
    currentUserId: UserId,
    request: PartBRecordRequest
  ): Result<PartBRecordId, List<Pair<DocumentCategory, FileName>>> =
    recallRepository.getByRecallId(recallId).let { recall ->

      // TODO: improve on this validation with PUD-1626 Cross-property Request validation: move to DTOs or restructure Request classes?
      val oasysName = request.oasysFileName
      val oasysContent = request.oasysFileContent
      if ((oasysName == null && !oasysContent.isNullOrBlank()) || (oasysContent.isNullOrBlank() && oasysName != null)) {
        throw IllegalArgumentException("Both fileName and fileContent of OASYS must be supplied as non-blank or neither")
      }
      val docs = scanAndStoreDocuments(request, recallId, currentUserId)

      val savedFiles = mutableMapOf<DocumentCategory, DocumentId>()
      val failedFiles = mutableListOf<Pair<DocumentCategory, FileName>>()
      docs.forEach { result ->
        result.value.second
          .map { savedFiles.put(result.key, result.value.second.valueOrNull()!!) }
          .mapFailure { failedFiles.add(result.key to result.value.first) }
      }
      if (failedFiles.isNotEmpty()) {
        return Failure(failedFiles)
      }
      val currentVersion = recall.partBRecords.maxByOrNull { it.version }?.version ?: 0
      val record = partBRecordRepository.save(
        PartBRecord(
          ::PartBRecordId.random(),
          recallId,
          request.details,
          request.partBReceivedDate,
          savedFiles[PART_B_RISK_REPORT]!!,
          savedFiles[PART_B_EMAIL_FROM_PROBATION]!!,
          savedFiles[OASYS_RISK_ASSESSMENT],
          currentVersion + 1,
          currentUserId,
          OffsetDateTime.now()
        )
      )
      Success(record.id())
    }

  private fun scanAndStoreDocuments(request: PartBRecordRequest, recallId: RecallId, currentUserId: UserId):
    Map<DocumentCategory, Pair<FileName, Result<DocumentId, VirusScanResult>>> {
    val res = mutableMapOf<DocumentCategory, Pair<FileName, Result<DocumentId, VirusScanResult>>>()
    runBlocking {
      val asyncs = mutableListOf<Deferred<Pair<DocumentCategory, Pair<FileName, Result<DocumentId, VirusScanResult>>>>>()

      asyncs.add(async { scanAndStore(PART_B_RISK_REPORT, request.details, request.partBFileContent, request.partBFileName, recallId, currentUserId) })
      asyncs.add(async { scanAndStore(PART_B_EMAIL_FROM_PROBATION, request.details, request.emailFileContent, request.emailFileName, recallId, currentUserId) })

      request.oasysFileContent?.let {
        asyncs.add(async { scanAndStore(OASYS_RISK_ASSESSMENT, "Uploaded alongside Part B", it, request.oasysFileName!!, recallId, currentUserId) })
      }

      asyncs.forEach {
        val pair = it.await()
        res[pair.first] = pair.second
      }
    }
    return res
  }

  private fun scanAndStore(
    category: DocumentCategory,
    details: String,
    fileContent: String,
    fileName: FileName,
    recallId: RecallId,
    currentUserId: UserId
  ): Pair<DocumentCategory, Pair<FileName, Result<DocumentId, VirusScanResult>>> =
    category to (
      fileName to
        documentService.scanAndStoreDocument(
          recallId,
          currentUserId,
          fileContent.toBase64DecodedByteArray(),
          category,
          fileName,
          details
        )
      )
}
