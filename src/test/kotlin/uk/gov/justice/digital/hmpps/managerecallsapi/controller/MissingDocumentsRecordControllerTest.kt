package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Success
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.extractor.TokenExtractor
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.db.MissingDocumentsRecord
import uk.gov.justice.digital.hmpps.managerecallsapi.db.MissingDocumentsRecordRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.encodeToBase64String
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.MissingDocumentsRecordId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.service.DocumentService
import java.time.OffsetDateTime

class MissingDocumentsRecordControllerTest {
  private val recallRepository = mockk<RecallRepository>()
  private val missingDocumentsRecordRepository = mockk<MissingDocumentsRecordRepository>()
  private val documentService = mockk<DocumentService>()
  private val tokenExtractor = mockk<TokenExtractor>()

  private val underTest = MissingDocumentsRecordController(
    recallRepository,
    missingDocumentsRecordRepository,
    documentService,
    tokenExtractor
  )

  private val recallId = ::RecallId.random()

  @Test
  fun `store first record as version 1`() {
    val recall = mockk<Recall>()
    val documentBytes = "a document".toByteArray()
    val fileName = "email.msg"
    val emailId = ::DocumentId.random()
    val savedMissingDocumentsRecord = slot<MissingDocumentsRecord>()
    val record = mockk<MissingDocumentsRecord>()
    val bearerToken = "BEARER TOKEN"
    val userId = ::UserId.random()
    val missingDocumentsRecordId = ::MissingDocumentsRecordId.random()
    val createdDateTime = OffsetDateTime.now()

    every { recall.missingDocumentsRecords } returns emptySet()

    every { recallRepository.getByRecallId(recallId) } returns recall

    every {
      documentService.scanAndStoreDocument(
        recallId,
        userId,
        documentBytes,
        DocumentCategory.MISSING_DOCUMENTS_EMAIL,
        fileName
      )
    } returns Success(emailId)

    every { tokenExtractor.getTokenFromHeader(bearerToken) } returns TokenExtractor.Token(userId.toString())

    every { missingDocumentsRecordRepository.save(capture(savedMissingDocumentsRecord)) } returns record
    every { record.toResponse() } returns Api.MissingDocumentsRecord(
      missingDocumentsRecordId,
      listOf(DocumentCategory.PART_A_RECALL_REPORT),
      emailId,
      "blah blah",
      1,
      userId,
      createdDateTime
    )

    val request = MissingDocumentsRecordRequest(
      recallId, listOf(DocumentCategory.PART_A_RECALL_REPORT), "blah blah",
      documentBytes.encodeToBase64String(), fileName
    )

    val response = underTest.createMissingDocumentsRecord(request, bearerToken)

    assertThat(savedMissingDocumentsRecord.captured.version, equalTo(1))

    assertThat(response.statusCode, equalTo(HttpStatus.CREATED))

    assertThat(
      response.body,
      equalTo(
        Api.MissingDocumentsRecord(
          missingDocumentsRecordId,
          listOf(DocumentCategory.PART_A_RECALL_REPORT),
          emailId,
          "blah blah",
          1,
          userId,
          createdDateTime
        )
      )
    )
  }

  @Test
  fun `store record as version 2 if recall already has a record with version 1`() {
    val recall = mockk<Recall>()
    val documentBytes = "a document".toByteArray()
    val fileName = "email.msg"
    val emailId = ::DocumentId.random()
    val savedMissingDocumentsRecord = slot<MissingDocumentsRecord>()
    val record = mockk<MissingDocumentsRecord>()
    val bearerToken = "BEARER TOKEN"
    val userId = ::UserId.random()
    val missingDocumentsRecordId = ::MissingDocumentsRecordId.random()
    val createdDateTime = OffsetDateTime.now()

    val existingRecord = mockk<MissingDocumentsRecord>()
    every { recall.missingDocumentsRecords } returns setOf(existingRecord)

    every { existingRecord.version } returns 1

    every { recallRepository.getByRecallId(recallId) } returns recall

    every {
      documentService.scanAndStoreDocument(
        recallId,
        userId,
        documentBytes,
        DocumentCategory.MISSING_DOCUMENTS_EMAIL,
        fileName
      )
    } returns Success(emailId)

    every { tokenExtractor.getTokenFromHeader(bearerToken) } returns TokenExtractor.Token(userId.toString())

    every { missingDocumentsRecordRepository.save(capture(savedMissingDocumentsRecord)) } returns record
    val missingDocumentsRecordResponse = Api.MissingDocumentsRecord(
      missingDocumentsRecordId,
      listOf(DocumentCategory.PART_A_RECALL_REPORT),
      emailId,
      "blah blah \n blah blee blah",
      2,
      userId,
      createdDateTime
    )
    every { record.toResponse() } returns missingDocumentsRecordResponse

    val request = MissingDocumentsRecordRequest(
      recallId, listOf(DocumentCategory.PART_A_RECALL_REPORT), "blah blah \n blah blee blah",
      documentBytes.encodeToBase64String(), fileName
    )

    val response = underTest.createMissingDocumentsRecord(request, bearerToken)

    assertThat(savedMissingDocumentsRecord.captured.version, equalTo(2))

    assertThat(response.statusCode, equalTo(HttpStatus.CREATED))

    assertThat(
      response.body,
      equalTo(
        missingDocumentsRecordResponse
      )
    )
  }
}
