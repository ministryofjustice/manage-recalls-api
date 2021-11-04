package uk.gov.justice.digital.hmpps.managerecallsapi.service

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.get
import io.mockk.Called
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Document
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.LICENCE
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.OTHER
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.PART_A_RECALL_REPORT
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.REVOCATION_ORDER
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.UNCATEGORISED
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomString
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomVersionedDocumentCategory
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
  private val documentRepository = mockk<DocumentRepository>()
  private val virusScanner = mockk<VirusScanner>()
  private val fixedClock = Clock.fixed(Instant.parse("2021-10-04T16:48:30.00Z"), ZoneId.of("UTC"))

  private val underTest = DocumentService(
    s3Service,
    recallRepository,
    documentRepository,
    virusScanner,
    fixedClock
  )

  private val recallId = ::RecallId.random()
  private val documentBytes = randomString().toByteArray()
  private val nomsNumber = NomsNumber("A1235B")
  private val aRecallWithoutDocuments = Recall(recallId, nomsNumber, ::UserId.random(), OffsetDateTime.now())
  private val documentCategory = PART_A_RECALL_REPORT
  private val fileName = randomString()

  // TODO - parameterise tests for versioned and unversioned category

  @Test
  fun `can scan and upload a versioned document to S3 and add it to the existing recall`() {
    val uploadedToS3DocumentIdSlot = slot<DocumentId>()
    val savedDocumentSlot = slot<Document>()
    val document = mockk<Document>()

    every { recallRepository.getByRecallId(recallId) } returns aRecallWithoutDocuments
    every { virusScanner.scan(documentBytes) } returns NoVirusFound
    every { documentRepository.findLatestVersionedDocumentByRecallIdAndCategory(recallId, any()) } returns null
    every { s3Service.uploadFile(capture(uploadedToS3DocumentIdSlot), documentBytes) } just runs
    every { documentRepository.save(capture(savedDocumentSlot)) } returns document

    val result = underTest.scanAndStoreDocument(recallId, documentBytes, documentCategory, fileName)

    assertThat(result.get(), equalTo(uploadedToS3DocumentIdSlot.captured))
    assertThat(
      savedDocumentSlot.captured,
      equalTo(
        Document(
          uploadedToS3DocumentIdSlot.captured,
          recallId,
          documentCategory,
          fileName,
          1,
          OffsetDateTime.now(fixedClock)
        )
      )
    )

    verify { documentRepository.save(any()) }
  }

  @Test
  fun `uploading a versioned document with a virus does not add to S3 or add to the recall`() {
    val expectedVirusScanResult = VirusFound(emptyMap())
    every { recallRepository.getByRecallId(recallId) } returns aRecallWithoutDocuments
    every { virusScanner.scan(documentBytes) } returns expectedVirusScanResult

    val result = underTest.scanAndStoreDocument(recallId, documentBytes, documentCategory, fileName)

    assertThat(result, equalTo(Failure(expectedVirusScanResult)))

    verify(exactly = 0) { s3Service.uploadFile(any(), documentBytes) }
    verify { documentRepository wasNot Called }
  }

  @Test
  fun `add a versioned category document that is already present for the recall creates a new document in both S3 and the repository and increments the version`() {
    val uploadedToS3DocumentIdSlot = slot<DocumentId>()
    val savedDocumentSlot = slot<Document>()
    val document = mockk<Document>()

    val existingDocumentId = ::DocumentId.random()
    val existingDocument = Document(
      existingDocumentId,
      recallId,
      documentCategory,
      fileName,
      1,
      OffsetDateTime.now(fixedClock)
    )
    val aRecallWithDocument = aRecallWithoutDocuments.copy(
      documents = setOf(existingDocument)
    )
    val newFileName = randomString()

    every { recallRepository.getByRecallId(recallId) } returns aRecallWithDocument
    every { documentRepository.findLatestVersionedDocumentByRecallIdAndCategory(recallId, documentCategory) } returns existingDocument
    every { s3Service.uploadFile(capture(uploadedToS3DocumentIdSlot), documentBytes) } just runs
    every { documentRepository.save(capture(savedDocumentSlot)) } returns document

    val actualDocumentId = underTest.storeDocument(recallId, documentBytes, documentCategory, newFileName)

    assertThat(uploadedToS3DocumentIdSlot.captured, !equalTo(existingDocumentId))
    assertThat(actualDocumentId, !equalTo(existingDocumentId))
    assertThat(
      savedDocumentSlot.captured,
      equalTo(
        Document(
          uploadedToS3DocumentIdSlot.captured,
          recallId,
          documentCategory,
          newFileName,
          2,
          OffsetDateTime.now(fixedClock)
        )
      )
    )
  }

  @Test
  fun `gets a versioned document by recall ID and document ID`() {
    val aDocumentId = ::DocumentId.random()
    val aDocument = Document(
      aDocumentId,
      recallId,
      PART_A_RECALL_REPORT,
      randomString(),
      1,
      OffsetDateTime.now()
    )
    val aRecallWithDocument = aRecallWithoutDocuments.copy(documents = setOf(aDocument))
    val fileBytes = randomString().toByteArray()

    every { recallRepository.getByRecallId(recallId) } returns aRecallWithDocument
    every { documentRepository.getByRecallIdAndDocumentId(recallId, aDocumentId) } returns aDocument
    every { s3Service.downloadFile(aDocumentId) } returns fileBytes

    val (actualDocument, actualBytes) = underTest.getDocument(recallId, aDocumentId)

    assertThat(actualDocument, equalTo(aDocument))
    assertThat(actualBytes, equalTo(fileBytes))
  }

  @Test
  fun `gets a versioned document by recall ID and document category`() {
    val aDocumentCategory = randomVersionedDocumentCategory()
    val documentId = ::DocumentId.random()
    val aDocument = Document(
      documentId,
      recallId,
      aDocumentCategory,
      randomString(),
      1,
      OffsetDateTime.now()
    )
    val aRecallWithDocument = aRecallWithoutDocuments.copy(documents = setOf(aDocument))
    val fileBytes = randomString().toByteArray()

    every { recallRepository.getByRecallId(recallId) } returns aRecallWithDocument
    every { documentRepository.findLatestVersionedDocumentByRecallIdAndCategory(recallId, aDocumentCategory) } returns aDocument
    every { s3Service.downloadFile(documentId) } returns fileBytes

    val actualBytes = underTest.getLatestVersionedDocumentContentWithCategory(recallId, aDocumentCategory)

    assertThat(actualBytes, equalTo(fileBytes))
  }

  @Test
  fun `gets null if a document by recall ID and document category doesnt exist`() {
    val aDocumentCategory = randomVersionedDocumentCategory()

    every { recallRepository.getByRecallId(recallId) } returns aRecallWithoutDocuments
    every { documentRepository.findLatestVersionedDocumentByRecallIdAndCategory(recallId, aDocumentCategory) } returns null

    val result = underTest.getLatestVersionedDocumentContentWithCategoryIfExists(recallId, aDocumentCategory)

    verify(exactly = 0) { s3Service.uploadFile(any(), any()) }
    assertThat(result, equalTo(null))
  }

  @Test
  fun `getDocumentContentWithCategory throws a custom 'document with category not found' error if no document of category is found for recall`() {
    val randomDocumentCategory = randomVersionedDocumentCategory()
    every { recallRepository.getByRecallId(recallId) } returns aRecallWithoutDocuments
    every { documentRepository.findLatestVersionedDocumentByRecallIdAndCategory(recallId, randomDocumentCategory) } returns null

    assertThrows<RecallDocumentWithCategoryNotFoundException> {
      underTest.getLatestVersionedDocumentContentWithCategory(recallId, randomDocumentCategory)
    }
  }

  @Test
  fun `getDocumentContentWithCategory ignores all but one document matching recall ID and document category`() {
    val theDocumentCategory = randomVersionedDocumentCategory()
    val documentOne = Document(
      UUID.randomUUID(),
      recallId.value,
      theDocumentCategory,
      randomString(),
      1,
      OffsetDateTime.now()
    )
    val documentTwo = Document(
      UUID.randomUUID(),
      recallId.value,
      theDocumentCategory,
      randomString(),
      2,
      OffsetDateTime.now()
    )
    val aRecallWithDocument = aRecallWithoutDocuments.copy(documents = setOf(documentOne, documentTwo))
    val fileBytes = randomString().toByteArray()

    every { recallRepository.getByRecallId(recallId) } returns aRecallWithDocument
    every {
      documentRepository.findLatestVersionedDocumentByRecallIdAndCategory(recallId, theDocumentCategory)
    } returns documentOne
    every { s3Service.downloadFile(any()) } returns fileBytes

    val actualBytes = underTest.getLatestVersionedDocumentContentWithCategory(recallId, theDocumentCategory)

    assertThat(actualBytes, equalTo(fileBytes))
  }

  @Test
  fun `stores a document of type OTHER in document repository`() {
    val documentBytes = randomString().toByteArray()
    val documentId = ::DocumentId.random()
    val savedDocumentSlot = slot<Document>()

    every { recallRepository.getByRecallId(recallId) } returns aRecallWithoutDocuments
    every { s3Service.uploadFile(any(), documentBytes) } just runs
    every { documentRepository.save(capture(savedDocumentSlot)) } returns Document(
      documentId,
      recallId,
      OTHER,
      "filename.txt",
      null,
      OffsetDateTime.now()
    )

    val result = underTest.storeDocument(recallId, documentBytes, OTHER, "filename.txt")

    verify(exactly = 1) { documentRepository.save(any()) }

    assertThat(result, equalTo(savedDocumentSlot.captured.id()))
  }

  @Test
  fun `update a document category for a document that doesnt exist throws not found`() {
    val documentId = ::DocumentId.random()
    every { recallRepository.getByRecallId(recallId) } returns aRecallWithoutDocuments
    every {
      documentRepository.getByRecallIdAndDocumentId(
        recallId,
        documentId
      )
    } throws DocumentNotFoundException(recallId, documentId)

    assertThrows<DocumentNotFoundException> {
      underTest.updateDocumentCategory(recallId, documentId, randomVersionedDocumentCategory())
    }
  }

  @Test
  fun `update a document category for a versioned category to another versioned category`() {
    val documentId = ::DocumentId.random()
    val now = OffsetDateTime.now()
    val document = Document(documentId, recallId, PART_A_RECALL_REPORT, "parta.pdf", 1, now)
    val updatedCategory = LICENCE
    val updatedVersionDocument = document.copy(category = updatedCategory)
    val recallWithDoc = aRecallWithoutDocuments.copy(documents = setOf(document))

    every { recallRepository.getByRecallId(recallId) } returns recallWithDoc
    every { documentRepository.getByRecallIdAndDocumentId(recallId, documentId) } returns document
    every { documentRepository.save(updatedVersionDocument) } returns updatedVersionDocument

    val result = underTest.updateDocumentCategory(recallId, documentId, updatedCategory)

    assertThat(result, equalTo(updatedVersionDocument))

    verify { documentRepository.save(updatedVersionDocument) }
  }

  @Test
  fun `update a document category for an unversioned category to another unversioned category`() {
    val documentId = ::DocumentId.random()
    val now = OffsetDateTime.now()
    val document = Document(documentId, recallId, OTHER, "my-document.pdf", null, now)
    val updatedCategory = UNCATEGORISED
    val updatedDocument = document.copy(category = updatedCategory)
    val recallWithDoc = aRecallWithoutDocuments.copy(documents = setOf(document))

    every { recallRepository.getByRecallId(recallId) } returns recallWithDoc
    every { documentRepository.getByRecallIdAndDocumentId(recallId, documentId) } returns document
    every { documentRepository.save(updatedDocument) } returns updatedDocument

    val result = underTest.updateDocumentCategory(recallId, documentId, updatedCategory)

    assertThat(result, equalTo(updatedDocument))

    verify { documentRepository.save(updatedDocument) }
  }

  @Test
  fun `update a document category for a versioned category to an unversioned category clears version to null`() {
    val documentId = ::DocumentId.random()
    val now = OffsetDateTime.now()
    val originalDocument = Document(documentId, recallId, PART_A_RECALL_REPORT, "part-a.pdf", 1, now)
    val updatedCategory = UNCATEGORISED
    val updatedDocument = originalDocument.copy(category = updatedCategory, version = null)
    val recallWithDoc = aRecallWithoutDocuments.copy(documents = setOf(originalDocument))

    every { recallRepository.getByRecallId(recallId) } returns recallWithDoc
    every { documentRepository.getByRecallIdAndDocumentId(recallId, documentId) } returns originalDocument
    every { documentRepository.save(updatedDocument) } returns updatedDocument

    val result = underTest.updateDocumentCategory(recallId, documentId, updatedCategory)

    assertThat(result, equalTo(updatedDocument))

    verify(exactly = 0) { documentRepository.delete(originalDocument) }
    verify { documentRepository.save(updatedDocument) }
  }

  @Test
  fun `update a document category for an unversioned category to a versioned category sets version to 1`() {
    val documentId = ::DocumentId.random()
    val now = OffsetDateTime.now()
    val originalDocument = Document(documentId, recallId, UNCATEGORISED, "license.pdf", null, now)
    val updatedCategory = LICENCE
    val updatedDocument = originalDocument.copy(category = updatedCategory, version = 1)
    val recallWithDoc = aRecallWithoutDocuments.copy(documents = setOf(originalDocument))

    every { recallRepository.getByRecallId(recallId) } returns recallWithDoc
    every { documentRepository.getByRecallIdAndDocumentId(recallId, documentId) } returns originalDocument
    every { documentRepository.save(updatedDocument) } returns updatedDocument

    val result = underTest.updateDocumentCategory(recallId, documentId, updatedCategory)

    assertThat(result, equalTo(updatedDocument))

    verify(exactly = 0) { documentRepository.delete(originalDocument) }
    verify { documentRepository.save(updatedDocument) }
  }

  @Test
  fun `can delete an uploaded document for a Recall with status null`() {
    val documentId = ::DocumentId.random()
    val now = OffsetDateTime.now()
    val document = Document(documentId, recallId, UNCATEGORISED, "license.pdf", null, now)
    val recallWithDoc = aRecallWithoutDocuments.copy(documents = setOf(document))

    every { recallRepository.getByRecallId(recallId) } returns recallWithDoc
    every { documentRepository.getByRecallIdAndDocumentId(recallId, documentId) } returns document
    every { documentRepository.deleteByDocumentId(documentId) } just Runs

    underTest.deleteDocument(recallId, documentId)

    verify { documentRepository.deleteByDocumentId(documentId) }
  }

  @Test
  fun `fails to delete an uploaded document for a Recall with status BOOKED_ON`() {
    val documentId = ::DocumentId.random()
    val now = OffsetDateTime.now()
    val document = Document(documentId, recallId, PART_A_RECALL_REPORT, "license.pdf", 1, now)
    val recallWithDoc = aRecallWithoutDocuments.copy(documents = setOf(document), bookedByUserId = ::UserId.random().value)

    every { recallRepository.getByRecallId(recallId) } returns recallWithDoc
    every { documentRepository.getByRecallIdAndDocumentId(recallId, documentId) } returns document

    assertThrows<DocumentDeleteException> {
      underTest.deleteDocument(recallId, documentId)
    }

    verify(exactly = 0) { documentRepository.deleteByDocumentId(documentId) }
  }

  @Test
  fun `fails to delete a generated document for a Recall with status null`() {
    val documentId = ::DocumentId.random()
    val now = OffsetDateTime.now()
    val document = Document(documentId, recallId, REVOCATION_ORDER, "revo.pdf", 1, now)
    val recallWithDoc = aRecallWithoutDocuments.copy(documents = setOf(document), bookedByUserId = ::UserId.random().value)

    every { recallRepository.getByRecallId(recallId) } returns recallWithDoc
    every { documentRepository.getByRecallIdAndDocumentId(recallId, documentId) } returns document

    assertThrows<DocumentDeleteException> {
      underTest.deleteDocument(recallId, documentId)
    }

    verify(exactly = 0) { documentRepository.deleteByDocumentId(documentId) }
  }
}
