package uk.gov.justice.digital.hmpps.managerecallsapi.service

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.PartBRecordController.PartBRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.OASYS_RISK_ASSESSMENT
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.PART_B_EMAIL_FROM_PROBATION
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.PART_B_RISK_REPORT
import uk.gov.justice.digital.hmpps.managerecallsapi.db.PartBRecord
import uk.gov.justice.digital.hmpps.managerecallsapi.db.PartBRecordRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.encodeToBase64String
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FileName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PartBRecordId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomFileName
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomHistoricalDate
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomString

class PartBRecordServiceTest {
  private val recallRepository = mockk<RecallRepository>()
  private val partBRecordRepository = mockk<PartBRecordRepository>()
  private val documentService = mockk<DocumentService>()

  private val userId = ::UserId.random()
  private val details = randomString()
  private val emailReceivedDate = randomHistoricalDate()
  private val partBDocumentId = ::DocumentId.random()
  private val partBContent = randomString().toByteArray()
  private val partBFileName = randomFileName()
  private val emailDocumentId = ::DocumentId.random()
  private val emailContent = randomString().toByteArray()
  private val emailFileName = randomFileName()
  private val oasysDocumentId = ::DocumentId.random()
  private val oasysContent = randomString().toByteArray()
  private val oasysFileName = randomFileName()

  private val underTest = PartBRecordService(
    partBRecordRepository,
    recallRepository,
    documentService,
  )

  private val recallId = ::RecallId.random()

  @Test
  fun `stores first partBRecord for recall as version 1`() {
    val recall = mockk<Recall>()
    val savedPartBRecordSlot = slot<PartBRecord>()
    val mockPartBRecord = mockk<PartBRecord>()
    val partBRecordId = ::PartBRecordId.random()

    every { recall.partBRecords } returns emptySet()
    every { recallRepository.getByRecallId(recallId) } returns recall

    mockScanAndStoreDocumentSuccess(partBContent, PART_B_RISK_REPORT, partBFileName, partBDocumentId, details)
    mockScanAndStoreDocumentSuccess(emailContent, PART_B_EMAIL_FROM_PROBATION, emailFileName, emailDocumentId, details)
    mockScanAndStoreDocumentSuccess(oasysContent, OASYS_RISK_ASSESSMENT, oasysFileName, oasysDocumentId, "Uploaded alongside Part B")
    every { partBRecordRepository.save(capture(savedPartBRecordSlot)) } returns mockPartBRecord
    every { mockPartBRecord.id() } returns partBRecordId
    every { mockPartBRecord.version } returns 1

    val request = partBRequest()

    val response = underTest.createRecord(recallId, userId, request)

    assertThat(response, equalTo(Success(partBRecordId)))
    assertThat(savedPartBRecordSlot.captured.details, equalTo(details))
    assertThat(savedPartBRecordSlot.captured.partBReceivedDate, equalTo(emailReceivedDate))
    assertThat(savedPartBRecordSlot.captured.partBDocumentId, equalTo(partBDocumentId.value))
    assertThat(savedPartBRecordSlot.captured.emailId, equalTo(emailDocumentId.value))
    assertThat(savedPartBRecordSlot.captured.oasysDocumentId, equalTo(oasysDocumentId.value))
    assertThat(savedPartBRecordSlot.captured.version, equalTo(1))
    assertThat(savedPartBRecordSlot.captured.createdByUserId, equalTo(userId.value))
  }

  @Test
  fun `stores partBRecord also if oasys is not provided`() {
    val recall = mockk<Recall>()
    val savedPartBRecordSlot = slot<PartBRecord>()
    val mockPartBRecord = mockk<PartBRecord>()
    val partBRecordId = ::PartBRecordId.random()

    every { recall.partBRecords } returns emptySet()
    every { recallRepository.getByRecallId(recallId) } returns recall

    mockScanAndStoreDocumentSuccess(partBContent, PART_B_RISK_REPORT, partBFileName, partBDocumentId, details)
    mockScanAndStoreDocumentSuccess(emailContent, PART_B_EMAIL_FROM_PROBATION, emailFileName, emailDocumentId, details)
    every { partBRecordRepository.save(capture(savedPartBRecordSlot)) } returns mockPartBRecord
    every { mockPartBRecord.id() } returns partBRecordId
    every { mockPartBRecord.version } returns 1

    val request = partBRequest().copy(oasysFileName = null, oasysFileContent = null)

    val response = underTest.createRecord(recallId, userId, request)

    assertThat(response, equalTo(Success(partBRecordId)))
    assertThat(savedPartBRecordSlot.captured.details, equalTo(details))
    assertThat(savedPartBRecordSlot.captured.partBReceivedDate, equalTo(emailReceivedDate))
    assertThat(savedPartBRecordSlot.captured.partBDocumentId, equalTo(partBDocumentId.value))
    assertThat(savedPartBRecordSlot.captured.emailId, equalTo(emailDocumentId.value))
    assertThat(savedPartBRecordSlot.captured.oasysDocumentId, equalTo(null))
    assertThat(savedPartBRecordSlot.captured.version, equalTo(1))
    assertThat(savedPartBRecordSlot.captured.createdByUserId, equalTo(userId.value))
  }

