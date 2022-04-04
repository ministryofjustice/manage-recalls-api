package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isEmpty
import com.natpryce.hamkrest.present
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.OK
import uk.gov.justice.digital.hmpps.managerecallsapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.BookRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RescindRecordController
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RescindRecordController.RescindRequestRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Status
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.StopReason
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.encodeToBase64String
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CroNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FileName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FullName
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
  private val requestFileName = FileName("requestFileName")
  private val approvalFileName = FileName("approvalFileName")

  @Test
  fun `create and approve the first RescindRecord for a recall`() {
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
    val decisionRequest = RescindRecordController.RescindDecisionRequest(
      true,
      approvalDetails,
      LocalDate.now(),
      base64EncodedDocumentContents,
      approvalFileName
    )

    assertThat(recall.rescindRecords, isEmpty)

    val rescindRecordId = requestRescindSuccess(recall, createRequest)
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
    assertThat(recallWithRequestedRescindRecord.stopReason, absent())
    assertThat(recallWithRequestedRescindRecord.stopByUserName, absent())
    assertThat(recallWithRequestedRescindRecord.stopDateTime, absent())

    decideRescindSuccess(recall, rescindRecordId, decisionRequest)
    val recallWithApprovedRescindRecord = authenticatedClient.getRecall(recall.recallId)

    assertThat(recallWithApprovedRescindRecord.status, equalTo(Status.STOPPED))
    assertThat(recallWithApprovedRescindRecord.stopReason, equalTo(StopReason.RESCINDED))
    assertThat(recallWithApprovedRescindRecord.stopByUserName, equalTo(FullName("Bertie Badger")))
    assertThat(recallWithApprovedRescindRecord.stopDateTime, present())
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
  fun `create and reject a RescindRecord for a recall`() {
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
    val decisionRequest = RescindRecordController.RescindDecisionRequest(
      false,
      approvalDetails,
      LocalDate.now(),
      base64EncodedDocumentContents,
      approvalFileName
    )

    assertThat(recall.rescindRecords, isEmpty)

    val rescindRecordId = requestRescindSuccess(recall, createRequest)
    val recallWithRequestedRescindRecord = authenticatedClient.getRecall(recall.recallId)

    assertThat(recallWithRequestedRescindRecord.rescindRecords.size, equalTo(1))
    assertThat(recallWithRequestedRescindRecord.status, equalTo(Status.BEING_BOOKED_ON))
    assertThat(recallWithRequestedRescindRecord.stopReason, absent())
    assertThat(recallWithRequestedRescindRecord.stopByUserName, absent())
    assertThat(recallWithRequestedRescindRecord.stopDateTime, absent())

    decideRescindSuccess(recall, rescindRecordId, decisionRequest)
    val recallWithApprovedRescindRecord = authenticatedClient.getRecall(recall.recallId)

    assertThat(recallWithApprovedRescindRecord.status, equalTo(Status.BEING_BOOKED_ON))
    assertThat(recallWithApprovedRescindRecord.stopReason, absent())
    assertThat(recallWithApprovedRescindRecord.stopByUserName, absent())
    assertThat(recallWithApprovedRescindRecord.stopDateTime, absent())
    assertThat(recallWithApprovedRescindRecord.rescindRecords.size, equalTo(1))
    assertThat(recallWithApprovedRescindRecord.rescindRecords.first().approved, equalTo(false))
  }

  @Test
  fun `deciding a record that doesnt exist returns a 404`() {
    expectNoVirusesWillBeFound()
    val recall = authenticatedClient.bookRecall(bookRecallRequest)

    val decisionRequest = RescindRecordController.RescindDecisionRequest(
      true,
      "Some details",
      LocalDate.now(),
      base64EncodedDocumentContents,
      approvalFileName
    )

    val rescindRecordId = ::RescindRecordId.random()
    val result = decideRescindFailure(recall, rescindRecordId, decisionRequest, NOT_FOUND)

    assertThat(result, equalTo(ErrorResponse(NOT_FOUND, "RescindRecordNotFoundException(recallId=${recall.recallId}, rescindRecordId=$rescindRecordId)")))
  }

  @Test
  fun `add a document with a virus returns bad request with body`() {
    expectAVirusWillBeFound()

    val recall = authenticatedClient.bookRecall(bookRecallRequest)

    val requestDetails = "Some request detail"
    val createRequest = RescindRequestRequest(
      requestDetails,
      LocalDate.now().minusDays(3),
      base64EncodedDocumentContents,
      requestFileName
    )

    val result = requestRescindFailure(recall, createRequest, HttpStatus.BAD_REQUEST)

    assertThat(result, equalTo(ErrorResponse(HttpStatus.BAD_REQUEST, "VirusFoundException")))
  }

  @Test
  fun `403 error thrown when requesting a new rescind if one is already in progress`() {
    expectNoVirusesWillBeFound()

    val recall = authenticatedClient.bookRecall(bookRecallRequest)

    val createRequest = RescindRequestRequest(
      "blah blah",
      LocalDate.now().minusDays(3),
      base64EncodedDocumentContents,
      requestFileName
    )
    requestRescindSuccess(recall, createRequest)
    requestRescindFailure(recall, createRequest, FORBIDDEN)
  }

  @Test
  fun `can not update a record which has already been decided`() {
    expectNoVirusesWillBeFound()
    val recall = authenticatedClient.bookRecall(bookRecallRequest)

    val createRequest = RescindRequestRequest(
      "blah blah",
      LocalDate.now().minusDays(3),
      base64EncodedDocumentContents,
      requestFileName
    )

    val decisionRequest = RescindRecordController.RescindDecisionRequest(
      true,
      "More blah blah",
      LocalDate.now(),
      base64EncodedDocumentContents,
      approvalFileName
    )

    val rescindRecordId = requestRescindSuccess(recall, createRequest)

    decideRescindSuccess(recall, rescindRecordId, decisionRequest)
    decideRescindFailure(recall, rescindRecordId, decisionRequest, FORBIDDEN)
  }

  private fun requestRescindSuccess(
    recall: RecallResponse,
    createRequest: RescindRequestRequest
  ) = authenticatedClient.requestRescind(recall.recallId, createRequest, CREATED, RescindRecordId::class.java)

  private fun requestRescindFailure(
    recall: RecallResponse,
    createRequest: RescindRequestRequest,
    expectedStatus: HttpStatus
  ) = authenticatedClient.requestRescind(recall.recallId, createRequest, expectedStatus, ErrorResponse::class.java)

  private fun decideRescindSuccess(
    recall: RecallResponse,
    rescindRecordId: RescindRecordId,
    decisionRequest: RescindRecordController.RescindDecisionRequest
  ) = authenticatedClient.decideRescind(recall.recallId, rescindRecordId, decisionRequest, OK, RescindRecordId::class.java)

  private fun decideRescindFailure(
    recall: RecallResponse,
    rescindRecordId: RescindRecordId,
    decisionRequest: RescindRecordController.RescindDecisionRequest,
    status: HttpStatus
  ) = authenticatedClient.decideRescind(recall.recallId, rescindRecordId, decisionRequest, status, ErrorResponse::class.java)
}
