package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.onFailure
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.config.WrongDocumentTypeException
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.extractor.TokenExtractor
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.DOSSIER
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.LETTER_TO_PRISON
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.REASONS_FOR_RECALL
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.RECALL_NOTIFICATION
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.REVOCATION_ORDER
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier.DossierService
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier.ReasonsForRecallService
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.encodeToBase64String
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.lettertoprison.LetterToPrisonService
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.recallnotification.RecallNotificationService
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.recallnotification.RevocationOrderService
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.toBase64DecodedByteArray
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FullName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.service.DocumentService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.UserDetailsService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.VirusFoundException
import uk.gov.justice.digital.hmpps.managerecallsapi.service.VirusScanResult
import java.net.URI
import java.time.OffsetDateTime

@RestController
@RequestMapping(produces = [APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_MANAGE_RECALLS')")
class DocumentController(
  @Autowired private val documentService: DocumentService,
  @Autowired private val recallNotificationService: RecallNotificationService,
  @Autowired private val dossierService: DossierService,
  @Autowired private val letterToPrisonService: LetterToPrisonService,
  @Autowired private val revocationOrderService: RevocationOrderService,
  @Autowired private val reasonsForRecallService: ReasonsForRecallService,
  @Autowired private val tokenExtractor: TokenExtractor,
  @Autowired private val userDetailsService: UserDetailsService,
  @Value("\${manage-recalls-api.base-uri}") private val baseUri: String
) {

  @GetMapping("/recalls/{recallId}/documents/{documentId}")
  fun getRecallDocument(
    @PathVariable("recallId") recallId: RecallId,
    @PathVariable("documentId") documentId: DocumentId
  ): ResponseEntity<GetDocumentResponse> {
    val (document, bytes) = documentService.getDocument(recallId, documentId)

    return ResponseEntity.ok(
      GetDocumentResponse(
        documentId,
        document.category,
        bytes.encodeToBase64String(),
        document.fileName,
        document.version,
        document.details,
        userDetailsService.get(document.createdByUserId()).fullName(),
        document.createdDateTime
      )
    )
  }

  @GetMapping("/recalls/{recallId}/documents")
  fun getRecallDocumentsByCategory(
    @PathVariable("recallId") recallId: RecallId,
    @RequestParam("category") category: DocumentCategory
  ): ResponseEntity<List<Api.RecallDocument>> {
    return ResponseEntity.ok(
      documentService.getAllDocumentsByCategory(recallId, category).map { document ->
        Api.RecallDocument(
          document.id(),
          document.category,
          document.fileName,
          document.version,
          document.details,
          document.createdDateTime,
          userDetailsService.get(document.createdByUserId()).fullName()
        )
      }
    )
  }

  @ApiResponses(
    ApiResponse(
      responseCode = "400",
      description = "Bad request, e.g. virus found exception",
      content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
    )
  )
  @PostMapping("/recalls/{recallId}/documents/uploaded")
  fun uploadDocument(
    @PathVariable("recallId") recallId: RecallId,
    @RequestBody uploadDocumentRequest: UploadDocumentRequest,
    @RequestHeader("Authorization") bearerToken: String
  ): ResponseEntity<NewDocumentResponse> {
    val token = tokenExtractor.getTokenFromHeader(bearerToken)
    return uploadDocument(recallId, token.userUuid(), uploadDocumentRequest).map { documentId ->
      ResponseEntity
        .created(URI.create("$baseUri/recalls/$recallId/documents/$documentId"))
        .body(NewDocumentResponse(documentId = documentId))
    }.onFailure {
      throw VirusFoundException()
    }
  }

  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = NewDocumentResponse::class))]
      )
    ]
  )
  @PostMapping("/recalls/{recallId}/documents/generated")
  fun generateDocument(
    @PathVariable("recallId") recallId: RecallId,
    @RequestBody generateDocumentRequest: GenerateDocumentRequest,
    @RequestHeader("Authorization") bearerToken: String
  ): Mono<ResponseEntity<NewDocumentResponse>> {
    val token = tokenExtractor.getTokenFromHeader(bearerToken)
    return generateDocument(recallId, token.userUuid(), generateDocumentRequest).map { documentId ->
      ResponseEntity
        .created(URI.create("$baseUri/recalls/$recallId/documents/$documentId"))
        .body(NewDocumentResponse(documentId = documentId))
    }
  }

  private fun generateDocument(
    recallId: RecallId,
    currentUserUuid: UserId,
    generateDocumentRequest: GenerateDocumentRequest
  ): Mono<DocumentId> {
    if (generateDocumentRequest.category.uploaded)
      throw WrongDocumentTypeException(generateDocumentRequest.category)

    return when (generateDocumentRequest.category) { // TODO: all the below service calls should be annotated @Transactional - in their respective service classes of course
      RECALL_NOTIFICATION -> recallNotificationService.generateAndStorePdf(recallId, currentUserUuid, generateDocumentRequest.details)
      REVOCATION_ORDER -> revocationOrderService.generateAndStorePdf(recallId, currentUserUuid, generateDocumentRequest.details)
      REASONS_FOR_RECALL -> reasonsForRecallService.generateAndStorePdf(recallId, currentUserUuid, generateDocumentRequest.details)
      DOSSIER -> dossierService.generateAndStorePdf(recallId, currentUserUuid, generateDocumentRequest.details)
      LETTER_TO_PRISON -> letterToPrisonService.generateAndStorePdf(recallId, currentUserUuid, generateDocumentRequest.details)
      else -> throw WrongDocumentTypeException(generateDocumentRequest.category)
    }
  }

  private fun uploadDocument(
    recallId: RecallId,
    currentUserId: UserId,
    uploadDocumentRequest: UploadDocumentRequest
  ): Result<DocumentId, VirusScanResult> {
    if (!uploadDocumentRequest.category.uploaded)
      throw WrongDocumentTypeException(uploadDocumentRequest.category)

    return documentService.scanAndStoreDocument(
      recallId,
      currentUserId,
      uploadDocumentRequest.fileContent.toBase64DecodedByteArray(),
      uploadDocumentRequest.category,
      uploadDocumentRequest.fileName,
      uploadDocumentRequest.details
    )
  }

  @PatchMapping("/recalls/{recallId}/documents/{documentId}")
  fun updateDocumentCategory(
    @PathVariable("recallId") recallId: RecallId,
    @PathVariable("documentId") documentId: DocumentId,
    @RequestBody updateDocumentRequest: UpdateDocumentRequest
  ): ResponseEntity<UpdateDocumentResponse> {
    val document = documentService.updateDocumentCategory(recallId, documentId, updateDocumentRequest.category)
    return ResponseEntity.ok(
      UpdateDocumentResponse(
        document.id(),
        document.recallId(),
        document.category,
        document.fileName,
        document.details
      )
    )
  }

  @DeleteMapping("/recalls/{recallId}/documents/{documentId}")
  @ResponseStatus(value = NO_CONTENT)
  fun deleteDocument(
    @PathVariable("recallId") recallId: RecallId,
    @PathVariable("documentId") documentId: DocumentId
  ) {
    documentService.deleteDocument(recallId, documentId)
  }
}

data class UpdateDocumentRequest(
  val category: DocumentCategory
)

data class UpdateDocumentResponse(
  val documentId: DocumentId,
  val recallId: RecallId,
  val category: DocumentCategory,
  val fileName: String,
  val details: String? = null,
)

data class UploadDocumentRequest(
  val category: DocumentCategory,
  val fileContent: String,
  val fileName: String,
  val details: String? = null
)

data class GenerateDocumentRequest(
  val category: DocumentCategory,
  val details: String?
)

data class NewDocumentResponse(val documentId: DocumentId)

data class GetDocumentResponse(
  val documentId: DocumentId,
  val category: DocumentCategory,
  val content: String,
  val fileName: String,
  val version: Int?,
  val details: String?,
  val createdByUserName: FullName,
  val createdDateTime: OffsetDateTime,
)
