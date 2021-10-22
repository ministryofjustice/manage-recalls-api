package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.NOT_FOUND
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.AddDocumentRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.BookRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.GetDocumentResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.OTHER
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.PART_A_RECALL_REPORT
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.encodeToBase64String
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import java.util.UUID

class DocumentComponentTest : ComponentTestBase() {
  private val nomsNumber = NomsNumber("123456")
  private val bookRecallRequest = BookRecallRequest(nomsNumber)
  private val documentCategory = PART_A_RECALL_REPORT
  private val documentContents = "Expected Generated PDF".toByteArray()
  private val base64EncodedDocumentContents = documentContents.encodeToBase64String()
  private val fileName = "emailfileName"
  private val addDocumentRequest = AddDocumentRequest(documentCategory, base64EncodedDocumentContents, fileName)

  @Test
  fun `add a document uploads the file to S3 and returns the documentId`() {
    expectNoVirusesWillBeFound()

    val recall = authenticatedClient.bookRecall(bookRecallRequest)

    val response = authenticatedClient.uploadRecallDocument(recall.recallId, addDocumentRequest)

    assertThat(response.documentId, present())
  }

  @Test
  fun `add a document with a virus returns bad request`() {
    expectAVirusWillBeFound()

    val recall = authenticatedClient.bookRecall(bookRecallRequest)

    authenticatedClient.uploadRecallDocument(recall.recallId, addDocumentRequest, BAD_REQUEST)
  }

  @Test
  fun `upload a document with a 'versioned' category that already exists overwrites the existing document`() {
    expectNoVirusesWillBeFound()

    val recall = authenticatedClient.bookRecall(bookRecallRequest)

    val originalDocumentId = authenticatedClient.uploadRecallDocument(recall.recallId, addDocumentRequest).documentId
    val newFilename = "newFilename"
    val addDocumentRequest = AddDocumentRequest(documentCategory, base64EncodedDocumentContents, newFilename)
    authenticatedClient.uploadRecallDocument(recall.recallId, addDocumentRequest)

    val recallDocument = authenticatedClient.getRecallDocument(recall.recallId, originalDocumentId)
    assertThat(recallDocument, equalTo(GetDocumentResponse(originalDocumentId, documentCategory, base64EncodedDocumentContents, newFilename)))
  }

  @Test
  fun `upload two documents with a 'versioned' category that already exists overwrites the existing document`() {
    expectNoVirusesWillBeFound()

    val recall = authenticatedClient.bookRecall(bookRecallRequest)

    val otherDocumentRequest = AddDocumentRequest(OTHER, base64EncodedDocumentContents, fileName)

    val firstDocumentId = authenticatedClient.uploadRecallDocument(recall.recallId, otherDocumentRequest).documentId
    val secondDocumentId = authenticatedClient.uploadRecallDocument(recall.recallId, otherDocumentRequest).documentId

    val recallResponse = authenticatedClient.getRecall(recall.recallId)

    assertThat(firstDocumentId, !equalTo(secondDocumentId))
    assertThat(recallResponse.documents.size, equalTo(2))
  }

  @Test
  fun `add a document returns 404 if recall does not exist`() {
    authenticatedClient.uploadRecallDocument(::RecallId.random(), addDocumentRequest, expectedStatus = NOT_FOUND)
  }

  @Test
  fun `can download an uploaded document`() {
    expectNoVirusesWillBeFound()

    val recall = authenticatedClient.bookRecall(bookRecallRequest)
    val document = authenticatedClient.uploadRecallDocument(
      recall.recallId,
      AddDocumentRequest(documentCategory, base64EncodedDocumentContents, fileName)
    )

    val response = authenticatedClient.getRecallDocument(recall.recallId, document.documentId)

    assertThat(
      response,
      equalTo(
        GetDocumentResponse(
          document.documentId,
          documentCategory,
          base64EncodedDocumentContents,
          fileName
        )
      )
    )
  }

  @Test
  fun `get document returns 404 if the recall exists but the document does not`() {
    expectNoVirusesWillBeFound()

    val recall = authenticatedClient.bookRecall(bookRecallRequest)

    authenticatedClient.getRecallDocument(recall.recallId, UUID.randomUUID(), expectedStatus = NOT_FOUND)
  }
}
