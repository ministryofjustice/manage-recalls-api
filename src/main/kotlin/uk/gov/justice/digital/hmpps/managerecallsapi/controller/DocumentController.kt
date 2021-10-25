package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.recover
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.toBase64DecodedByteArray
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.service.DocumentService
import java.net.URI
import java.util.UUID

@RestController
@RequestMapping(produces = [APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_MANAGE_RECALLS')")
class DocumentController(
  @Autowired private val documentService: DocumentService,
  @Value("\${manage-recalls-api.base-uri}") private val baseUri: String
) {

  // TODO:  Restrict the types of documents that can be uploaded. i.e. RECALL_NOTIFICATION, REVOCATION_ORDER
  @PostMapping("/recalls/{recallId}/documents")
  fun addDocument(
    @PathVariable("recallId") recallId: RecallId,
    @RequestBody addDocumentRequest: AddDocumentRequest
  ): ResponseEntity<AddDocumentResponse> =
    uploadDocument(recallId, addDocumentRequest).map { documentId ->
      ResponseEntity
        .created(URI.create("$baseUri/recalls/$recallId/documents/$documentId"))
        .body(AddDocumentResponse(documentId = documentId))
    }.recover {
      ResponseEntity.badRequest().build()
    }

  private fun uploadDocument(
    recallId: RecallId,
    addDocumentRequest: AddDocumentRequest
  ) = documentService.scanAndStoreDocument(
    recallId = recallId,
    documentBytes = addDocumentRequest.fileContent.toBase64DecodedByteArray(),
    documentCategory = addDocumentRequest.category,
    fileName = addDocumentRequest.fileName
  )
}

data class AddDocumentRequest(
  val category: RecallDocumentCategory,
  val fileContent: String,
  val fileName: String
)

data class AddDocumentResponse(val documentId: UUID)
