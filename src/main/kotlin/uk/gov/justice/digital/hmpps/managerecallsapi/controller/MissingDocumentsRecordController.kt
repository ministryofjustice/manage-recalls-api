package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.onFailure
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import io.swagger.annotations.Example
import io.swagger.annotations.ExampleProperty
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.managerecallsapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.extractor.TokenExtractor
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.db.MissingDocumentsRecord
import uk.gov.justice.digital.hmpps.managerecallsapi.db.MissingDocumentsRecordRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.toBase64DecodedByteArray
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.MissingDocumentsRecordId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.service.DocumentService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.VirusFoundException
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
      code = 400, message = "Bad request, e.g. virus found exception", response = ErrorResponse::class,
      examples = Example(ExampleProperty(mediaType = "application/json", value = "{\n\"status\": 400,\n\"message\":\"VirusFoundException\"\n}"))
    )
  )
  @PostMapping("/missing-documents-records")
  fun createMissingDocumentsRecord(
    @RequestBody missingDocumentsRecordRequest: MissingDocumentsRecordRequest,
    @RequestHeader("Authorization") bearerToken: String
  ) =
    recallRepository.getByRecallId(missingDocumentsRecordRequest.recallId).let {
      documentService.scanAndStoreDocument(
        missingDocumentsRecordRequest.recallId,
        missingDocumentsRecordRequest.emailFileContent.toBase64DecodedByteArray(),
        DocumentCategory.MISSING_DOCUMENTS_EMAIL,
        missingDocumentsRecordRequest.emailFileName
      ).map { documentId ->
        val currentVersion = it.missingDocumentsRecords.maxByOrNull { it.version }?.version ?: 0
        val mdr = missingDocumentsRecordRepository.save(
          MissingDocumentsRecord(
            ::MissingDocumentsRecordId.random(),
            missingDocumentsRecordRequest.recallId,
            missingDocumentsRecordRequest.categories.toSet(),
            documentId,
            missingDocumentsRecordRequest.detail,
            currentVersion + 1,
            tokenExtractor.getTokenFromHeader(bearerToken).userUuid(),
            OffsetDateTime.now()
          )
        )

        ResponseEntity(mdr.toResponse(), HttpStatus.CREATED)
      }.onFailure {
        throw VirusFoundException()
      }
    }
}

data class MissingDocumentsRecordRequest(
  val recallId: RecallId,
  val categories: List<DocumentCategory>,
  val detail: String,
  val emailFileContent: String,
  val emailFileName: String
)

data class MissingDocumentResponse(val documentId: DocumentId)
