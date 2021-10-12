package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.recover
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.toBase64DecodedByteArray
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallDocumentService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallNotFoundException
import java.net.URI
import java.util.UUID

@RestController
@RequestMapping(produces = [APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_MANAGE_RECALLS')")
class AddDocumentController(
  @Autowired private val recallDocumentService: RecallDocumentService,
  @Value("\${manage-recalls-api.base-uri}") private val baseUri: String
) {

  @PostMapping("/recalls/{recallId}/documents")
  fun addDocument(
    @PathVariable("recallId") recallId: RecallId,
    @RequestBody body: AddDocumentRequest
  ): ResponseEntity<AddDocumentResponse> =
    // TODO:  Restrict the types of documents that can be uploaded. i.e. RECALL_NOTIFICATION, REVOCATION_ORDER
    uploadDocument(recallId, body).map { documentId ->
      ResponseEntity
        .created(URI.create("$baseUri/recalls/$recallId/documents/$documentId"))
        .body(AddDocumentResponse(documentId = documentId))
    }.recover {
      ResponseEntity.badRequest().build()
    }

  private fun uploadDocument(
    recallId: RecallId,
    body: AddDocumentRequest
  ) = try {
    recallDocumentService.scanAndStoreDocument(
      recallId = recallId,
      documentBytes = body.fileContent.toBase64DecodedByteArray(),
      documentCategory = body.category,
      fileName = body.fileName
    )
  } catch (e: RecallNotFoundException) {
    // Anyone object ot getting rid of this and letting the default NotFoundException return NOT_FOUND?
    throw ResponseStatusException(BAD_REQUEST, e.message, e)
  }
}

data class AddDocumentRequest(
  val category: RecallDocumentCategory,
  val fileContent: String,
  val fileName: String? = null
)

data class AddDocumentResponse(val documentId: UUID)
