package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isEmpty
import com.natpryce.hamkrest.present
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.BAD_REQUEST
import uk.gov.justice.digital.hmpps.managerecallsapi.config.FileError
import uk.gov.justice.digital.hmpps.managerecallsapi.config.MultiErrorResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.BookRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.PartBRecordController.PartBRecordRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.OASYS_RISK_ASSESSMENT
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.PART_B_EMAIL_FROM_PROBATION
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.PART_B_RISK_REPORT
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.encodeToBase64String
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CroNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
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
      PartBRecordRequest()
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

  @Test
  fun `add documents which all fail virus scan returns bad request with fileErrors list of objects wrapping category, filename and error text as body`() {
    expectAVirusWillBeFound()

    val recall = authenticatedClient.bookRecall(bookRecallRequest)

    val response = authenticatedClient.partBRecord(
      recall.recallId,
      PartBRecordRequest(),
      BAD_REQUEST
    ).expectBody(MultiErrorResponse::class.java).returnResult().responseBody!!

    assertThat(
      response,
      equalTo(
        MultiErrorResponse(
          BAD_REQUEST,
          listOf(
            FileError(PART_B_RISK_REPORT, partBFileName, "VirusFoundException"),
            FileError(PART_B_EMAIL_FROM_PROBATION, emailFileName, "VirusFoundException"),
            FileError(OASYS_RISK_ASSESSMENT, oasysFileName, "VirusFoundException")
          )
        )
      )
    )
  }

  @Test
  fun `add documents which all fail virus scan returns bad request with json array fileErrors of objects of category, filename and error text as body`() {
    expectAVirusWillBeFound()

    val recall = authenticatedClient.bookRecall(bookRecallRequest)

    authenticatedClient.partBRecord(
      recall.recallId,
      PartBRecordRequest(),
      BAD_REQUEST
    ).expectBody()
      .jsonPath("$.fileErrors.length()").isEqualTo(3)
      .jsonPath("$.fileErrors[0].category").isEqualTo(PART_B_RISK_REPORT.toString())
      .jsonPath("$.fileErrors[0].fileName").isEqualTo(partBFileName.value)
      .jsonPath("$.fileErrors[0].error").isEqualTo("VirusFoundException")
      .jsonPath("$.fileErrors[1].category").isEqualTo(PART_B_EMAIL_FROM_PROBATION.toString())
      .jsonPath("$.fileErrors[2].category").isEqualTo(OASYS_RISK_ASSESSMENT.toString())
  }

  private fun PartBRecordRequest() = PartBRecordRequest(
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
