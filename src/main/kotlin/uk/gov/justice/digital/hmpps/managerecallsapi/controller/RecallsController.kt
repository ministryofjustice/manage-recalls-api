package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.service.NotFoundException
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallDocumentService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallNotFoundException
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RevocationOrderService
import java.net.URI
import java.util.Base64
import java.util.UUID

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_MANAGE_RECALLS')")
class RecallsController(
  @Autowired private val recallRepository: RecallRepository,
  @Autowired private val revocationOrderService: RevocationOrderService,
  @Autowired private val recallDocumentService: RecallDocumentService,
  @Value("\${manage-recalls-api.base-uri}") private val baseUri: String
) {

  @PostMapping("/recalls")
  fun bookRecall(@RequestBody bookRecallRequest: BookRecallRequest) =
    ResponseEntity(
      recallRepository.save(bookRecallRequest.toRecall()).toResponse(),
      HttpStatus.CREATED
    )

  @GetMapping("/recalls")
  fun findAll(): List<RecallResponse> = recallRepository.findAll().map { it.toResponse() }

  @GetMapping("/recalls/{recallId}")
  fun getRecall(@PathVariable("recallId") recallId: RecallId): RecallResponse =
    recallRepository.getByRecallId(recallId).toResponse()

  @GetMapping("/recalls/{recallId}/revocationOrder")
  fun getRevocationOrder(@PathVariable("recallId") recallId: RecallId): Mono<ResponseEntity<Pdf>> =
    revocationOrderService.getRevocationOrder(recallId)
      .map {
        val pdfBase64Encoded = Base64.getEncoder().encodeToString(it)
        ResponseEntity.ok(Pdf(pdfBase64Encoded))
      }

  @PostMapping("/recalls/{recallId}/documents")
  fun addDocument(
    @PathVariable("recallId") recallId: RecallId,
    @RequestBody body: AddDocumentRequest
  ): ResponseEntity<AddDocumentResponse> {
    val fileS3Key = try {
      recallDocumentService.addDocumentToRecall(
        recallId = recallId,
        documentBytes = Base64.getDecoder().decode(body.fileContent),
        documentCategory = RecallDocumentCategory.valueOf(body.category)
      )
    } catch (e: RecallNotFoundException) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message, e)
    }

    return ResponseEntity
      .created(URI.create("$baseUri/recalls/$recallId/documents/$fileS3Key"))
      .body(AddDocumentResponse(documentId = fileS3Key))
  }

  @GetMapping("/recalls/{recallId}/documents/{documentId}")
  fun getRecallDocument(
    @PathVariable("recallId") recallId: RecallId,
    @PathVariable("documentId") documentId: UUID
  ): ResponseEntity<GetDocumentResponse> {
    try {
      val (document, bytes) = recallDocumentService.getDocument(recallId, documentId)
      return ResponseEntity.ok(
        GetDocumentResponse(
          documentId = documentId,
          category = document.category,
          content = Base64.getEncoder().encodeToString(bytes)
        )
      )
    } catch (e: NotFoundException) {
      throw ResponseStatusException(HttpStatus.NOT_FOUND, e.message, e)
    }
  }
}

fun BookRecallRequest.toRecall() = Recall(::RecallId.random(), this.nomsNumber)

fun Recall.toResponse() = RecallResponse(
  recallId = this.recallId(),
  nomsNumber = this.nomsNumber,
  revocationOrderId = this.revocationOrderDocS3Key,
  documents = this.documents.map { doc -> ApiRecallDocument(doc.id, doc.category) },
  agreeWithRecallRecommendation = this.agreeWithRecallRecommendation,
  recallLength = this.recallLength
)

data class BookRecallRequest(val nomsNumber: NomsNumber)

data class RecallResponse(
  val recallId: RecallId,
  val nomsNumber: NomsNumber,
  val revocationOrderId: UUID?,
  val documents: List<ApiRecallDocument>,
  val agreeWithRecallRecommendation: Boolean?,
  val recallLength: RecallLength? = null
)

data class ApiRecallDocument(
  val documentId: UUID,
  val category: RecallDocumentCategory
)

data class Pdf(val content: String)

data class AddDocumentRequest(val category: String, val fileContent: String)

data class AddDocumentResponse(val documentId: UUID)

data class GetDocumentResponse(
  val documentId: UUID,
  val category: RecallDocumentCategory,
  val content: String
)
