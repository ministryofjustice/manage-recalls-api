package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isEmpty
import com.natpryce.hamkrest.present
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CREATED
import uk.gov.justice.digital.hmpps.managerecallsapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.BookRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.TempMissingDocumentsRecordRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.PART_A_RECALL_REPORT
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.encodeToBase64String
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.MissingDocumentsRecordId
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
    val missingDocsRecordReq = TempMissingDocumentsRecordRequest(recall.recallId, listOf(PART_A_RECALL_REPORT), "Some detail", base64EncodedDocumentContents, fileName, null)

    val response = authenticatedClient.missingDocumentsRecord(missingDocsRecordReq, CREATED, MissingDocumentsRecordId::class.java)

    assertThat(response, present())

    val recallWithMissingDocumentsRecord = authenticatedClient.getRecall(recall.recallId)
    assertThat(recall.missingDocumentsRecords, isEmpty)
    assertThat(recallWithMissingDocumentsRecord.missingDocumentsRecords.size, equalTo(1))
    assertThat(recallWithMissingDocumentsRecord.missingDocumentsRecords.first().version, equalTo(1))
  }

  @Test
  fun `create a MissingDocumentsRecord with temp form of request`() { // TODO PUD-1250: remove use of temp request and class when UI has transitioned
    expectNoVirusesWillBeFound()
    val recall = authenticatedClient.bookRecall(bookRecallRequest)
    val missingDocsRecordReq = TempMissingDocumentsRecordRequest(recall.recallId, listOf(PART_A_RECALL_REPORT), details = null, base64EncodedDocumentContents, fileName, detail = "old detail prop")

    val response = authenticatedClient.missingDocumentsRecord(missingDocsRecordReq, CREATED, MissingDocumentsRecordId::class.java)

    assertThat(response, present())

    val recallWithMissingDocumentsRecord = authenticatedClient.getRecall(recall.recallId)
    assertThat(recall.missingDocumentsRecords, isEmpty)
    assertThat(recallWithMissingDocumentsRecord.missingDocumentsRecords.size, equalTo(1))
    assertThat(recallWithMissingDocumentsRecord.missingDocumentsRecords.first().version, equalTo(1))
  }

  @Test
  fun `adding 2 MissingDocumentsRecords for the same recall, all missing documents will be returned on the recall`() {
    expectNoVirusesWillBeFound()
    val recall = authenticatedClient.bookRecall(bookRecallRequest)
    val missingDocsRecordReq = TempMissingDocumentsRecordRequest(recall.recallId, listOf(PART_A_RECALL_REPORT), "Some detail", base64EncodedDocumentContents, fileName, null)

    authenticatedClient.missingDocumentsRecord(missingDocsRecordReq, CREATED, MissingDocumentsRecordId::class.java)
    val response = authenticatedClient.missingDocumentsRecord(missingDocsRecordReq.copy(details = "Some details; some more detail"), CREATED, MissingDocumentsRecordId::class.java)

    assertThat(response, present())

    val recallWithMissingDocumentsRecord = authenticatedClient.getRecall(recall.recallId)
    assertThat(recall.missingDocumentsRecords, isEmpty)
    assertThat(recallWithMissingDocumentsRecord.missingDocumentsRecords.size, equalTo(2))
    assertThat(recallWithMissingDocumentsRecord.missingDocumentsRecords[0].version, !equalTo(recallWithMissingDocumentsRecord.missingDocumentsRecords[1].version))
  }

  @Test
  fun `add a MissingDocumentsRecord with a virus email returns bad request with body`() {
    expectAVirusWillBeFound()

    val recall = authenticatedClient.bookRecall(bookRecallRequest)
    val missingDocsRecordReq = TempMissingDocumentsRecordRequest(recall.recallId, listOf(PART_A_RECALL_REPORT), "Some detail", base64EncodedDocumentContents, fileName, null)

    val response = authenticatedClient.missingDocumentsRecord(missingDocsRecordReq, BAD_REQUEST, ErrorResponse::class.java)

    assertThat(response, equalTo(ErrorResponse(BAD_REQUEST, "VirusFoundException")))
  }
}
