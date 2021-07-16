package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import org.springframework.beans.factory.annotation.Autowired
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
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RevocationOrderService
import java.util.Base64
import java.util.UUID
import javax.validation.Valid
import javax.validation.constraints.NotBlank

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_MANAGE_RECALLS')")
class RecallsController(
  @Autowired private val recallRepository: RecallRepository,
  @Autowired private val revocationOrderService: RevocationOrderService
) {

  @PostMapping("/recalls")
  fun bookRecall(@Valid @RequestBody bookRecallRequest: BookRecallRequest) =
    ResponseEntity(
      recallRepository.save(bookRecallRequest.toRecall()).toResponse(), HttpStatus.CREATED
    )

  @GetMapping("/recalls")
  fun findAll(): List<RecallResponse> = recallRepository.findAll().map { it.toResponse() }

  @GetMapping("/recalls/{recallId}")
  fun getRecall(@PathVariable("recallId") recallId: UUID): RecallResponse =
    recallRepository.getById(recallId).toResponse()

  @GetMapping("/recalls/{recallId}/revocationOrder")
  fun getRevocationOrder(@PathVariable("recallId") recallId: UUID): Mono<ResponseEntity<Pdf>> =
    revocationOrderService.getRevocationOrder(recallId)
      .map {
        val pdfBase64Encoded = Base64.getEncoder().encodeToString(it)
        ResponseEntity.ok(Pdf(pdfBase64Encoded))
      }
}

fun BookRecallRequest.toRecall() = Recall(UUID.randomUUID(), this.nomsNumber)

fun Recall.toResponse() = RecallResponse(this.id, this.nomsNumber, this.revocationOrderDocS3Key)

data class BookRecallRequest(@field:NotBlank val nomsNumber: String)

data class RecallResponse(val id: UUID, val nomsNumber: String, val revocationOrderId: UUID?)

data class Pdf(val content: String)
