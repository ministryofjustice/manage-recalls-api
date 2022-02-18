package uk.gov.justice.digital.hmpps.managerecallsapi.integration.db

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.startsWith
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Document
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.NOTE_DOCUMENT
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Note
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NoteId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomString
import java.time.OffsetDateTime
import javax.transaction.Transactional

class NoteRepositoryIntegrationTest(
  @Autowired private val documentRepository: DocumentRepository,
) : IntegrationTestBase() {

  // Note: when using @Transactional to clean up after the tests we need to 'flush' to trigger the DB constraints, hence use of saveAndFlush()
  @Test
  @Transactional
  fun `can save and flush one then a second note with valid index values for an existing recall`() {
    recallRepository.save(recall, currentUserId)

    val noteId1 = ::NoteId.random()
    val note1 = note(noteId1, 1)

    noteRepository.saveAndFlush(note1)
    val retrieved1 = noteRepository.getById(noteId1.value)

    assertThat(retrieved1, equalTo(note1))

    val noteId2 = ::NoteId.random()
    val note2 = note(noteId2, 2)

    noteRepository.saveAndFlush(note2)
    val retrieved2 = noteRepository.getById(noteId2.value)

    assertThat(retrieved2, equalTo(note2))
  }

  @Test
  @Transactional
  fun `can save and flush a note with a non-null documentId when NOTE_DOCUMENT document already stored for an existing recall`() {
    recallRepository.save(recall, currentUserId)
    val documentId = ::DocumentId.random()
    documentRepository.saveAndFlush(
      Document(
        documentId,
        recallId,
        NOTE_DOCUMENT,
        randomString(),
        null,
        null,
        OffsetDateTime.now(),
        createdByUserId
      )
    )

    val noteId = ::NoteId.random()
    val note = note(noteId, 1, documentId)

    noteRepository.saveAndFlush(note)
    val retrieved1 = noteRepository.getById(noteId.value)

    assertThat(retrieved1, equalTo(note))
  }

  @Test
  @Transactional
  fun `cannot save and flush a note with a non-null documentId when document NOT already stored for an existing recall`() {
    recallRepository.save(recall, currentUserId)
    val documentId = ::DocumentId.random()

    val noteId = ::NoteId.random()
    val note = note(noteId, 1, documentId)

    val thrown = assertThrows<DataIntegrityViolationException> {
      noteRepository.saveAndFlush(note)
    }
    assertThat(thrown.message!!, startsWith("could not execute statement"))
  }

  @Test
  @Transactional
  fun `cannot save and flush two notes with the same index value for an existing recall`() {
    recallRepository.save(recall, currentUserId)

    val noteId1 = ::NoteId.random()
    val note1 = note(noteId1, 1)

    noteRepository.saveAndFlush(note1)
    val retrieved1 = noteRepository.getById(noteId1.value)

    assertThat(retrieved1, equalTo(note1))

    val noteId2 = ::NoteId.random()
    val note2 = note(noteId2, 1)

    val thrown = assertThrows<DataIntegrityViolationException> {
      noteRepository.saveAndFlush(note2)
    }
    assertThat(thrown.message!!, startsWith("could not execute statement"))
  }

  @Test
  @Transactional
  fun `cannot save and flush a note with an invalid(0) index value for an existing recall`() {
    recallRepository.save(recall, currentUserId)

    val noteId = ::NoteId.random()
    val note = note(noteId, 0)

    val thrown = assertThrows<DataIntegrityViolationException> {
      noteRepository.saveAndFlush(note)
    }
    assertThat(thrown.message!!, startsWith("could not execute statement"))
  }

  @Test
  @Transactional
  fun `cannot save and flush a note with an invalid(negative) index value for an existing recall`() {
    recallRepository.save(recall, currentUserId)

    val noteId = ::NoteId.random()
    val note = note(noteId, -1)

    val thrown = assertThrows<DataIntegrityViolationException> {
      noteRepository.saveAndFlush(note)
    }
    assertThat(thrown.message!!, startsWith("could not execute statement"))
  }

  private fun note(
    noteId: NoteId,
    index: Int,
    documentId: DocumentId? = null
  ) = Note(
    noteId,
    recallId,
    randomString(),
    randomString(),
    index,
    documentId,
    createdByUserId,
    OffsetDateTime.now()
  )
}
