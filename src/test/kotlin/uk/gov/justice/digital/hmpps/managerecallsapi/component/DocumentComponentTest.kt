package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.NOT_FOUND
import uk.gov.justice.digital.hmpps.managerecallsapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.AddDocumentRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ApiRecallDocument
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.BookRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.GetDocumentResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UpdateDocumentRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UpdateDocumentResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.OTHER
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.PART_A_RECALL_REPORT
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.RECALL_NOTIFICATION
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.UNCATEGORISED
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.encodeToBase64String
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random

class DocumentComponentTest : ComponentTestBase() {
  private val nomsNumber = NomsNumber("123456")
  private val bookRecallRequest = BookRecallRequest(nomsNumber, ::UserId.random())
  private val versionedDocumentCategory = PART_A_RECALL_REPORT
  private val documentContents = "Expected Generated PDF".toByteArray()
  private val base64EncodedDocumentContents = documentContents.encodeToBase64String()
  private val fileName = "fileName"
  private val addVersionedDocumentRequest = AddDocumentRequest(versionedDocumentCategory, base64EncodedDocumentContents, fileName)

  @Test
  fun `add a document uploads the file to S3 and returns the documentId`() {
    expectNoVirusesWillBeFound()

    val recall = authenticatedClient.bookRecall(bookRecallRequest)

    val response = authenticatedClient.uploadDocument(recall.recallId, addVersionedDocumentRequest)

    assertThat(response.documentId, present())
  }

  @Test
  fun `add a document with a virus returns bad request with body`() {
    expectAVirusWillBeFound()

    val recall = authenticatedClient.bookRecall(bookRecallRequest)

    val result = authenticatedClient.uploadDocument(recall.recallId, addVersionedDocumentRequest, BAD_REQUEST)
      .expectBody(ErrorResponse::class.java).returnResult().responseBody!!

    assertThat(result, equalTo(ErrorResponse(BAD_REQUEST, "VirusFoundException")))
  }

  @Test
  fun `upload multiple documents with a 'versioned' category that already exists writes a new document and increments version`() {
    expectNoVirusesWillBeFound()

    val recall = authenticatedClient.bookRecall(bookRecallRequest)

    val firstDocumentId = authenticatedClient.uploadDocument(recall.recallId, addVersionedDocumentRequest).documentId

    val secondDocumentId = authenticatedClient.uploadDocument(recall.recallId, addVersionedDocumentRequest).documentId
    val secondDocument = authenticatedClient.getRecallDocument(recall.recallId, secondDocumentId)

    assertThat(firstDocumentId, !equalTo(secondDocumentId))
    assertThat(
      secondDocument,
      equalTo(GetDocumentResponse(secondDocumentId, versionedDocumentCategory, base64EncodedDocumentContents, fileName, 2, secondDocument.createdDateTime))
    )

    val thirdDocumentId = authenticatedClient.uploadDocument(recall.recallId, addVersionedDocumentRequest).documentId
    val recallDocument = authenticatedClient.getRecallDocument(recall.recallId, thirdDocumentId)

    assertThat(firstDocumentId, !equalTo(secondDocumentId))
    assertThat(firstDocumentId, !equalTo(thirdDocumentId))
    assertThat(
      recallDocument,
      equalTo(GetDocumentResponse(thirdDocumentId, versionedDocumentCategory, base64EncodedDocumentContents, fileName, 3, recallDocument.createdDateTime))
    )

    val recallResponse = authenticatedClient.getRecall(recall.recallId)
    assertThat(recallResponse.documents.size, equalTo(1))
  }

  @Test
  fun `upload two documents with an 'unversioned' category allows both to be persisted and returned on a recall`() {
    expectNoVirusesWillBeFound()

    val recall = authenticatedClient.bookRecall(bookRecallRequest)

    val otherDocumentRequest = AddDocumentRequest(OTHER, base64EncodedDocumentContents, fileName)

    val firstDocumentId = authenticatedClient.uploadDocument(recall.recallId, otherDocumentRequest).documentId
    val secondDocumentId = authenticatedClient.uploadDocument(recall.recallId, otherDocumentRequest).documentId

    val recallResponse = authenticatedClient.getRecall(recall.recallId)

    assertThat(firstDocumentId, !equalTo(secondDocumentId))
    assertThat(recallResponse.documents.size, equalTo(2))
  }

  @Test
  fun `add a document returns 404 if recall does not exist`() {
    authenticatedClient.uploadDocument(::RecallId.random(), addVersionedDocumentRequest, expectedStatus = NOT_FOUND)
  }

  @Test
  fun `can download an uploaded document`() {
    expectNoVirusesWillBeFound()

    val recall = authenticatedClient.bookRecall(bookRecallRequest)
    val document = authenticatedClient.uploadDocument(
      recall.recallId,
      AddDocumentRequest(versionedDocumentCategory, base64EncodedDocumentContents, fileName)
    )

    val response = authenticatedClient.getRecallDocument(recall.recallId, document.documentId)

    assertThat(
      response,
      equalTo(
        GetDocumentResponse(
          document.documentId,
          versionedDocumentCategory,
          base64EncodedDocumentContents,
          fileName,
          1,
          response.createdDateTime
        )
      )
    )
  }

  @Test
  fun `get document returns 404 if the recall exists but the document does not`() {
    expectNoVirusesWillBeFound()

    val recall = authenticatedClient.bookRecall(bookRecallRequest)

    authenticatedClient.getRecallDocument(recall.recallId, ::DocumentId.random(), expectedStatus = NOT_FOUND)
  }

