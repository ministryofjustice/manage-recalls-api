package uk.gov.justice.digital.hmpps.managerecallsapi.service

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.nhaarman.mockitokotlin2.any
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocument
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.PART_A_RECALL_REPORT
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomDocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomString
import uk.gov.justice.digital.hmpps.managerecallsapi.storage.S3Service
import java.util.UUID

internal class RecallDocumentServiceTest {

  private val s3Service = mockk<S3Service>()
  private val recallRepository = mockk<RecallRepository>()
  private val recallDocumentRepository = mockk<RecallDocumentRepository>()

  private val underTest = RecallDocumentService(s3Service, recallRepository, recallDocumentRepository)

  private val recallId = ::RecallId.random()
  private val documentBytes = randomString().toByteArray()
  private val nomsNumber = NomsNumber("A1235B")
  private val aRecallWithoutDocuments = Recall(recallId, nomsNumber)
  private val documentCategory = PART_A_RECALL_REPORT
  private val fileName = randomString()

  @Test
  fun `can upload a document to S3 and adds it to the existing recall`() {
    val documentIdSlot = slot<UUID>()
    val savedDocumentSlot = slot<RecallDocument>()

    every { recallRepository.getByRecallId(recallId) } returns aRecallWithoutDocuments
    every { recallDocumentRepository.findByRecallIdAndCategory(recallId.value, any()) } returns null
    every { s3Service.uploadFile(capture(documentIdSlot), documentBytes) } just runs
    every { recallRepository.addDocumentToRecall(recallId, capture(savedDocumentSlot)) } just runs
    every { recallDocumentRepository.save(capture(savedDocumentSlot)) } returns any()

    val actualDocumentId = underTest.uploadAndAddDocumentForRecall(recallId, documentBytes, documentCategory, fileName)

    val uploadedDocumentId = documentIdSlot.captured
    assertThat(actualDocumentId, equalTo(uploadedDocumentId))
    assertThat(
      savedDocumentSlot.captured,
      equalTo(RecallDocument(uploadedDocumentId, recallId.value, documentCategory, fileName))
    )
  }

  @Test
  fun `add document with a category that already exists updates the existing document in both S3 and the repository`() {
    val existingDocumentId = UUID.randomUUID()
    val existingDocument = RecallDocument(existingDocumentId, recallId.value, documentCategory, fileName)
    val aRecallWithDocument = Recall(recallId, nomsNumber, documents = setOf(existingDocument))
    val newFileName = randomString()
    val updatedDocument = RecallDocument(existingDocumentId, recallId.value, documentCategory, newFileName)

    every { recallRepository.getByRecallId(recallId) } returns aRecallWithDocument
    every { recallDocumentRepository.findByRecallIdAndCategory(recallId.value, any()) } returns existingDocument
    every { s3Service.uploadFile(existingDocumentId, documentBytes) } just runs
    every { recallDocumentRepository.save(updatedDocument) } returns updatedDocument

    val actualDocumentId = underTest.uploadAndAddDocumentForRecall(recallId, documentBytes, documentCategory, newFileName)

    assertThat(actualDocumentId, equalTo(existingDocumentId))
  }

  @Test
  fun `gets a document by recall ID and document ID`() {
    val aDocumentId = UUID.randomUUID()
    val aDocument = RecallDocument(
      id = aDocumentId,
      recallId = recallId.value,
      category = PART_A_RECALL_REPORT,
      fileName = randomString()
    )
    val aRecallWithDocument = aRecallWithoutDocuments.copy(documents = setOf(aDocument))
    val fileBytes = randomString().toByteArray()

    every { recallRepository.getByRecallId(recallId) } returns aRecallWithDocument
    every { recallDocumentRepository.getByRecallIdAndDocumentId(recallId, aDocumentId) } returns aDocument
    every { s3Service.downloadFile(aDocumentId) } returns fileBytes

    val (actualDocument, actualBytes) = underTest.getDocument(recallId, aDocumentId)

    assertThat(actualDocument, equalTo(aDocument))
    assertThat(actualBytes, equalTo(fileBytes))
  }

  @Test
  fun `gets a document by recall ID and document category`() {
    val aDocumentCategory = randomDocumentCategory()
    val documentId = UUID.randomUUID()
    val aDocument = RecallDocument(
      id = documentId,
      recallId = recallId.value,
      category = aDocumentCategory,
      fileName = randomString()
    )
    val aRecallWithDocument = aRecallWithoutDocuments.copy(documents = setOf(aDocument))
    val fileBytes = randomString().toByteArray()

    every { recallRepository.getByRecallId(recallId) } returns aRecallWithDocument
    every { recallDocumentRepository.findByRecallIdAndCategory(recallId.value, aDocumentCategory) } returns aDocument
    every { s3Service.downloadFile(documentId) } returns fileBytes

    val actualBytes = underTest.getDocumentContentWithCategory(recallId, aDocumentCategory)

    assertThat(actualBytes, equalTo(fileBytes))
  }

  @Test
  fun `gets null if a document by recall ID and document category doesnt exist`() {
    val aDocumentCategory = randomDocumentCategory()

    every { recallRepository.getByRecallId(recallId) } returns aRecallWithoutDocuments
    every { recallDocumentRepository.findByRecallIdAndCategory(recallId.value, aDocumentCategory) } returns null

    val result = underTest.getDocumentContentWithCategoryIfExists(recallId, aDocumentCategory)

    verify(exactly = 0) { s3Service.uploadFile(any(), any()) }
    assertThat(result, equalTo(null))
  }

  @Test
  fun `getDocumentContentWithCategory throws a custom 'document with category not found' error if no document of category is found for recall`() {
    val randomDocumentCategory = randomDocumentCategory()
    every { recallRepository.getByRecallId(recallId) } returns aRecallWithoutDocuments
    every { recallDocumentRepository.findByRecallIdAndCategory(recallId.value, randomDocumentCategory) } returns null

    assertThrows<RecallDocumentWithCategoryNotFoundException> {
      underTest.getDocumentContentWithCategory(recallId, randomDocumentCategory)
    }
  }

  @Test
  fun `getDocumentContentWithCategory ignores all but one document matching recall ID and document category`() {
    val theDocumentCategory = randomDocumentCategory()
    val documentOne = RecallDocument(UUID.randomUUID(), recallId.value, theDocumentCategory, randomString())
    val documentTwo = RecallDocument(UUID.randomUUID(), recallId.value, theDocumentCategory, randomString())
    val aRecallWithDocument = aRecallWithoutDocuments.copy(documents = setOf(documentOne, documentTwo))
    val fileBytes = randomString().toByteArray()

    every { recallRepository.getByRecallId(recallId) } returns aRecallWithDocument
    every { recallDocumentRepository.findByRecallIdAndCategory(recallId.value, theDocumentCategory) } returns documentOne
    every { s3Service.downloadFile(any()) } returns fileBytes

    val actualBytes = underTest.getDocumentContentWithCategory(recallId, theDocumentCategory)

    assertThat(actualBytes, equalTo(fileBytes))
  }
}
