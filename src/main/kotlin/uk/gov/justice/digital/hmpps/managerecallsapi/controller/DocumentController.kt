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
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.managerecallsapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.encodeToBase64String
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.toBase64DecodedByteArray
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.service.DocumentService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.VirusFoundException
import java.net.URI
import java.time.OffsetDateTime

@RestController
@RequestMapping(produces = [APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_MANAGE_RECALLS')")
class DocumentController(
  @Autowired private val documentService: DocumentService,
  @Value("\${manage-recalls-api.base-uri}") private val baseUri: String
) {

  @GetMapping("/recalls/{recallId}/documents/{documentId}")
  fun getRecallDocument(
    @PathVariable("recallId") recallId: RecallId,
    @PathVariable("documentId") documentId: DocumentId
  ): ResponseEntity<GetDocumentResponse> {
    val (document, bytes) = documentService.getDocument(recallId, documentId)
    return ResponseEntity.ok(
      GetDocumentResponse(documentId, document.category, bytes.encodeToBase64String(), document.fileName, document.version, document.createdDateTime)
    )
  }

  @ApiResponses(
    ApiResponse(
      code = 400, message = "Bad request, e.g. virus scan returns error", response = ErrorResponse::class,
      examples = Example(ExampleProperty(mediaType = "application/json", value = "{\n\"status\": 400,\n\"message\":\"VirusFoundException\"\n}"))
    )
  )
  // TODO:  Restrict the types of documents that can be uploaded. i.e. RECALL_NOTIFICATION, REVOCATION_ORDER
  @PostMapping("/recalls/{recallId}/documents")
  fun addDocument(
    @PathVariable("recallId") recallId: RecallId,
    @RequestBody addDocumentRequest: AddDocumentRequest
  ) =
    uploadDocument(recallId, addDocumentRequest).map { documentId ->
      ResponseEntity
        .created(URI.create("$baseUri/recalls/$recallId/documents/$documentId"))
        .body(AddDocumentResponse(documentId = documentId))
    }.onFailure {
      throw VirusFoundException()
    }

  private fun uploadDocument(
    recallId: RecallId,
    addDocumentRequest: AddDocumentRequest
  ) = documentService.scanAndStoreDocument(
    recallId,
    addDocumentRequest.fileContent.toBase64DecodedByteArray(),
    addDocumentRequest.category,
    addDocumentRequest.fileName
  )

  @PatchMapping("/recalls/{recallId}/documents/{documentId}")
  fun updateDocumentCategory(
    @PathVariable("recallId") recallId: RecallId,
    @PathVariable("documentId") documentId: DocumentId,
    @RequestBody updateDocumentRequest: UpdateDocumentRequest
  ): ResponseEntity<UpdateDocumentResponse> {
    val document = documentService.updateDocumentCategory(recallId, documentId, updateDocumentRequest.category)
    return ResponseEntity.ok(UpdateDocumentResponse(document.id(), document.recallId(), document.category, document.fileName))
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
  val category: RecallDocumentCategory
)

data class UpdateDocumentResponse(
  val documentId: DocumentId,
  val recallId: RecallId,
  val category: RecallDocumentCategory,
  val fileName: String
)

data class AddDocumentRequest(
  val category: RecallDocumentCategory,
  val fileContent: String,
  val fileName: String
)

data class AddDocumentResponse(val documentId: DocumentId)

data class GetDocumentResponse(
  val documentId: DocumentId,
  val category: RecallDocumentCategory,
  val content: String,
  val fileName: String,
  val version: Int?,
  val createdDateTime: OffsetDateTime
)
