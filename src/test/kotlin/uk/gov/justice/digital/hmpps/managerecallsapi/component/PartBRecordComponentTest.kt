package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isEmpty
import com.natpryce.hamkrest.present
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.managerecallsapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.BookRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.PartBRecordController.PartBRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.encodeToBase64String
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CroNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PartBRecordId
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomFileName
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomHistoricalDate
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomNoms
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomString
import java.time.LocalDate

class PartBRecordComponentTest : ComponentTestBase() {
  private val nomsNumber = randomNoms()
  private val bookRecallRequest = BookRecallRequest(
    nomsNumber,
    FirstName("Barrie"),
    null,
    LastName("Badger"),
    CroNumber("1234/56A"),
    LocalDate.now()
  )
  private val details = "PartBRecord details"
  private val emailReceivedDate = randomHistoricalDate()
  private val partBContent = randomString().toByteArray().encodeToBase64String()
  private val partBFileName = randomFileName()
  private val emailContent = randomString().toByteArray().encodeToBase64String()
  private val emailFileName = randomFileName()
  private val oasysContent = randomString().toByteArray().encodeToBase64String()
  private val oasysFileName = randomFileName()

  @Test
  fun `create the first PartBRecord for a recall and verify on get recall`() {
    expectNoVirusesWillBeFound()
    val recall = authenticatedClient.bookRecall(bookRecallRequest)

    assertThat(recall.partBRecords, isEmpty)

    val partBRecordId = authenticatedClient.partBRecord(
      recall.recallId,
      partBRequest(),
      HttpStatus.CREATED,
      PartBRecordId::class.java
    )
    val recallWithPartBRecord = authenticatedClient.getRecall(recall.recallId)

    assertThat(recallWithPartBRecord.partBRecords.size, equalTo(1))
    assertThat(recallWithPartBRecord.partBRecords.first().partBRecordId, equalTo(partBRecordId))
    assertThat(recallWithPartBRecord.partBRecords.first().details, equalTo(details))
    assertThat(recallWithPartBRecord.partBRecords.first().partBReceivedDate, equalTo(emailReceivedDate))
    assertThat(recallWithPartBRecord.partBRecords.first().partBDocumentId, present())
    assertThat(recallWithPartBRecord.partBRecords.first().partBFileName, equalTo(partBFileName))
    assertThat(recallWithPartBRecord.partBRecords.first().emailId, present())
    assertThat(recallWithPartBRecord.partBRecords.first().emailFileName, equalTo(emailFileName))
    assertThat(recallWithPartBRecord.partBRecords.first().oasysDocumentId, present())
    assertThat(recallWithPartBRecord.partBRecords.first().oasysFileName, equalTo(oasysFileName))
    assertThat(recallWithPartBRecord.partBRecords.first().version, equalTo(1))
  }

  // TODO PUD-1605 Multiple error handling for virus found

  @Test
  fun `add a document with a virus returns bad request with body`() {
    expectAVirusWillBeFound()

    val recall = authenticatedClient.bookRecall(bookRecallRequest)

    val response = authenticatedClient.partBRecord(
      recall.recallId,
      partBRequest(),
      HttpStatus.BAD_REQUEST,
      ErrorResponse::class.java
    )

    assertThat(response, equalTo(ErrorResponse(HttpStatus.BAD_REQUEST, "VirusFoundException")))
  }

  private fun partBRequest() = PartBRequest(
    details,
    emailReceivedDate,
    partBFileName,
    partBContent,
    emailFileName,
    emailContent,
    oasysFileName,
    oasysContent
  )
}
