package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import io.mockk.every
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.AddDocumentRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.GetDocumentResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocument
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import java.util.Base64
import java.util.UUID

// TODO: MD  Use localstack instead of mocking S3Service
class RecallDocumentComponentTest : ComponentTestBase() {

  private val nomsNumber = NomsNumber("123456")
  private val documentCategory = RecallDocumentCategory.PART_A_RECALL_REPORT
  private val documentContents = "Expected Generated PDF".toByteArray()
  private val base64EncodedDocumentContents = Base64.getEncoder().encodeToString(documentContents)
  private val addDocumentRequest = AddDocumentRequest(
    category = documentCategory.name,
    fileContent = base64EncodedDocumentContents
  )

  @Test
  fun `add a recall document uploads the file to S3 and returns the documentId`() {
    val recallId = ::RecallId.random()
    recallRepository.save(Recall(recallId, nomsNumber))

    expectADocumentWillBeUploadedToS3()

    val response = uploadRecallDocument(recallId, addDocumentRequest)

    assertThat(response.documentId, present())
  }

  @Test
  fun `get a recall document downloads the file from S3`() {
    val recallId = ::RecallId.random()
    val documentId = UUID.randomUUID()
    recallRepository.save(
      Recall(recallId, nomsNumber, documents = setOf(RecallDocument(documentId, recallId.value, documentCategory)))
    )

    expectADocumentWillBeDownloadedFromS3(documentId, documentContents)

    val response = getRecallDocument(recallId, documentId)

    assertThat(response, equalTo(GetDocumentResponse(documentId, documentCategory, base64EncodedDocumentContents)))
  }

  private fun expectADocumentWillBeDownloadedFromS3(documentId: UUID, expectedDocument: ByteArray) {
    every { s3Service.downloadFile(documentId) } returns expectedDocument
  }

  private fun expectADocumentWillBeUploadedToS3() {
    every { s3Service.uploadFile(any()) } returns UUID.randomUUID()
  }
}