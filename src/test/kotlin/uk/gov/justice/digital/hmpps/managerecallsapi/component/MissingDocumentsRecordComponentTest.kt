package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isEmpty
import com.natpryce.hamkrest.present
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CREATED
import uk.gov.justice.digital.hmpps.managerecallsapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Api
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.BookRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MissingDocumentsRecordRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.PART_A_RECALL_REPORT
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.encodeToBase64String
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber

class MissingDocumentsRecordComponentTest : ComponentTestBase() {
  private val nomsNumber = NomsNumber("123456")
  private val bookRecallRequest = BookRecallRequest(nomsNumber, FirstName("Barrie"), null, LastName("Badger"))
  private val documentContents = "Expected Generated PDF".toByteArray()
  private val base64EncodedDocumentContents = documentContents.encodeToBase64String()
  private val fileName = "fileName"

  @Test
  fun `create a MissingDocumentsRecord`() {
    expectNoVirusesWillBeFound()
    val recall = authenticatedClient.bookRecall(bookRecallRequest)
    val missingDocsRecordReq = MissingDocumentsRecordRequest(recall.recallId, listOf(PART_A_RECALL_REPORT), "Some detail", base64EncodedDocumentContents, fileName)

    val response = authenticatedClient.missingDocumentsRecord(missingDocsRecordReq, CREATED, Api.MissingDocumentsRecord::class.java)

    assertThat(response.missingDocumentsRecordId, present())

    val recallWithMissingDocumentsRecord = authenticatedClient.getRecall(recall.recallId)
    assertThat(recall.missingDocumentsRecords, isEmpty)
    assertThat(recallWithMissingDocumentsRecord.missingDocumentsRecords.size, equalTo(1))
    assertThat(recallWithMissingDocumentsRecord.missingDocumentsRecords.first().version, equalTo(1))
  }

  @Test
  fun `adding 2 MissingDocumentsRecords for the same recall, only the latest version will be returned on the recall`() {
    expectNoVirusesWillBeFound()
    val recall = authenticatedClient.bookRecall(bookRecallRequest)
    val missingDocsRecordReq = MissingDocumentsRecordRequest(recall.recallId, listOf(PART_A_RECALL_REPORT), "Some detail", base64EncodedDocumentContents, fileName)

    authenticatedClient.missingDocumentsRecord(missingDocsRecordReq, CREATED, Api.MissingDocumentsRecord::class.java)
    val response = authenticatedClient.missingDocumentsRecord(missingDocsRecordReq.copy(detail = "Some details; some more detail"), CREATED, Api.MissingDocumentsRecord::class.java)

    assertThat(response.missingDocumentsRecordId, present())

    val recallWithMissingDocumentsRecord = authenticatedClient.getRecall(recall.recallId)
    assertThat(recall.missingDocumentsRecords, isEmpty)
    assertThat(recallWithMissingDocumentsRecord.missingDocumentsRecords.size, equalTo(1))
    assertThat(recallWithMissingDocumentsRecord.missingDocumentsRecords.first().version, equalTo(2))
  }

  @Test
  fun `add a MissingDocumentsRecord with a virus email returns bad request with body`() {
    expectAVirusWillBeFound()

    val recall = authenticatedClient.bookRecall(bookRecallRequest)
    val missingDocsRecordReq = MissingDocumentsRecordRequest(recall.recallId, listOf(PART_A_RECALL_REPORT), "Some detail", base64EncodedDocumentContents, fileName)

    val response = authenticatedClient.missingDocumentsRecord(missingDocsRecordReq, BAD_REQUEST, ErrorResponse::class.java)

    assertThat(response, equalTo(ErrorResponse(BAD_REQUEST, "VirusFoundException")))
  }
}