  @Test
  fun `can update category for an unversioned document to versioned`() {
    expectNoVirusesWillBeFound()
    val updatedCategory = PART_A_RECALL_REPORT

    val recall = authenticatedClient.bookRecall(bookRecallRequest)
    val documentId = authenticatedClient.uploadDocument(
      recall.recallId,
      AddDocumentRequest(UNCATEGORISED, base64EncodedDocumentContents, fileName)
    ).documentId

    val document = documentRepository.getByRecallIdAndDocumentId(recall.recallId, documentId)
    document.let {
      assertThat(it.id(), equalTo(documentId))
      assertThat(it.recallId(), equalTo(recall.recallId))
      assertThat(it.category, equalTo(UNCATEGORISED))
      assertThat(it.version, equalTo(null))
      assertThat(it.fileName, equalTo(fileName))
    }

    val result = authenticatedClient.updateDocumentCategory(
      recall.recallId,
      documentId,
      UpdateDocumentRequest(updatedCategory)
    )

    assertThat(result, equalTo(UpdateDocumentResponse(documentId, recall.recallId, updatedCategory, fileName)))

    val response = authenticatedClient.getRecallDocument(recall.recallId, documentId)

    assertThat(
      response,
      equalTo(
        GetDocumentResponse(documentId, updatedCategory, base64EncodedDocumentContents, fileName, 1, document.createdDateTime)
      )
    )
  }

  @Test
  fun `can update category for a versioned document to unversioned`() {
    expectNoVirusesWillBeFound()
    val updatedCategory = UNCATEGORISED

    val recall = authenticatedClient.bookRecall(bookRecallRequest)
    val documentId = authenticatedClient.uploadDocument(
      recall.recallId,
      AddDocumentRequest(PART_A_RECALL_REPORT, base64EncodedDocumentContents, fileName)
    ).documentId

    val document = documentRepository.getByRecallIdAndDocumentId(recall.recallId, documentId)
    document.let {
      assertThat(it.id(), equalTo(documentId))
      assertThat(it.recallId(), equalTo(recall.recallId))
      assertThat(it.category, equalTo(PART_A_RECALL_REPORT))
      assertThat(it.version, equalTo(1))
      assertThat(it.fileName, equalTo(fileName))
    }

    val result = authenticatedClient.updateDocumentCategory(
      recall.recallId,
      documentId,
      UpdateDocumentRequest(updatedCategory)
    )

    assertThat(result, equalTo(UpdateDocumentResponse(documentId, recall.recallId, updatedCategory, fileName)))

    val response = authenticatedClient.getRecallDocument(recall.recallId, documentId)

    assertThat(
      response,
      equalTo(
        GetDocumentResponse(documentId, updatedCategory, base64EncodedDocumentContents, fileName, null, document.createdDateTime)
      )
    )
  }

  @Test
  fun `can delete uploaded document for Recall being booked`() {
    expectNoVirusesWillBeFound()

    val recall = authenticatedClient.bookRecall(bookRecallRequest)
    val document = authenticatedClient.uploadDocument(
      recall.recallId,
      AddDocumentRequest(PART_A_RECALL_REPORT, base64EncodedDocumentContents, fileName)
    )

    authenticatedClient.deleteDocument(recall.recallId, document.documentId)
  }

  @Test
  fun `can't delete generated document for Recall being booked`() {
    expectNoVirusesWillBeFound()

    val recall = authenticatedClient.bookRecall(bookRecallRequest)
    val document = authenticatedClient.uploadDocument(
      recall.recallId,
      AddDocumentRequest(RECALL_NOTIFICATION, base64EncodedDocumentContents, fileName) // This wouldn't be uploaded, but works for now.
    )

    val response = authenticatedClient.deleteDocument(recall.recallId, document.documentId, BAD_REQUEST)
      .expectBody(ErrorResponse::class.java)
      .returnResult()
      .responseBody!!

    assertThat(response, equalTo(ErrorResponse(BAD_REQUEST, "DocumentDeleteException: Unable to delete document: Wrong status [BEING_BOOKED_ON] and/or document category [RECALL_NOTIFICATION]")))
  }

  @Test
  fun `after deleting a document with a 'versioned' category the previous version is returned`() {
    expectNoVirusesWillBeFound()

    val recall = authenticatedClient.bookRecall(bookRecallRequest)

    val firstDocumentId = authenticatedClient.uploadDocument(recall.recallId, addVersionedDocumentRequest).documentId
    val secondDocumentId = authenticatedClient.uploadDocument(recall.recallId, addVersionedDocumentRequest).documentId
    val secondDocument = authenticatedClient.getRecallDocument(recall.recallId, secondDocumentId)

    assertThat(firstDocumentId, !equalTo(secondDocumentId))
    assertThat(
      secondDocument,
      equalTo(GetDocumentResponse(secondDocumentId, versionedDocumentCategory, base64EncodedDocumentContents, fileName, 2, secondDocument.createdDateTime))
    )

    authenticatedClient.deleteDocument(recall.recallId, secondDocumentId)
    val documents = authenticatedClient.getRecall(recall.recallId).documents

    assertThat(firstDocumentId, !equalTo(secondDocumentId))
    assertThat(documents.size, equalTo(1))
    assertThat(
      documents[0],
      equalTo(ApiRecallDocument(firstDocumentId, versionedDocumentCategory, fileName, 1, documents[0].createdDateTime))
    )
  }
}
