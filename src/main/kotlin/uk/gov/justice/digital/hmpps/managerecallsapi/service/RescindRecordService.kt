package uk.gov.justice.digital.hmpps.managerecallsapi.service

import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.map
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RescindRecordController.RescindDecisionRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RescindRecordController.RescindRequestRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.StoppedReason
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RescindRecord
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RescindRecordRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.toBase64DecodedByteArray
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RescindRecordId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import java.time.OffsetDateTime
import javax.transaction.Transactional

@Service
class RescindRecordService(
  @Autowired private val rescindRecordRepository: RescindRecordRepository,
  @Autowired private val recallRepository: RecallRepository,
  @Autowired private val documentService: DocumentService,
  @Autowired private val statusService: StatusService,
) {

  @Transactional
  fun makeDecision(
    recallId: RecallId,
    currentUserId: UserId,
    rescindRecordId: RescindRecordId,
    request: RescindDecisionRequest
  ): Result<RescindRecordId, VirusScanResult> {

    return recallRepository.getByRecallId(recallId).let { recall ->

      val currentRecord = recall.rescindRecords.firstOrNull { it.id() == rescindRecordId } ?: throw RescindRecordNotFoundException(recallId, rescindRecordId)

      if (currentRecord.hasBeenDecided()) {
        throw RescindRecordAlreadyDecidedException(recallId, rescindRecordId)
      }

      documentService.scanAndStoreDocument(
        recallId,
        currentUserId,
        request.emailFileContent.toBase64DecodedByteArray(),
        DocumentCategory.RESCIND_DECISION_EMAIL,
        request.emailFileName
      ).map { documentId ->
        val record = rescindRecordRepository.save(
          currentRecord.copy(
            approved = request.approved,
            decisionEmailId = documentId.value,
            decisionDetails = request.details,
            decisionEmailSentDate = request.emailSentDate,
            lastUpdatedDateTime = OffsetDateTime.now()
          )
        )
        if (request.approved) {
          statusService.stopRecall(recallId, StoppedReason.RESCINDED, currentUserId)
        }
        record.id()
      }
    }
  }

  fun createRequest(
    recallId: RecallId,
    currentUserId: UserId,
    request: RescindRequestRequest
  ): Result<RescindRecordId, VirusScanResult> =
    recallRepository.getByRecallId(recallId).let { recall ->
      if (recall.rescindRecords.maxByOrNull { it.version }?.hasBeenDecided() == false) {
        throw UndecidedRescindRecordAlreadyExistsException(recallId)
      }
      documentService.scanAndStoreDocument(
        recallId,
        currentUserId,
        request.emailFileContent.toBase64DecodedByteArray(),
        DocumentCategory.RESCIND_REQUEST_EMAIL,
        request.emailFileName
      ).map { documentId ->
        val currentVersion = recall.rescindRecords.maxByOrNull { it.version }?.version ?: 0
        val record = rescindRecordRepository.save(
          RescindRecord(
            ::RescindRecordId.random(),
            recallId,
            currentVersion + 1,
            currentUserId,
            OffsetDateTime.now(),
            documentId,
            request.details,
            request.emailReceivedDate
          )
        )
        record.id()
      }
    }
}
