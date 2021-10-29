package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import com.natpryce.hamkrest.allElements
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import dev.forkhandles.result4k.Success
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocument
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.PART_A_RECALL_REPORT
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.encodeToBase64String
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.service.DocumentService
import java.time.OffsetDateTime

class DocumentControllerTest {
  private val documentService = mockk<DocumentService>()
  private val advertisedBaseUri = "https://api"

  private val underTest = DocumentController(documentService, advertisedBaseUri)

  private val recallId = ::RecallId.random()
  private val fileName = "fileName"

  @Test
  fun `adds a recall document successfully`() {
    val document = "a document"
    val documentBytes = document.toByteArray()
    val category = PART_A_RECALL_REPORT
    val documentId = ::DocumentId.random()

    every {
      documentService.scanAndStoreDocument(
        recallId,
        documentBytes,
        category,
        fileName
      )
    } returns Success(documentId)

    val request = AddDocumentRequest(category, documentBytes.encodeToBase64String(), fileName)
    val response = underTest.addDocument(recallId, request)

    assertThat(response.statusCode, equalTo(HttpStatus.CREATED))
    assertThat(
      response.headers["Location"]?.toList(),
      present(allElements(equalTo("$advertisedBaseUri/recalls/$recallId/documents/$documentId")))
    )
    assertThat(response.body, equalTo(AddDocumentResponse(documentId)))
  }

  @Test
  fun `gets a document`() {
    val recallId1 = ::RecallId.random()
    val documentId = ::DocumentId.random()
    val aRecallDocument = RecallDocument(
      documentId,
      recallId1,
      PART_A_RECALL_REPORT,
      fileName,
      1,
      OffsetDateTime.now()
    )
    val bytes = "Hello".toByteArray()

    every { documentService.getDocument(recallId1, documentId) } returns Pair(aRecallDocument, bytes)

    val response = underTest.getRecallDocument(recallId1, documentId)

    assertThat(response.statusCode, equalTo(HttpStatus.OK))
    val expected = GetDocumentResponse(
      documentId,
      aRecallDocument.category,
      content = bytes.encodeToBase64String(),
      fileName
    )
    assertThat(response.body, equalTo(expected))
  }
}
