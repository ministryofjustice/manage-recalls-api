package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.NOT_FOUND
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.AddDocumentRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.BookRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.GetDocumentResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.LETTER_TO_PRISON
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import java.util.Base64
import java.util.UUID

class LetterToPrisonDocumentComponentTest : ComponentTestBase() {

  private val nomsNumber = NomsNumber("123456")
  private val documentCategory = LETTER_TO_PRISON
  private val documentContents = "Expected Generated PDF".toByteArray()
  private val base64EncodedDocumentContents = Base64.getEncoder().encodeToString(documentContents)
  private val fileName = "emailfileName"
  private val addDocumentRequest = AddDocumentRequest(documentCategory, base64EncodedDocumentContents, fileName)

  @Test
  fun `add a letter document uploads the file to S3 and returns the documentId`() {
    val recallId = ::RecallId.random()
    recallRepository.save(Recall(recallId, nomsNumber))
    //
    // val response = authenticatedClient.uploadRecallDocument(recallId, addDocumentRequest)
    //
    // assertThat(response.documentId, present())
  }

  @Test
  fun `upload a recall document with a category that already exists overwrites the existing document`() {
    val recallId = ::RecallId.random()
    recallRepository.save(Recall(recallId, nomsNumber))

    val originalDocumentId = authenticatedClient.uploadRecallDocument(recallId, addDocumentRequest).documentId
    val newFilename = "newFilename"
    val addDocumentRequest = AddDocumentRequest(documentCategory, base64EncodedDocumentContents, newFilename)
    authenticatedClient.uploadRecallDocument(recallId, addDocumentRequest)

    val recallDocument = authenticatedClient.getRecallDocument(recallId, originalDocumentId)
    assertThat(recallDocument, equalTo(GetDocumentResponse(originalDocumentId, documentCategory, base64EncodedDocumentContents, newFilename)))
  }

  @Test
  fun `get a letter to prison document downloads the file from S3`() {
    val recall = authenticatedClient.bookRecall(BookRecallRequest(nomsNumber))
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
  fun `get recall document returns 404 if recall exist but document does not exist`() {
    val recallId = ::RecallId.random()
    recallRepository.save(Recall(recallId, nomsNumber))

    authenticatedClient.getRecallDocument(recallId, UUID.randomUUID(), expectedStatus = NOT_FOUND)
  }
}
