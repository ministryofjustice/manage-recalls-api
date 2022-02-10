package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isEmpty
import com.natpryce.hamkrest.present
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.BookRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RescindRecordController
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RescindRecordController.RescindRequestRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.encodeToBase64String
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CroNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RescindRecordId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import java.time.LocalDate

class RescindRecordComponentTest : ComponentTestBase() {
  private val nomsNumber = NomsNumber("123456")
  private val bookRecallRequest = BookRecallRequest(
    nomsNumber,
    FirstName("Barrie"),
    null,
    LastName("Badger"),
    CroNumber("1234/56A"),
    LocalDate.now()
  )
  private val documentContents = "Expected Generated PDF".toByteArray()
  private val base64EncodedDocumentContents = documentContents.encodeToBase64String()
  private val requestFileName = "requestFileName"
  private val approvalFileName = "approvalFileName"

  @Test
  fun `create and complete the first RescindRecord for a recall`() {
    expectNoVirusesWillBeFound()
    val recall = authenticatedClient.bookRecall(bookRecallRequest)
    val requestDetails = "Some request detail"
    val approvalDetails = "Some approval detail"
    val createRequest = RescindRequestRequest(
      requestDetails,
      LocalDate.now().minusDays(3),
      base64EncodedDocumentContents,
      requestFileName
    )
    val putRequest = RescindRecordController.RescindDecisionRequest(
      true,
      approvalDetails,
      LocalDate.now(),
      base64EncodedDocumentContents,
      approvalFileName
    )

    assertThat(recall.rescindRecords, isEmpty)

    val rescindRecordId = authenticatedClient.requestRescind(recall.recallId, createRequest)
    val recallWithRequestedRescindRecord = authenticatedClient.getRecall(recall.recallId)

    assertThat(recallWithRequestedRescindRecord.rescindRecords.size, equalTo(1))
    assertThat(recallWithRequestedRescindRecord.rescindRecords.first().rescindRecordId, equalTo(rescindRecordId))
    assertThat(recallWithRequestedRescindRecord.rescindRecords.first().version, equalTo(1))
    assertThat(recallWithRequestedRescindRecord.rescindRecords.first().requestDetails, equalTo(requestDetails))
    assertThat(recallWithRequestedRescindRecord.rescindRecords.first().requestEmailFileName, equalTo(requestFileName))
    assertThat(recallWithRequestedRescindRecord.rescindRecords.first().requestEmailId, present())
    assertThat(recallWithRequestedRescindRecord.rescindRecords.first().requestEmailReceivedDate, equalTo(LocalDate.now().minusDays(3)))
    assertThat(recallWithRequestedRescindRecord.rescindRecords.first().approved, absent())
    assertThat(recallWithRequestedRescindRecord.rescindRecords.first().decisionDetails, absent())
    assertThat(recallWithRequestedRescindRecord.rescindRecords.first().decisionEmailFileName, absent())
    assertThat(recallWithRequestedRescindRecord.rescindRecords.first().decisionEmailId, absent())
    assertThat(recallWithRequestedRescindRecord.rescindRecords.first().decisionEmailSentDate, absent())

    authenticatedClient.decideRescind(recall.recallId, rescindRecordId, putRequest)
    val recallWithApprovedRescindRecord = authenticatedClient.getRecall(recall.recallId)

    assertThat(recallWithApprovedRescindRecord.rescindRecords.size, equalTo(1))
    assertThat(recallWithApprovedRescindRecord.rescindRecords.first().rescindRecordId, equalTo(rescindRecordId))
    assertThat(recallWithApprovedRescindRecord.rescindRecords.first().version, equalTo(1))
    assertThat(recallWithApprovedRescindRecord.rescindRecords.first().requestDetails, equalTo(requestDetails))
    assertThat(recallWithApprovedRescindRecord.rescindRecords.first().requestEmailFileName, equalTo(requestFileName))
    assertThat(recallWithApprovedRescindRecord.rescindRecords.first().requestEmailId, present())
    assertThat(recallWithApprovedRescindRecord.rescindRecords.first().requestEmailReceivedDate, equalTo(LocalDate.now().minusDays(3)))
    assertThat(recallWithApprovedRescindRecord.rescindRecords.first().approved, equalTo(true))
    assertThat(recallWithApprovedRescindRecord.rescindRecords.first().decisionDetails, equalTo(approvalDetails))
    assertThat(recallWithApprovedRescindRecord.rescindRecords.first().decisionEmailFileName, equalTo(approvalFileName))
    assertThat(recallWithApprovedRescindRecord.rescindRecords.first().decisionEmailId, present())
    assertThat(recallWithApprovedRescindRecord.rescindRecords.first().decisionEmailSentDate, equalTo(LocalDate.now()))
  }

  @Test
  fun `deciding a record that doesnt exist returns a 404`() {
    expectNoVirusesWillBeFound()
    val recall = authenticatedClient.bookRecall(bookRecallRequest)

    val putRequest = RescindRecordController.RescindDecisionRequest(
      true,
      "Some details",
      LocalDate.now(),
      base64EncodedDocumentContents,
      approvalFileName
    )

    authenticatedClient.decideRescind(recall.recallId, ::RescindRecordId.random(), putRequest, HttpStatus.NOT_FOUND)
  }
}
