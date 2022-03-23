package uk.gov.justice.digital.hmpps.managerecallsapi.service

import dev.forkhandles.result4k.onFailure
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.NoteController.CreateNoteRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Note
import uk.gov.justice.digital.hmpps.managerecallsapi.db.NoteRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.toBase64DecodedByteArray
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NoteId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import java.time.OffsetDateTime
import javax.transaction.Transactional

@Service
class NoteService(
  @Autowired private val noteRepository: NoteRepository,
  @Autowired private val recallRepository: RecallRepository,
  @Autowired private val documentService: DocumentService,
) {

  @Transactional
  fun createNote(
    recallId: RecallId,
    currentUserId: UserId,
    request: CreateNoteRequest
  ): NoteId =
    recallRepository.getByRecallId(recallId).let { recall ->
      // TODO: improve on this validation with PUD-1626 Cross-property Request validation: move to DTOs or restructure Request classes?
      val fileName = request.fileName
      val fileContent = request.fileContent
      if ((fileName == null && !fileContent.isNullOrBlank()) || (fileContent.isNullOrBlank() && fileName != null)) {
        throw IllegalArgumentException("Both fileName and fileContent must be supplied as non-blank or neither")
      }
      val documentId = fileName?.let {
        documentService.scanAndStoreDocument(
          recallId,
          currentUserId,
          request.fileContent!!.toBase64DecodedByteArray(),
          DocumentCategory.NOTE_DOCUMENT,
          it,
        ).onFailure {
          throw VirusFoundException()
        }
      }
      val previousIndex = recall.notes.maxByOrNull { it.index }?.index ?: 0
      val note = noteRepository.save(
        Note(
          ::NoteId.random(),
          recallId,
          request.subject,
          request.details,
          previousIndex + 1,
          documentId,
          currentUserId,
          OffsetDateTime.now(),
        )
      )
      note.id()
    }
}
