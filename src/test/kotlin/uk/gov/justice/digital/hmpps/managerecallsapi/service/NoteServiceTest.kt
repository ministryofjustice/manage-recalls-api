package uk.gov.justice.digital.hmpps.managerecallsapi.service

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.managerecallsapi.config.RecallNotFoundException
import uk.gov.justice.digital.hmpps.managerecallsapi.config.VirusFoundException
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.NoteController.CreateNoteRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Note
import uk.gov.justice.digital.hmpps.managerecallsapi.db.NoteRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.encodeToBase64String
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FileName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NoteId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomString

class NoteServiceTest {
  private val recallRepository = mockk<RecallRepository>()
  private val noteRepository = mockk<NoteRepository>()
  private val documentService = mockk<DocumentService>()

  private val userId = ::UserId.random()
  private val subject = "note subject"
  private val details = "note details"
  private val documentBytes = "a document".toByteArray()
  private val fileName = FileName("file.doc")
  private val documentId = ::DocumentId.random()

  private val underTest = NoteService(
    noteRepository,
    recallRepository,
    documentService,
  )

  private val recallId = ::RecallId.random()

  @Test
  fun `stores first note for recall as index 1`() {
    val recall = mockk<Recall>()
    val savedNoteSlot = slot<Note>()
    val mockNote = mockk<Note>()
    val mockNoteId = mockk<NoteId>()

    every { recall.notes } returns emptySet()
    every { recallRepository.getByRecallId(recallId) } returns recall
    every {
      documentService.scanAndStoreDocument(
        recallId,
        userId,
        documentBytes,
        DocumentCategory.NOTE_DOCUMENT,
        fileName
      )
    } returns Success(documentId)
    every { noteRepository.save(capture(savedNoteSlot)) } returns mockNote
    every { mockNote.id() } returns mockNoteId
    every { mockNote.index } returns 1

    val request = CreateNoteRequest(subject, details, fileName, documentBytes.encodeToBase64String())

    val response = underTest.createNote(recallId, userId, request)

    assertThat(savedNoteSlot.captured.subject, equalTo(subject))
    assertThat(savedNoteSlot.captured.details, equalTo(details))
    assertThat(savedNoteSlot.captured.index, equalTo(1))
    assertThat(savedNoteSlot.captured.documentId, equalTo(documentId.value))
    assertThat(savedNoteSlot.captured.createdByUserId, equalTo(userId.value))
    assertThat(response, equalTo(mockNoteId))
  }

  @Test
  fun `stores note also if document is not provided`() {
    val recall = mockk<Recall>()
    val savedNoteSlot = slot<Note>()
    val mockNote = mockk<Note>()
    val mockNoteId = mockk<NoteId>()

    every { recall.notes } returns emptySet()
    every { recallRepository.getByRecallId(recallId) } returns recall
    every { noteRepository.save(capture(savedNoteSlot)) } returns mockNote
    every { mockNote.id() } returns mockNoteId
    every { mockNote.index } returns 1

    val request = CreateNoteRequest(subject, details, null, null)

    val response = underTest.createNote(recallId, userId, request)

    assertThat(savedNoteSlot.captured.subject, equalTo(subject))
    assertThat(savedNoteSlot.captured.details, equalTo(details))
    assertThat(savedNoteSlot.captured.index, equalTo(1))
    assertThat(savedNoteSlot.captured.documentId, equalTo(null))
    assertThat(savedNoteSlot.captured.createdByUserId, equalTo(userId.value))
    assertThat(response, equalTo(mockNoteId))
  }

  @Test
  fun `store record as index 2 if recall already has a note with index 1`() {
    val recall = mockk<Recall>()
    val documentId = ::DocumentId.random()
    val savedNoteSlot = slot<Note>()
    val noteId = ::NoteId.random()
    val existingNote = mockk<Note>()

    every { recall.notes } returns setOf(existingNote)
    every { existingNote.index } returns 1
    every { recallRepository.getByRecallId(recallId) } returns recall
    every {
      documentService.scanAndStoreDocument(
        recallId,
        userId,
        documentBytes,
        DocumentCategory.NOTE_DOCUMENT,
        fileName
      )
    } returns Success(documentId)
    every { noteRepository.save(capture(savedNoteSlot)) } returns existingNote
    every { existingNote.id() } returns noteId
    every { existingNote.index } returns 1

    val request = CreateNoteRequest(subject, details, fileName, documentBytes.encodeToBase64String())

    val response = underTest.createNote(recallId, userId, request)

    assertThat(savedNoteSlot.captured.subject, equalTo(subject))
    assertThat(savedNoteSlot.captured.details, equalTo(details))
    assertThat(savedNoteSlot.captured.index, equalTo(2))
    assertThat(response, equalTo(noteId))
  }

  @Test
  fun `VirusFoundException thrown if document scan returns Failure`() {
    val recall = mockk<Recall>()

    every { recallRepository.getByRecallId(recallId) } returns recall
    every {
      documentService.scanAndStoreDocument(
        recallId,
        userId,
        documentBytes,
        DocumentCategory.NOTE_DOCUMENT,
        fileName
      )
    } returns Failure(mockk())

    val request = CreateNoteRequest(randomString(), randomString(), fileName, documentBytes.encodeToBase64String())

    assertThrows<VirusFoundException> { underTest.createNote(recallId, userId, request) }
  }

  @Test
  fun `NotFoundException thrown if recall does not exist on note creation`() {
    every { recallRepository.getByRecallId(recallId) } throws RecallNotFoundException(recallId)

    val request = CreateNoteRequest(randomString(), randomString(), FileName(randomString()), randomString().toByteArray().encodeToBase64String())

    assertThrows<RecallNotFoundException> { underTest.createNote(recallId, userId, request) }
  }

  @Test
  fun `IllegalArgumentException thrown if note has fileName but empty fileContent`() {
    val recall = mockk<Recall>()
    every { recallRepository.getByRecallId(recallId) } returns recall

    val request = CreateNoteRequest(randomString(), randomString(), FileName(randomString()), "".toByteArray().encodeToBase64String())

    assertThrows<IllegalArgumentException> { underTest.createNote(recallId, userId, request) }
  }

  @Test
  fun `IllegalArgumentException thrown if note has fileContent but null fileName`() {
    val recall = mockk<Recall>()
    every { recallRepository.getByRecallId(recallId) } returns recall

    val request = CreateNoteRequest(randomString(), randomString(), null, randomString().toByteArray().encodeToBase64String())

    assertThrows<IllegalArgumentException> { underTest.createNote(recallId, userId, request) }
  }
}