  @Test
  fun `store record as version 2 if recall already has a partBRecord with version 1`() {
    val recall = mockk<Recall>()
    val savedPartBRecordSlot = slot<PartBRecord>()
    val partBRecordId = ::PartBRecordId.random()
    val existingPartBRecord = mockk<PartBRecord>()

    every { recall.partBRecords } returns setOf(existingPartBRecord)
    every { existingPartBRecord.version } returns 1
    every { recallRepository.getByRecallId(recallId) } returns recall

    mockScanAndStoreDocumentSuccess(partBContent, PART_B_RISK_REPORT, partBFileName, partBDocumentId, details)
    mockScanAndStoreDocumentSuccess(emailContent, PART_B_EMAIL_FROM_PROBATION, emailFileName, emailDocumentId, details)
    mockScanAndStoreDocumentSuccess(oasysContent, OASYS_RISK_ASSESSMENT, oasysFileName, oasysDocumentId, "Uploaded alongside Part B")
    every { partBRecordRepository.save(capture(savedPartBRecordSlot)) } returns existingPartBRecord
    every { existingPartBRecord.id() } returns partBRecordId
    every { existingPartBRecord.version } returns 1

    val request = partBRequest()

    val response = underTest.createRecord(recallId, userId, request)

    assertThat(response, equalTo(Success(partBRecordId)))
    assertThat(savedPartBRecordSlot.captured.details, equalTo(details))
    assertThat(savedPartBRecordSlot.captured.version, equalTo(2))
  }

  @Test
  fun `Returns failure if document scan returns Failure`() {
    val recall = mockk<Recall>()

    every { recallRepository.getByRecallId(recallId) } returns recall
    mockScanAndStoreDocumentSuccess(partBContent, PART_B_RISK_REPORT, partBFileName, partBDocumentId, details)
    mockScanAndStoreDocumentSuccess(emailContent, PART_B_EMAIL_FROM_PROBATION, emailFileName, emailDocumentId, details)

    mockScanAndStoreDocumentFailure(oasysContent, OASYS_RISK_ASSESSMENT, oasysFileName, "Uploaded alongside Part B")

    val request = partBRequest()

    val response = underTest.createRecord(recallId, userId, request)

    assertThat(response, equalTo(Failure(listOf(Pair(OASYS_RISK_ASSESSMENT, oasysFileName)))))
  }

  @Test
  fun `NotFoundException thrown if recall does not exist on partBRecord creation`() {
    every { recallRepository.getByRecallId(recallId) } throws RecallNotFoundException(recallId)

    val request = partBRequest()

    assertThrows<RecallNotFoundException> { underTest.createRecord(recallId, userId, request) }
  }

  @Test
  fun `IllegalArgumentException thrown if partBRecord has fileName but empty fileContent`() {
    val recall = mockk<Recall>()
    every { recallRepository.getByRecallId(recallId) } returns recall

    val request = partBRequest().copy(oasysFileName = null)

    assertThrows<IllegalArgumentException> { underTest.createRecord(recallId, userId, request) }
  }

  @Test
  fun `IllegalArgumentException thrown if partBRecord has oasys fileContent but null fileName`() {
    val recall = mockk<Recall>()
    every { recallRepository.getByRecallId(recallId) } returns recall

    val request = partBRequest().copy(oasysFileContent = null)

    assertThrows<IllegalArgumentException> { underTest.createRecord(recallId, userId, request) }
  }

  private fun partBRequest() = PartBRequest(
    details,
    emailReceivedDate,
    partBFileName,
    partBContent.encodeToBase64String(),
    emailFileName,
    emailContent.encodeToBase64String(),
    oasysFileName,
    oasysContent.encodeToBase64String()
  )

  private fun mockScanAndStoreDocumentSuccess(documentBytes: ByteArray, category: DocumentCategory, fileName: FileName, documentId: DocumentId, details: String) {
    everyScanAndStoreDocument(documentBytes, category, fileName, details) returns Success(documentId)
  }

  private fun mockScanAndStoreDocumentFailure(
    documentBytes: ByteArray,
    category: DocumentCategory,
    fileName: FileName,
    details: String
  ) {
    everyScanAndStoreDocument(documentBytes, category, fileName, details) returns Failure(mockk())
  }

  private fun everyScanAndStoreDocument(
    documentBytes: ByteArray,
    category: DocumentCategory,
    fileName: FileName,
    details: String
  ) =
    every {
      documentService.scanAndStoreDocument(
        recallId,
        userId,
        documentBytes,
        category,
        fileName,
        details
      )
    }
}
