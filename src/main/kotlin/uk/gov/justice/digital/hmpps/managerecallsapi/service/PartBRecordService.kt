package uk.gov.justice.digital.hmpps.managerecallsapi.service

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.result4k.valueOrNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.PartBRecordController.PartBRequest
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
    request: PartBRequest
  ): Result<PartBRecordId, List<Pair<DocumentCategory, FileName>>> =
    recallRepository.getByRecallId(recallId).let { recall ->
      // TODO: Move validation to DTO?  Even then, cross-field validation requires custom code.
      //  As these properties are mutually dependent an alternative would be to combine them as a single object in the DTO
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

  private fun scanAndStoreDocuments(request: PartBRequest, recallId: RecallId, currentUserId: UserId):
    Map<DocumentCategory, Pair<FileName, Result<DocumentId, VirusScanResult>>> {
    val res = mutableMapOf(
      scanAndStore(PART_B_RISK_REPORT, request.details, request.partBFileContent, request.partBFileName, recallId, currentUserId),
      scanAndStore(PART_B_EMAIL_FROM_PROBATION, request.details, request.emailFileContent, request.emailFileName, recallId, currentUserId)
    )
    request.oasysFileContent?.let {
      val oasysPair = scanAndStore(OASYS_RISK_ASSESSMENT, "Uploaded alongside Part B", it, request.oasysFileName!!, recallId, currentUserId)
      res.put(oasysPair.first, oasysPair.second)
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
