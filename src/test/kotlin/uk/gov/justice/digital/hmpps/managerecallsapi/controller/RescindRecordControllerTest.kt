package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import dev.forkhandles.result4k.Success
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RescindRecordController.RescindDecisionRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RescindRecordController.RescindRequestRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.extractor.TokenExtractor
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RescindRecord
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RescindRecordRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.encodeToBase64String
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RescindRecordId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.service.DocumentService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallNotFoundException
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RescindRecordNotFoundException
import java.time.LocalDate
import java.time.OffsetDateTime

class RescindRecordControllerTest {
  private val recallRepository = mockk<RecallRepository>()
  private val rescindRecordRepository = mockk<RescindRecordRepository>()
  private val documentService = mockk<DocumentService>()
  private val tokenExtractor = mockk<TokenExtractor>()

  private val bearerToken = "BEARER TOKEN"
  private val userId = ::UserId.random()

  private val underTest = RescindRecordController(
    recallRepository,
    documentService,
    rescindRecordRepository,
    tokenExtractor
  )

  private val recallId = ::RecallId.random()

  @Test
  fun `store first record as version 1`() {
    val recall = mockk<Recall>()
    val documentBytes = "a document".toByteArray()
    val fileName = "email.msg"
    val emailId = ::DocumentId.random()
    val savedRecordSlot = slot<RescindRecord>()
    val mockRecord = mockk<RescindRecord>()
    val mockRescindRecordId = mockk<RescindRecordId>()
    val requestDetails = "blah blah"

    every { recall.rescindRecords } returns emptySet()
    every { recallRepository.getByRecallId(recallId) } returns recall
    every {
      documentService.scanAndStoreDocument(
        recallId,
        userId,
        documentBytes,
        DocumentCategory.RESCIND_REQUEST_EMAIL,
        fileName
      )
    } returns Success(emailId)
    every { tokenExtractor.getTokenFromHeader(bearerToken) } returns TokenExtractor.Token(userId.toString())
    every { rescindRecordRepository.save(capture(savedRecordSlot)) } returns mockRecord
    every { mockRecord.id() } returns mockRescindRecordId
    every { mockRecord.version } returns 1

    val request = RescindRequestRequest(
      requestDetails, LocalDate.now(),
      documentBytes.encodeToBase64String(), fileName
    )

    val response = underTest.createRescindRecord(recallId, request, bearerToken)

    assertThat(savedRecordSlot.captured.version, equalTo(1))
    assertThat(savedRecordSlot.captured.createdDateTime, equalTo(savedRecordSlot.captured.lastUpdatedDateTime))
    assertThat(savedRecordSlot.captured.requestDetails, equalTo(requestDetails))
    assertThat(response.statusCode, equalTo(HttpStatus.CREATED))
    assertThat(
      response.body,
      equalTo(
        mockRescindRecordId
      )
    )
  }

  @Test
  fun `decide undecided rescind request`() {
    val recall = mockk<Recall>()
    val documentBytes = "a document".toByteArray()
    val fileName = "file.msg"
    val decisionDetails = "blah blah again"
    val rescindRecordId = ::RescindRecordId.random()
    val emailId = ::DocumentId.random()
    val rescindRecord = RescindRecord(rescindRecordId, recallId, 1, userId, OffsetDateTime.now(), emailId, "details", LocalDate.now())
    val savedRecordSlot = slot<RescindRecord>()

    every { recall.rescindRecords } returns setOf(rescindRecord)
    every { recallRepository.getByRecallId(recallId) } returns recall
    every { tokenExtractor.getTokenFromHeader(bearerToken) } returns TokenExtractor.Token(userId.toString())
    every {
      documentService.scanAndStoreDocument(
        recallId,
        userId,
        documentBytes,
        DocumentCategory.RESCIND_DECISION_EMAIL,
        fileName
      )
    } returns Success(emailId)
    every { rescindRecordRepository.save(capture(savedRecordSlot)) } returns rescindRecord

    val decisionRequest = RescindDecisionRequest(
      false,
      decisionDetails, LocalDate.now(),
      documentBytes.encodeToBase64String(), fileName
    )

    val response =
      underTest.decideRescindRecord(recallId, rescindRecordId, decisionRequest, bearerToken)

    assertThat(savedRecordSlot.captured.approved, equalTo(false))
    assertThat(savedRecordSlot.captured.createdDateTime, !equalTo(savedRecordSlot.captured.lastUpdatedDateTime))
    assertThat(savedRecordSlot.captured.decisionDetails, equalTo(decisionDetails))
    assertThat(savedRecordSlot.captured.decisionEmailId, present())
    assertThat(response.statusCode, equalTo(HttpStatus.OK))
    assertThat(
      response.body,
      equalTo(
        rescindRecordId
      )
    )
  }

  @Test
  fun `403 error thrown when requesting a new rescind if one is already in progress`() {
    val recall = mockk<Recall>()
    val documentBytes = "a document".toByteArray()
    val fileName = "email.msg"
    val mockRecord = mockk<RescindRecord>()
    val rescindRecordId = ::RescindRecordId.random()
    val details = "blah blah"

    every { recall.rescindRecords } returns setOf(mockRecord)
    every { recallRepository.getByRecallId(recallId) } returns recall
    every { tokenExtractor.getTokenFromHeader(bearerToken) } returns TokenExtractor.Token(userId.toString())
    every { mockRecord.id() } returns rescindRecordId
    every { mockRecord.hasBeenDecided() } returns false
    every { mockRecord.version } returns 1

    val request = RescindRequestRequest(
      details, LocalDate.now(),
      documentBytes.encodeToBase64String(), fileName
    )

    val response2 = underTest.createRescindRecord(recallId, request, bearerToken)

    assertThat(response2.statusCode, equalTo(HttpStatus.FORBIDDEN))

    verify { documentService wasNot Called }
    verify { rescindRecordRepository wasNot Called }
  }

