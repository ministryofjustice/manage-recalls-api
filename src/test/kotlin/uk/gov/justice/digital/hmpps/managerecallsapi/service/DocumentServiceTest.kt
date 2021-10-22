package uk.gov.justice.digital.hmpps.managerecallsapi.service

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.nhaarman.mockitokotlin2.any
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.get
import io.mockk.Called
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.OTHER
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.PART_A_RECALL_REPORT
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UnversionedDocument
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UnversionedDocumentRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.VersionedDocument
import uk.gov.justice.digital.hmpps.managerecallsapi.db.VersionedDocumentRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.toRecallDocument
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomDocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomString
import uk.gov.justice.digital.hmpps.managerecallsapi.service.VirusScanResult.NoVirusFound
import uk.gov.justice.digital.hmpps.managerecallsapi.service.VirusScanResult.VirusFound
import uk.gov.justice.digital.hmpps.managerecallsapi.storage.S3Service
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.UUID

internal class DocumentServiceTest {

  private val s3Service = mockk<S3Service>()
  private val recallRepository = mockk<RecallRepository>()
  private val versionedDocumentRepository = mockk<VersionedDocumentRepository>()
  private val unversionedDocumentRepository = mockk<UnversionedDocumentRepository>()
  private val virusScanner = mockk<VirusScanner>()
  private val fixedClock = Clock.fixed(Instant.parse("2021-10-04T16:48:30.00Z"), ZoneId.of("UTC"))

  private val underTest = DocumentService(s3Service, recallRepository, versionedDocumentRepository, unversionedDocumentRepository, virusScanner, fixedClock)

  private val recallId = ::RecallId.random()
  private val documentBytes = randomString().toByteArray()
  private val nomsNumber = NomsNumber("A1235B")
  private val aRecallWithoutDocuments = Recall(recallId, nomsNumber)
  private val documentCategory = PART_A_RECALL_REPORT
  private val fileName = randomString()

  @Test
  fun `can scan and upload a versioned document to S3 and add it to the existing recall`() {
    val uploadedToS3DocumentIdSlot = slot<UUID>()
    val savedDocumentSlot = slot<VersionedDocument>()

    every { recallRepository.getByRecallId(recallId) } returns aRecallWithoutDocuments
    every { virusScanner.scan(documentBytes) } returns NoVirusFound
    every { versionedDocumentRepository.findByRecallIdAndCategory(recallId.value, any()) } returns null
    every { s3Service.uploadFile(capture(uploadedToS3DocumentIdSlot), documentBytes) } just runs
    every { versionedDocumentRepository.save(capture(savedDocumentSlot)) } returns any()

    val result = underTest.scanAndStoreDocument(recallId, documentBytes, documentCategory, fileName)

    assertThat(result.get(), equalTo(uploadedToS3DocumentIdSlot.captured))
    assertThat(
      savedDocumentSlot.captured,
      equalTo(
        VersionedDocument(
          uploadedToS3DocumentIdSlot.captured,
          recallId.value,
          documentCategory,
          fileName,
          OffsetDateTime.now(fixedClock)
        )
      )
    )

    verify { versionedDocumentRepository.save(any()) }
    verify(exactly = 0) { unversionedDocumentRepository.save(any()) }
  }

  @Test
  fun `uploading a versioned document with a virus does not add to S3 or add to the recall`() {
    val expectedVirusScanResult = VirusFound(emptyMap())
    every { recallRepository.getByRecallId(recallId) } returns aRecallWithoutDocuments
    every { virusScanner.scan(documentBytes) } returns expectedVirusScanResult

    val result = underTest.scanAndStoreDocument(recallId, documentBytes, documentCategory, fileName)

    assertThat(result, equalTo(Failure(expectedVirusScanResult)))

    verify(exactly = 0) { s3Service.uploadFile(any(), documentBytes) }
    verify(exactly = 0) { recallRepository.addDocumentToRecall(recallId, any()) }
    verify { versionedDocumentRepository wasNot Called }
  }

