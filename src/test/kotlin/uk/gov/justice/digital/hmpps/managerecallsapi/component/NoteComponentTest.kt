package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isEmpty
import com.natpryce.hamkrest.present
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.managerecallsapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.BookRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.NoteController.CreateNoteRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.encodeToBase64String
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CroNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomFileName
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomString
import java.time.LocalDate

class NoteComponentTest : ComponentTestBase() {
  private val nomsNumber = NomsNumber("123456")
  private val bookRecallRequest = BookRecallRequest(
    nomsNumber,
    FirstName("Barrie"),
    null,
    LastName("Badger"),
    CroNumber("1234/56A"),
    LocalDate.now()
  )
  private val subject = "Note subject"
  private val details = "Note details"
  private val documentContents = randomString().toByteArray()
  private val base64EncodedDocumentContents = documentContents.encodeToBase64String()
  private val fileName = randomFileName()

  @Test
  fun `create the first Note for a recall and verify on get recall`() {
    expectNoVirusesWillBeFound()
    val recall = authenticatedClient.bookRecall(bookRecallRequest)
    val createRequest = CreateNoteRequest(
      subject,
      details,
      fileName,
      base64EncodedDocumentContents,
    )

    assertThat(recall.notes, isEmpty)

    val noteId = authenticatedClient.createNote(recall.recallId, createRequest)
    val recallWithNote = authenticatedClient.getRecall(recall.recallId)

    assertThat(recallWithNote.notes.size, equalTo(1))
    assertThat(recallWithNote.notes.first().noteId, equalTo(noteId))
    assertThat(recallWithNote.notes.first().subject, equalTo(subject))
    assertThat(recallWithNote.notes.first().details, equalTo(details))
    assertThat(recallWithNote.notes.first().index, equalTo(1))
    assertThat(recallWithNote.notes.first().documentId, present())
    assertThat(recallWithNote.notes.first().fileName, equalTo(fileName))
  }

  @Test
  fun `add a document with a virus returns bad request with body`() {
    expectAVirusWillBeFound()

    val recall = authenticatedClient.bookRecall(bookRecallRequest)

    val createRequest = CreateNoteRequest(
      subject,
      details,
      fileName,
      base64EncodedDocumentContents,
    )

    val result = authenticatedClient.createNote(recall.recallId, createRequest, HttpStatus.BAD_REQUEST)
      .expectBody(ErrorResponse::class.java).returnResult().responseBody!!

    assertThat(result, equalTo(ErrorResponse(HttpStatus.BAD_REQUEST, "VirusFoundException")))
  }
}
