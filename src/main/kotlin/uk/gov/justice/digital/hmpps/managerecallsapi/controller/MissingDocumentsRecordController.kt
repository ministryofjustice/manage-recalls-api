package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.onFailure
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.managerecallsapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.config.VirusFoundException
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.extractor.TokenExtractor
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.db.MissingDocumentsRecord
import uk.gov.justice.digital.hmpps.managerecallsapi.db.MissingDocumentsRecordRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.toBase64DecodedByteArray
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FileName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.MissingDocumentsRecordId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.service.DocumentService
import java.time.OffsetDateTime

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_MANAGE_RECALLS')")
class MissingDocumentsRecordController(
  @Autowired private val recallRepository: RecallRepository,
  @Autowired private val missingDocumentsRecordRepository: MissingDocumentsRecordRepository,
  @Autowired private val documentService: DocumentService,
  @Autowired private val tokenExtractor: TokenExtractor
) {

  @ApiResponses(
    ApiResponse(
      responseCode = "400",
      description = "Bad request, e.g. virus found exception",
      content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
    )
  )
  @PostMapping("/recalls/{recallId}/missing-documents-records")
  @Operation(summary = "Creates a missing document record for the recall identified by the recallId")
  @ResponseStatus(HttpStatus.CREATED)
  fun createMissingDocumentsRecord(
    @PathVariable("recallId") recallId: RecallId,
    @RequestBody missingDocumentsRecordRequest: MissingDocumentsRecordRequest,
    @RequestHeader("Authorization") bearerToken: String
  ): MissingDocumentsRecordId {
    return recallRepository.getByRecallId(recallId).let { // TODO: PUD-1500: should be moved into a service class and annotated @Transactional
      val currentUserId = tokenExtractor.getTokenFromHeader(bearerToken).userUuid()
      documentService.scanAndStoreDocument(
        recallId,
        currentUserId,
        missingDocumentsRecordRequest.emailFileContent.toBase64DecodedByteArray(),
        DocumentCategory.MISSING_DOCUMENTS_EMAIL,
        missingDocumentsRecordRequest.emailFileName,
        missingDocumentsRecordRequest.details // The MDR "details" are stored twice - see below - for ease of use: its needed here to avoid PUD-1251
      ).map { documentId ->
        val currentVersion = it.missingDocumentsRecords.maxByOrNull { it.version }?.version ?: 0
        val mdr = missingDocumentsRecordRepository.save(
          MissingDocumentsRecord(
            ::MissingDocumentsRecordId.random(),
            recallId,
            missingDocumentsRecordRequest.categories.toSet(),
            documentId,
            missingDocumentsRecordRequest.details,
            currentVersion + 1,
            currentUserId,
            OffsetDateTime.now()
          )
        )
        mdr.id()
      }.onFailure {
        throw VirusFoundException()
      }
    }
  }
}

data class MissingDocumentsRecordRequest(
  val categories: List<DocumentCategory>,
  val details: String,
  val emailFileContent: String,
  val emailFileName: FileName
)
