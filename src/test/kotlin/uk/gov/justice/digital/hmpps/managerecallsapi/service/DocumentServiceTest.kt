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
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.LICENCE
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.OTHER
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.PART_A_RECALL_REPORT
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.UNCATEGORISED
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UnversionedDocument
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UnversionedDocumentRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.VersionedDocument
import uk.gov.justice.digital.hmpps.managerecallsapi.db.VersionedDocumentRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
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

  private val underTest = DocumentService(
    s3Service,
    recallRepository,
    versionedDocumentRepository,
    unversionedDocumentRepository,
    virusScanner,
    fixedClock
  )

  private val recallId = ::RecallId.random()
  private val documentBytes = randomString().toByteArray()
  private val nomsNumber = NomsNumber("A1235B")
  private val aRecallWithoutDocuments = Recall(recallId, nomsNumber, OffsetDateTime.now())
  private val documentCategory = PART_A_RECALL_REPORT
  private val fileName = randomString()

  // TODO - parameterise tests for versioned and unversioned category

  @Test
  fun `can scan and upload a versioned document to S3 and add it to the existing recall`() {
    val uploadedToS3DocumentIdSlot = slot<DocumentId>()
    val savedDocumentSlot = slot<VersionedDocument>()
    val versionedDocument = mockk<VersionedDocument>()

    every { recallRepository.getByRecallId(recallId) } returns aRecallWithoutDocuments
    every { virusScanner.scan(documentBytes) } returns NoVirusFound
    every { versionedDocumentRepository.findByRecallIdAndCategory(recallId.value, any()) } returns null
    every { s3Service.uploadFile(capture(uploadedToS3DocumentIdSlot), documentBytes) } just runs
    every { versionedDocument.toRecallDocument() } returns mockk()
    every { versionedDocumentRepository.save(capture(savedDocumentSlot)) } returns versionedDocument

    val result = underTest.scanAndStoreDocument(recallId, documentBytes, documentCategory, fileName)

    assertThat(result.get(), equalTo(uploadedToS3DocumentIdSlot.captured))
    assertThat(
      savedDocumentSlot.captured,
      equalTo(
        VersionedDocument(
          uploadedToS3DocumentIdSlot.captured,
          recallId,
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
    verify { unversionedDocumentRepository wasNot Called }
    verify { versionedDocumentRepository wasNot Called }
  }

  @Test
  fun `add versioned document with a category that already exists updates the existing document in both S3 and the repository`() {
    val existingDocumentId = ::DocumentId.random()
    val existingDocument = VersionedDocument(
      existingDocumentId,
      recallId,
      documentCategory,
      fileName,
      OffsetDateTime.now(fixedClock)
    )
    val aRecallWithDocument = Recall(
      recallId,
      nomsNumber,
      OffsetDateTime.now(),
      OffsetDateTime.now(),
      documents = setOf(existingDocument)
    )
    val newFileName = randomString()
    val updatedDocument = VersionedDocument(
      existingDocumentId,
      recallId,
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
    val aDocumentId = ::DocumentId.random()
    val aDocument = VersionedDocument(
      aDocumentId,
      recallId,
      PART_A_RECALL_REPORT,
      randomString(),
      OffsetDateTime.now()
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
    val documentId = ::DocumentId.random()
    val aDocument = VersionedDocument(
      documentId,
      recallId,
      aDocumentCategory,
      randomString(),
      OffsetDateTime.now()
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
    every {
      versionedDocumentRepository.findByRecallIdAndCategory(
        recallId.value,
        theDocumentCategory
      )
    } returns documentOne
    every { s3Service.downloadFile(any()) } returns fileBytes

    val actualBytes = underTest.getVersionedDocumentContentWithCategory(recallId, theDocumentCategory)

    assertThat(actualBytes, equalTo(fileBytes))
  }

  @Test
  fun `stores a document of type OTHER in unversioned repository`() {
    val documentBytes = randomString().toByteArray()
    val documentId = ::DocumentId.random()
    val savedDocumentSlot = slot<UnversionedDocument>()

    every { recallRepository.getByRecallId(recallId) } returns aRecallWithoutDocuments
    every { s3Service.uploadFile(any(), documentBytes) } just runs
    every { unversionedDocumentRepository.save(capture(savedDocumentSlot)) } returns UnversionedDocument(
      documentId,
      recallId,
      OTHER,
      "filename.txt",
      OffsetDateTime.now()
    )

    val result = underTest.storeDocument(recallId, documentBytes, OTHER, "filename.txt")

    verify { unversionedDocumentRepository.save(any()) }
    verify(exactly = 0) { versionedDocumentRepository.save(any()) }

    assertThat(result, equalTo(savedDocumentSlot.captured.id()))
  }

  @Test
  fun `looks for a document in versioned repo and then unversioned repository if not found`() {
    val documentBytes = randomString().toByteArray()
    val documentId = ::DocumentId.random()
    val unversionedDocument = UnversionedDocument(documentId, recallId, OTHER, "file.txt", OffsetDateTime.now())

    every { recallRepository.getByRecallId(recallId) } returns aRecallWithoutDocuments
    every { versionedDocumentRepository.findByRecallIdAndDocumentId(recallId, documentId) } returns null
    every { unversionedDocumentRepository.getByRecallIdAndDocumentId(recallId, documentId) } returns unversionedDocument
    every { s3Service.downloadFile(documentId) } returns documentBytes

    val resultPair = underTest.getDocument(recallId, documentId)

    assertThat(resultPair.first, equalTo(unversionedDocument.toRecallDocument()))
    assertThat(String(resultPair.second), equalTo(String(documentBytes)))
  }

  @Test
  fun `update a document category for a document that doesnt exist throws not found`() {
    val randomDocumentCategory = randomDocumentCategory()
    val documentId = ::DocumentId.random()
    every { recallRepository.getByRecallId(recallId) } returns aRecallWithoutDocuments
    every { versionedDocumentRepository.findByRecallIdAndDocumentId(recallId, documentId) } returns null
    every {
      unversionedDocumentRepository.getByRecallIdAndDocumentId(
        recallId,
        documentId
      )
    } throws RecallDocumentNotFoundException(recallId, documentId)

    assertThrows<RecallDocumentNotFoundException> {
      underTest.updateDocumentCategory(recallId, documentId, randomDocumentCategory)
    }
  }

  @Test
  fun `update a document category for a versioned category to another versioned category`() {
    val documentId = ::DocumentId.random()
    val now = OffsetDateTime.now()
    val versionedDocument = VersionedDocument(documentId, recallId, PART_A_RECALL_REPORT, "parta.pdf", now)
    val updatedCategory = LICENCE
    val updatedVersionDocument =
      versionedDocument.copy(category = updatedCategory, createdDateTime = OffsetDateTime.now(fixedClock))
    val recallWithDoc = aRecallWithoutDocuments.copy(versionedDocuments = setOf(versionedDocument))

    every { recallRepository.getByRecallId(recallId) } returns recallWithDoc
    every { versionedDocumentRepository.findByRecallIdAndDocumentId(recallId, documentId) } returns versionedDocument
    every { versionedDocumentRepository.save(updatedVersionDocument) } returns updatedVersionDocument

    val result = underTest.updateDocumentCategory(recallId, documentId, updatedCategory)

    assertThat(result, equalTo(updatedVersionDocument.toRecallDocument()))

    verify { unversionedDocumentRepository wasNot Called }
    verify { versionedDocumentRepository.save(updatedVersionDocument) }
  }

  @Test
  fun `update a document category for an unversioned category to another unversioned category`() {
    val documentId = ::DocumentId.random()
    val now = OffsetDateTime.now()
    val unversionedDocument = UnversionedDocument(documentId, recallId, OTHER, "my-document.pdf", now)
    val updatedCategory = UNCATEGORISED
    val updatedUnversionedDocument =
      unversionedDocument.copy(category = updatedCategory, createdDateTime = OffsetDateTime.now(fixedClock))
    val recallWithDoc = aRecallWithoutDocuments.copy(unversionedDocuments = setOf(unversionedDocument))

    every { recallRepository.getByRecallId(recallId) } returns recallWithDoc
    every { versionedDocumentRepository.findByRecallIdAndDocumentId(recallId, documentId) } returns null
    every { unversionedDocumentRepository.getByRecallIdAndDocumentId(recallId, documentId) } returns unversionedDocument
    every { unversionedDocumentRepository.save(updatedUnversionedDocument) } returns updatedUnversionedDocument

    val result = underTest.updateDocumentCategory(recallId, documentId, updatedCategory)

    assertThat(result, equalTo(updatedUnversionedDocument.toRecallDocument()))

    verify { versionedDocumentRepository.save(any()) wasNot Called }
    verify { unversionedDocumentRepository.save(updatedUnversionedDocument) }
  }

  @Test
  fun `update a document category for a versioned category to an unversioned category moves to correct table and deletes old entry`() {
    val documentId = ::DocumentId.random()
    val now = OffsetDateTime.now()
    val versionedDocument = VersionedDocument(documentId, recallId, PART_A_RECALL_REPORT, "part-a.pdf", now)
    val updatedCategory = UNCATEGORISED
    val updatedUnversionedDocument =
      UnversionedDocument(documentId, recallId, updatedCategory, "part-a.pdf", OffsetDateTime.now(fixedClock))
    val recallWithDoc = aRecallWithoutDocuments.copy(versionedDocuments = setOf(versionedDocument))

    every { recallRepository.getByRecallId(recallId) } returns recallWithDoc
    every { versionedDocumentRepository.findByRecallIdAndDocumentId(recallId, documentId) } returns versionedDocument
    every { versionedDocumentRepository.delete(versionedDocument) } just runs
    every { unversionedDocumentRepository.save(updatedUnversionedDocument) } returns updatedUnversionedDocument

    val result = underTest.updateDocumentCategory(recallId, documentId, updatedCategory)

    assertThat(result, equalTo(updatedUnversionedDocument.toRecallDocument()))

    verify { versionedDocumentRepository.delete(versionedDocument) }
    verify { unversionedDocumentRepository.save(updatedUnversionedDocument) }
    verify { versionedDocumentRepository.save(any()) wasNot Called }
    verify { unversionedDocumentRepository.getByRecallIdAndDocumentId(recallId, documentId) wasNot Called }
  }

  @Test
  fun `update a document category for an unversioned category to a versioned category moves to correct table and deletes old entry`() {
    val documentId = ::DocumentId.random()
    val now = OffsetDateTime.now()
    val originalDocument = UnversionedDocument(documentId, recallId, UNCATEGORISED, "license.pdf", now)
    val updatedCategory = LICENCE
    val updatedDocument =
      VersionedDocument(documentId, recallId, updatedCategory, "license.pdf", OffsetDateTime.now(fixedClock))
    val recallWithDoc = aRecallWithoutDocuments.copy(unversionedDocuments = setOf(originalDocument))

    every { recallRepository.getByRecallId(recallId) } returns recallWithDoc
    every { versionedDocumentRepository.findByRecallIdAndDocumentId(recallId, documentId) } returns null
    every { unversionedDocumentRepository.getByRecallIdAndDocumentId(recallId, documentId) } returns originalDocument
    every { unversionedDocumentRepository.delete(originalDocument) } just runs
    every { versionedDocumentRepository.save(updatedDocument) } returns updatedDocument

    val result = underTest.updateDocumentCategory(recallId, documentId, updatedCategory)

    assertThat(result, equalTo(updatedDocument.toRecallDocument()))

    verify { unversionedDocumentRepository.delete(originalDocument) }
    verify { versionedDocumentRepository.save(updatedDocument) }
    verify { unversionedDocumentRepository.save(any()) wasNot Called }
  }
}
