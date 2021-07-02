package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import java.util.UUID

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_MANAGE_RECALLS')")
class RecallsController(
  @Autowired private val recallRepository: RecallRepository
) {

  @PostMapping("/recalls")
  fun bookRecall(@RequestBody bookRecallRequest: BookRecallRequest) =
    ResponseEntity(
      recallRepository.save(bookRecallRequest.toRecall()).toResponse(), HttpStatus.CREATED
    )

  @GetMapping("/recalls")
  fun findAll(): MutableList<Recall> = recallRepository.findAll()
}

fun BookRecallRequest.toRecall() = Recall(UUID.randomUUID(), this.nomsNumber)

fun Recall.toResponse() = BookRecallResponse(this.id, this.nomsNumber)

data class BookRecallRequest(val nomsNumber: String)

data class BookRecallResponse(val id: UUID, val nomsNumber: String)