  @Test
  fun `add versioned document with a category that already exists updates the existing document in both S3 and the repository`() {
    val existingDocumentId = UUID.randomUUID()
    val existingDocument = VersionedDocument(
      existingDocumentId,
      recallId.value,
      documentCategory,
      fileName,
      OffsetDateTime.now(fixedClock)
    )
    val aRecallWithDocument = Recall(recallId, nomsNumber, documents = setOf(existingDocument))
    val newFileName = randomString()
    val updatedDocument = VersionedDocument(
      existingDocumentId,
      recallId.value,
      documentCategory,
      newFileName,
      OffsetDateTime.now(fixedClock)
    )

    every { recallRepository.getByRecallId(recallId) } returns aRecallWithDocument
    every { versionedDocumentRepository.findByRecallIdAndCategory(recallId.value, any()) } returns existingDocument
    every { s3Service.uploadFile(existingDocumentId, documentBytes) } just runs
    every { versionedDocumentRepository.save(updatedDocument) } returns updatedDocument

    val actualDocumentId = underTest.storeDocument(recallId, documentBytes, documentCategory, newFileName)

    assertThat(actualDocumentId, equalTo(existingDocumentId))
  }

  @Test
  fun `gets a versioned document by recall ID and document ID`() {
    val aDocumentId = UUID.randomUUID()
    val aDocument = VersionedDocument(
      id = aDocumentId,
      recallId = recallId.value,
      category = PART_A_RECALL_REPORT,
      fileName = randomString(),
      createdDateTime = OffsetDateTime.now()
    )
    val aRecallWithDocument = aRecallWithoutDocuments.copy(versionedDocuments = setOf(aDocument))
    val fileBytes = randomString().toByteArray()

    every { recallRepository.getByRecallId(recallId) } returns aRecallWithDocument
    every { versionedDocumentRepository.findByRecallIdAndDocumentId(recallId, aDocumentId) } returns aDocument
    every { s3Service.downloadFile(aDocumentId) } returns fileBytes

    val (actualDocument, actualBytes) = underTest.getDocument(recallId, aDocumentId)

    assertThat(actualDocument, equalTo(aDocument.toRecallDocument()))
    assertThat(actualBytes, equalTo(fileBytes))
  }

  @Test
  fun `gets a versioned document by recall ID and document category`() {
    val aDocumentCategory = randomDocumentCategory()
    val documentId = UUID.randomUUID()
    val aDocument = VersionedDocument(
      id = documentId,
      recallId = recallId.value,
      category = aDocumentCategory,
      fileName = randomString(),
      createdDateTime = OffsetDateTime.now()
    )
    val aRecallWithDocument = aRecallWithoutDocuments.copy(versionedDocuments = setOf(aDocument))
    val fileBytes = randomString().toByteArray()

    every { recallRepository.getByRecallId(recallId) } returns aRecallWithDocument
    every { versionedDocumentRepository.findByRecallIdAndCategory(recallId.value, aDocumentCategory) } returns aDocument
    every { s3Service.downloadFile(documentId) } returns fileBytes

    val actualBytes = underTest.getVersionedDocumentContentWithCategory(recallId, aDocumentCategory)

    assertThat(actualBytes, equalTo(fileBytes))
  }

  @Test
  fun `gets null if a document by recall ID and document category doesnt exist`() {
    val aDocumentCategory = randomDocumentCategory()

    every { recallRepository.getByRecallId(recallId) } returns aRecallWithoutDocuments
    every { versionedDocumentRepository.findByRecallIdAndCategory(recallId.value, aDocumentCategory) } returns null

    val result = underTest.getVersionedDocumentContentWithCategoryIfExists(recallId, aDocumentCategory)

    verify(exactly = 0) { s3Service.uploadFile(any(), any()) }
    assertThat(result, equalTo(null))
  }

