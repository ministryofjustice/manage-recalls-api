package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.onFailure
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.extractor.TokenExtractor
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RescindRecord
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RescindRecordRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.toBase64DecodedByteArray
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RescindRecordId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.service.DocumentService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RescindRecordNotFoundException
import uk.gov.justice.digital.hmpps.managerecallsapi.service.VirusFoundException
import java.time.LocalDate
import java.time.OffsetDateTime

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_MANAGE_RECALLS')")
class RescindRecordController(
  @Autowired private val recallRepository: RecallRepository,
  @Autowired private val documentService: DocumentService,
  @Autowired private val rescindRecordRepository: RescindRecordRepository,
  @Autowired private val tokenExtractor: TokenExtractor
) {

  // FIXME: Check any existing rescind record is completed before allowing a second.

  @PostMapping("/recalls/{recallId}/rescind-records")
  fun createRescindRecord(
    @PathVariable("recallId") recallId: RecallId,
    @RequestBody request: RescindRequestRequest,
    @RequestHeader("Authorization") bearerToken: String
  ): ResponseEntity<RescindRecordId> =
    recallRepository.getByRecallId(recallId).let { recall ->
      if (recall.rescindRecords.maxByOrNull { it.version }?.hasBeenDecided() == false) {
        return ResponseEntity(HttpStatus.FORBIDDEN)
      }
      val currentUserId = tokenExtractor.getTokenFromHeader(bearerToken).userUuid()
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
        ResponseEntity(record.id(), HttpStatus.CREATED)
      }.onFailure {
        throw VirusFoundException()
      }
    }

  @PutMapping("/recalls/{recallId}/rescind-records/{rescindRecordId}")
  fun decideRescindRecord(
    @PathVariable("recallId") recallId: RecallId,
    @PathVariable("rescindRecordId") rescindRecordId: RescindRecordId,
    @RequestBody request: RescindDecisionRequest,
    @RequestHeader("Authorization") bearerToken: String
  ): ResponseEntity<RescindRecordId> =
    recallRepository.getByRecallId(recallId).let { recall ->
      val currentRecord = recall.rescindRecords.firstOrNull { it.id() == rescindRecordId } ?: throw RescindRecordNotFoundException(recallId, rescindRecordId)

      if (currentRecord.hasBeenDecided()) {
        return ResponseEntity(HttpStatus.FORBIDDEN)
      }

      val currentUserId = tokenExtractor.getTokenFromHeader(bearerToken).userUuid()
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
        ResponseEntity(record.id(), HttpStatus.OK)
      }.onFailure {
        throw VirusFoundException()
      }
    }

  data class RescindRequestRequest(
    val details: String,
    val emailReceivedDate: LocalDate,
    val emailFileContent: String,
    val emailFileName: String,
  )

  data class RescindDecisionRequest(
    val approved: Boolean,
    val details: String,
    val emailSentDate: LocalDate,
    val emailFileContent: String,
    val emailFileName: String,
  )
}
