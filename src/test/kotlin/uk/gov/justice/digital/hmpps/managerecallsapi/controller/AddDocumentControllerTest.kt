package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import com.natpryce.hamkrest.allElements
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import dev.forkhandles.result4k.Success
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.PART_A_RECALL_REPORT
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.encodeToBase64String
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallDocumentService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallNotFoundException
import java.util.UUID

class AddDocumentControllerTest {
  private val recallDocumentService = mockk<RecallDocumentService>()
  private val advertisedBaseUri = "https://api"

  private val underTest = AddDocumentController(recallDocumentService, advertisedBaseUri)

  private val recallId = ::RecallId.random()
  private val fileName = "fileName"

  @Test
  fun `adds a recall document successfully`() {
    val document = "a document"
    val documentBytes = document.toByteArray()
    val category = PART_A_RECALL_REPORT
    val documentId = UUID.randomUUID()

    every {
      recallDocumentService.scanUploadAndAddDocumentForRecall(
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
  fun `'add document' responds with BAD_REQUEST if target recall is not found`() {
    val document = "a document"
    val documentBytes = document.toByteArray()
    val category = PART_A_RECALL_REPORT
    val recallNotFoundError = RecallNotFoundException(recallId)

    every { recallDocumentService.scanUploadAndAddDocumentForRecall(recallId, documentBytes, category, fileName) } throws recallNotFoundError

    val request = AddDocumentRequest(category, documentBytes.encodeToBase64String(), fileName)

    val exception = assertThrows<ResponseStatusException> { underTest.addDocument(recallId, request) }

    assertThat(exception.status, equalTo(BAD_REQUEST))
    assertThat(exception.reason, equalTo(recallNotFoundError.message))
    assertThat(exception.cause, equalTo(recallNotFoundError))
  }
}
