package uk.gov.justice.digital.hmpps.managerecallsapi.controller

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
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.extractor.TokenExtractor
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NoteId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.service.NoteService

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_MANAGE_RECALLS')")
class NoteController(
  @Autowired private val noteService: NoteService,
  @Autowired private val tokenExtractor: TokenExtractor
) {

  @PostMapping("/recalls/{recallId}/notes")
  @ResponseStatus(HttpStatus.CREATED)
  fun createNote(
    @PathVariable("recallId") recallId: RecallId,
    @RequestBody request: CreateNoteRequest,
    @RequestHeader("Authorization") bearerToken: String
  ): NoteId =
    tokenExtractor.getTokenFromHeader(bearerToken).userUuid().let { currentUserId ->
      noteService.createNote(recallId, currentUserId, request)
    }

  data class CreateNoteRequest(
    val subject: String,
    val details: String,
    val fileName: String?,
    val fileContent: String?,
  )
}