  @Test
  fun `getDocumentContentWithCategory throws a custom 'document with category not found' error if no document of category is found for recall`() {
    val randomDocumentCategory = randomDocumentCategory()
    every { recallRepository.getByRecallId(recallId) } returns aRecallWithoutDocuments
    every { versionedDocumentRepository.findByRecallIdAndCategory(recallId.value, randomDocumentCategory) } returns null

    assertThrows<RecallDocumentWithCategoryNotFoundException> {
      underTest.getVersionedDocumentContentWithCategory(recallId, randomDocumentCategory)
    }
  }

  @Test
  fun `getDocumentContentWithCategory ignores all but one document matching recall ID and document category`() {
    val theDocumentCategory = randomDocumentCategory()
    val documentOne = VersionedDocument(
      UUID.randomUUID(),
      recallId.value,
      theDocumentCategory,
      randomString(),
      OffsetDateTime.now()
    )
    val documentTwo = VersionedDocument(
      UUID.randomUUID(),
      recallId.value,
      theDocumentCategory,
      randomString(),
      OffsetDateTime.now()
    )
    val aRecallWithDocument = aRecallWithoutDocuments.copy(versionedDocuments = setOf(documentOne, documentTwo))
    val fileBytes = randomString().toByteArray()

    every { recallRepository.getByRecallId(recallId) } returns aRecallWithDocument
    every { versionedDocumentRepository.findByRecallIdAndCategory(recallId.value, theDocumentCategory) } returns documentOne
    every { s3Service.downloadFile(any()) } returns fileBytes

    val actualBytes = underTest.getVersionedDocumentContentWithCategory(recallId, theDocumentCategory)

    assertThat(actualBytes, equalTo(fileBytes))
  }

  @Test
  fun `throws an exception when saving an unversioned document without filename with`() {
    val documentBytes = randomString().toByteArray()
    every { recallRepository.getByRecallId(recallId) } returns aRecallWithoutDocuments
    every { s3Service.uploadFile(any(), documentBytes) } just runs

    assertThrows<NullPointerException> {
      underTest.storeDocument(recallId, documentBytes, OTHER, null)
    }
  }

  @Test
  fun `stores a document of type OTHER in unversioned repository`() {
    val documentBytes = randomString().toByteArray()
    val documentId = UUID.randomUUID()
    val savedDocumentSlot = slot<UnversionedDocument>()

    every { recallRepository.getByRecallId(recallId) } returns aRecallWithoutDocuments
    every { s3Service.uploadFile(any(), documentBytes) } just runs
    every { unversionedDocumentRepository.save(capture(savedDocumentSlot)) } returns UnversionedDocument(
      documentId,
      recallId.value,
      OTHER,
      "filename.txt",
      OffsetDateTime.now()
    )

    val result = underTest.storeDocument(recallId, documentBytes, OTHER, "filename.txt")

    verify { unversionedDocumentRepository.save(any()) }
    verify(exactly = 0) { versionedDocumentRepository.save(any()) }

    assertThat(result, equalTo(savedDocumentSlot.captured.id))
  }

  @Test
  fun `looks for a document in versioned repo and then unversioned repository if not found`() {
    val documentBytes = randomString().toByteArray()
    val documentId = UUID.randomUUID()
    val unversionedDocument = UnversionedDocument(documentId, recallId.value, OTHER, "file.txt", OffsetDateTime.now())

    every { recallRepository.getByRecallId(recallId) } returns aRecallWithoutDocuments
    every { versionedDocumentRepository.findByRecallIdAndDocumentId(recallId, documentId) } returns null
    every { unversionedDocumentRepository.getByRecallIdAndDocumentId(recallId, documentId) } returns unversionedDocument
    every { s3Service.downloadFile(documentId) } returns documentBytes

    val resultPair = underTest.getDocument(recallId, documentId)

    assertThat(resultPair.first, equalTo(unversionedDocument.toRecallDocument()))
    assertThat(String(resultPair.second), equalTo(String(documentBytes)))
  }
}