  @Test
  fun `NotFoundException thrown if rescind record does not exist when deciding`() {
    val recall = mockk<Recall>()
    val mockRecord = mockk<RescindRecord>()
    val documentBytes = "a document".toByteArray()
    val fileName = "email.msg"
    val decisionDetails = "blah blah again"

    every { recallRepository.getByRecallId(recallId) } returns recall
    every { recall.rescindRecords } returns emptySet() andThen setOf(mockRecord)

    val decisionRequest = RescindDecisionRequest(
      false,
      decisionDetails, LocalDate.now(),
      documentBytes.encodeToBase64String(), fileName
    )

    assertThrows<RescindRecordNotFoundException> {
      underTest.decideRescindRecord(recallId, ::RescindRecordId.random(), decisionRequest, bearerToken)
    }
  }

  @Test
  fun `NotFoundException thrown if recall does not exist on rescind request creation`() {
    val documentBytes = "a document".toByteArray()
    val fileName = "email.msg"
    val details = "blah blah again"

    every { recallRepository.getByRecallId(recallId) } throws RecallNotFoundException(recallId)

    val request = RescindRequestRequest(
      details,
      LocalDate.now(),
      documentBytes.encodeToBase64String(),
      fileName
    )

    assertThrows<RecallNotFoundException> {
      underTest.createRescindRecord(recallId, request, bearerToken)
    }
  }

  @Test
  fun `NotFoundException thrown if recall does not exist on rescind decision`() {
    val documentBytes = "a document".toByteArray()
    val fileName = "email.msg"
    val decisionDetails = "blah blah again"

    every { recallRepository.getByRecallId(recallId) } throws RecallNotFoundException(recallId)

    val decisionRequest = RescindDecisionRequest(
      false,
      decisionDetails, LocalDate.now(),
      documentBytes.encodeToBase64String(), fileName
    )

    assertThrows<RecallNotFoundException> {
      underTest.decideRescindRecord(recallId, ::RescindRecordId.random(), decisionRequest, bearerToken)
    }
  }

  @Test
  fun `store record as version 2 if recall already has a decided record with version 1`() {
    val recall = mockk<Recall>()
    val documentBytes = "a document".toByteArray()
    val fileName = "email.msg"
    val emailId = ::DocumentId.random()
    val savedRescindRecord = slot<RescindRecord>()
    val rescindRecordId = ::RescindRecordId.random()
    val details = "detail"
    val existingRecord = mockk<RescindRecord>()

    every { recall.rescindRecords } returns setOf(existingRecord)
    every { existingRecord.version } returns 1
    every { existingRecord.hasBeenDecided() } returns true
    every { recallRepository.getByRecallId(recallId) } returns recall
    every {
      documentService.scanAndStoreDocument(
        recallId,
        userId,
        documentBytes,
        DocumentCategory.RESCIND_REQUEST_EMAIL,
        fileName,
      )
    } returns Success(emailId)
    every { tokenExtractor.getTokenFromHeader(bearerToken) } returns TokenExtractor.Token(userId.toString())
    every { rescindRecordRepository.save(capture(savedRescindRecord)) } returns existingRecord
    every { existingRecord.id() } returns rescindRecordId

    val request = RescindRequestRequest(
      details,
      LocalDate.now(),
      documentBytes.encodeToBase64String(),
      fileName
    )

    val response = underTest.createRescindRecord(recallId, request, bearerToken)

    assertThat(savedRescindRecord.captured.version, equalTo(2))
    assertThat(response.statusCode, equalTo(HttpStatus.CREATED))
    assertThat(
      response.body,
      equalTo(
        rescindRecordId
      )
    )
  }

  @Test
  fun `cant update a record which has already been decided`() {
    val recall = mockk<Recall>()
    val documentBytes = "a document".toByteArray()
    val fileName = "email.msg"
    val existingRecord = mockk<RescindRecord>()
    val rescindRecordId = ::RescindRecordId.random()
    val decisionDetails = "blah blah again"

    every { recallRepository.getByRecallId(recallId) } returns recall
    every { recall.rescindRecords } returns setOf(existingRecord)
    every { tokenExtractor.getTokenFromHeader(bearerToken) } returns TokenExtractor.Token(userId.toString())
    every { existingRecord.id() } returns rescindRecordId
    every { existingRecord.hasBeenDecided() } returns true

    val decisionRequest = RescindDecisionRequest(
      false,
      decisionDetails, LocalDate.now(),
      documentBytes.encodeToBase64String(), fileName
    )

    val response = underTest.decideRescindRecord(recallId, rescindRecordId, decisionRequest, bearerToken)

    assertThat(response.statusCode, equalTo(HttpStatus.FORBIDDEN))

    verify { documentService wasNot Called }
    verify { rescindRecordRepository wasNot Called }
  }
}
