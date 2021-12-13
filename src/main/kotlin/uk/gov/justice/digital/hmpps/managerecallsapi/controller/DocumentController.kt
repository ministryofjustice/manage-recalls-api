package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.onFailure
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import io.swagger.annotations.Example
import io.swagger.annotations.ExampleProperty
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
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier.DossierService
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.encodeToBase64String
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.lettertoprison.LetterToPrisonService
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.recallnotification.RecallNotificationService
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.toBase64DecodedByteArray
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FullName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.service.DocumentService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.UserDetailsService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.VirusFoundException
import java.net.URI
import java.time.OffsetDateTime

@RestController
@RequestMapping(produces = [APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_MANAGE_RECALLS')")
class DocumentController(
  @Autowired private val documentService: DocumentService,
  @Autowired private val recallNotificationService: RecallNotificationService,
  @Autowired private val dossierService: DossierService,
  @Autowired private val letterToPrison: LetterToPrisonService,
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

  @GetMapping("/recalls/{recallId}/recallNotification")
  fun getRecallNotification(
    @PathVariable("recallId") recallId: RecallId,
    @RequestHeader("Authorization") bearerToken: String
  ): Mono<ResponseEntity<Pdf>> {
    val token = tokenExtractor.getTokenFromHeader(bearerToken)
    return recallNotificationService.getOrCreatePdf(recallId, token.userUuid()).map {
      ResponseEntity.ok(Pdf.encode(it))
    }
  }

  @GetMapping("/recalls/{recallId}/dossier")
  fun getDossier(
    @PathVariable("recallId") recallId: RecallId,
    @RequestHeader("Authorization") bearerToken: String
  ): Mono<ResponseEntity<Pdf>> {
    val token = tokenExtractor.getTokenFromHeader(bearerToken)
    return dossierService.getOrCreatePdf(recallId, token.userUuid()).map {
      ResponseEntity.ok(Pdf.encode(it))
    }
  }

  @GetMapping("/recalls/{recallId}/letter-to-prison")
  fun getLetterToPrison(
    @PathVariable("recallId") recallId: RecallId,
    @RequestHeader("Authorization") bearerToken: String
  ): Mono<ResponseEntity<Pdf>> {
    val token = tokenExtractor.getTokenFromHeader(bearerToken)
    return letterToPrison.getPdf(recallId, token.userUuid()).map {
      ResponseEntity.ok(Pdf.encode(it))
    }
  }

  @ApiResponses(
    ApiResponse(
      code = 400, message = "Bad request, e.g. virus scan returns error", response = ErrorResponse::class,
      examples = Example(
        ExampleProperty(
          mediaType = "application/json",
          value = "{\n\"status\": 400,\n\"message\":\"VirusFoundException\"\n}"
        )
      )
    )
  )
  // TODO:  Restrict the types of documents that can be uploaded. i.e. RECALL_NOTIFICATION, REVOCATION_ORDER
  @PostMapping("/recalls/{recallId}/documents")
  fun uploadDocument(
    @PathVariable("recallId") recallId: RecallId,
    @RequestBody addDocumentRequest: AddDocumentRequest,
    @RequestHeader("Authorization") bearerToken: String
  ): ResponseEntity<AddDocumentResponse> {
    val token = tokenExtractor.getTokenFromHeader(bearerToken)
    return uploadDocument(recallId, token.userUuid(), addDocumentRequest).map { documentId ->
      ResponseEntity
        .created(URI.create("$baseUri/recalls/$recallId/documents/$documentId"))
        .body(AddDocumentResponse(documentId = documentId))
    }.onFailure {
      throw VirusFoundException()
    }
  }

  @PostMapping("/recalls/{recallId}/documents/create")
  fun createDocument(
    @PathVariable("recallId") recallId: RecallId,
    @RequestBody createDocumentRequest: CreateDocumentRequest,
    @RequestHeader("Authorization") bearerToken: String
  ): Mono<ResponseEntity<AddDocumentResponse>> {
    val token = tokenExtractor.getTokenFromHeader(bearerToken)
    return createDocument(recallId, token.userUuid(), createDocumentRequest).map { documentId ->
      ResponseEntity
        .created(URI.create("$baseUri/recalls/$recallId/documents/$documentId"))
        .body(AddDocumentResponse(documentId = documentId))
    }
  }

  private fun createDocument(
    recallId: RecallId,
    userUuid: UserId,
    createDocumentRequest: CreateDocumentRequest
  ): Mono<DocumentId> {
    if (createDocumentRequest.category.uploaded)
      throw WrongDocumentTypeException(createDocumentRequest.category)

    return when (createDocumentRequest.category) {
      DocumentCategory.RECALL_NOTIFICATION -> recallNotificationService.createAndStorePdf(recallId, userUuid, createDocumentRequest.details)
        .map { it.first }
      else -> throw WrongDocumentTypeException(createDocumentRequest.category)
    }
  }

  private fun uploadDocument(
    recallId: RecallId,
    createdByUserId: UserId,
    addDocumentRequest: AddDocumentRequest
  ) = documentService.scanAndStoreDocument(
    recallId,
    createdByUserId,
    addDocumentRequest.fileContent.toBase64DecodedByteArray(),
    addDocumentRequest.category,
    addDocumentRequest.fileName,
    addDocumentRequest.details
  )

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

data class AddDocumentRequest(
  val category: DocumentCategory,
  val fileContent: String,
  val fileName: String,
  val details: String? = null
)

data class CreateDocumentRequest(
  val category: DocumentCategory,
  val details: String
)

data class AddDocumentResponse(val documentId: DocumentId)

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
