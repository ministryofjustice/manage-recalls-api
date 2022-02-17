package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.extractor.TokenExtractor
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.extractor.TokenExtractor.Token
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.encodeToBase64String
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NoteId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomString
import uk.gov.justice.digital.hmpps.managerecallsapi.service.NoteService

class NoteControllerTest {

  private val noteService = mockk<NoteService>()
  private val tokenExtractor = mockk<TokenExtractor>()

  private val bearerToken = "BEARER TOKEN"
  private val userId = ::UserId.random()

  private val underTest = NoteController(
    noteService,
    tokenExtractor
  )

  private val recallId = ::RecallId.random()

  @Test
  fun `create note calls service with recallId, userId from bearer token and request object`() {
    val noteId = ::NoteId.random()

    val request = NoteController.CreateNoteRequest(
      randomString(),
      randomString(),
      randomString(),
      randomString().encodeToBase64String(),
    )

    every { tokenExtractor.getTokenFromHeader(bearerToken) } returns Token(userId.toString())
    every { noteService.createNote(recallId, userId, request) } returns noteId

    val result = underTest.createNote(recallId, request, bearerToken)

    assertThat(result, equalTo(noteId))
  }
}
