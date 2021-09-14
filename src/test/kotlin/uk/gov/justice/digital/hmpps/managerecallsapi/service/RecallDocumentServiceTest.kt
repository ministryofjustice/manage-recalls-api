package uk.gov.justice.digital.hmpps.managerecallsapi.service

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.managerecallsapi.component.randomDocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.component.randomString
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocument
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.PART_A_RECALL_REPORT
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.storage.S3Service
import java.util.UUID

internal class RecallDocumentServiceTest {

  private val s3Service = mockk<S3Service>()
  private val recallRepository = mockk<RecallRepository>()

  private val underTest = RecallDocumentService(s3Service, recallRepository)

  private val recallId = ::RecallId.random()
  private val documentBytes = randomString().toByteArray()
  private val nomsNumber = NomsNumber("A1235B")
  private val aRecallWithoutDocuments = Recall(recallId, nomsNumber)
  private val documentCategory = PART_A_RECALL_REPORT
  private val fileName = randomString()

  @Test
  fun `getDocument throws a custom 'document not found' error if document with id is not found in recall`() {
    val theDocumentId = UUID.randomUUID()
    val otherDocumentId = UUID.randomUUID()
    val otherDocument = RecallDocument(
      id = otherDocumentId,
      recallId = recallId.value,
      category = randomDocumentCategory(),
      fileName = randomString()
    )
    val aRecallWithAnotherDocument = aRecallWithoutDocuments.copy(documents = setOf(otherDocument))
    every { recallRepository.getByRecallId(recallId) } returns aRecallWithAnotherDocument

    assertThrows<RecallDocumentNotFoundException> {
      underTest.getDocument(recallId, theDocumentId)
    }
  }

  @Test
  fun `addDocumentToRecall uploads the document to S3 and adds it to the existing recall`() {
    val documentIdSlot = slot<UUID>()
    val savedDocumentSlot = slot<RecallDocument>()

    every { recallRepository.findByRecallId(recallId) } returns aRecallWithoutDocuments
    every { s3Service.uploadFile(capture(documentIdSlot), documentBytes) } just runs
    every { recallRepository.addDocumentToRecall(recallId, capture(savedDocumentSlot)) } just runs

    val actualDocumentId = underTest.addDocumentToRecall(recallId, documentBytes, documentCategory, fileName)

    val newDocumentId = documentIdSlot.captured
    assertThat(actualDocumentId, equalTo(newDocumentId))
    assertThat(
      savedDocumentSlot.captured,
      equalTo(RecallDocument(newDocumentId, recallId.value, documentCategory, fileName))
    )
  }

  @Test
  fun `addDocumentToRecall with a category that already exists updates the existing document in both S3 and the repository`() {
    val savedDocumentSlot = slot<RecallDocument>()
    val existingDocumentId = UUID.randomUUID()
    val aRecallWithDocument = Recall(recallId, nomsNumber, documents = setOf(RecallDocument(existingDocumentId, recallId.value, documentCategory, fileName)))
    val newFileName = randomString()

    every { recallRepository.findByRecallId(recallId) } returns aRecallWithDocument
    every { s3Service.uploadFile(existingDocumentId, documentBytes) } just runs
    every { recallRepository.addDocumentToRecall(recallId, capture(savedDocumentSlot)) } just runs

    val actualDocumentId = underTest.addDocumentToRecall(recallId, documentBytes, documentCategory, newFileName)

    assertThat(actualDocumentId, equalTo(existingDocumentId))
    assertThat(
      savedDocumentSlot.captured,
      equalTo(RecallDocument(existingDocumentId, recallId.value, documentCategory, newFileName))
    )
  }

  @Test
  fun `add document to recall throws NotFoundException if recall does not exist`() {
    every { recallRepository.findByRecallId(recallId) } returns null

    assertThrows<RecallNotFoundException> {
      underTest.addDocumentToRecall(recallId, documentBytes, PART_A_RECALL_REPORT, randomString())
    }

    verify(exactly = 0) { s3Service.uploadFile(any(), any()) }
    verify(exactly = 0) { recallRepository.save(any()) }
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
    every { s3Service.downloadFile(documentId) } returns fileBytes

    val actualBytes = underTest.getDocumentContentWithCategory(recallId, aDocumentCategory)

    assertThat(actualBytes, equalTo(fileBytes))
  }

  @Test
  fun `getDocumentContentWithCategory throws a custom 'document with category not found' error if no document of category is found for recall`() {
    every { recallRepository.getByRecallId(recallId) } returns aRecallWithoutDocuments

    assertThrows<RecallDocumentWithCategoryNotFoundException> {
      underTest.getDocumentContentWithCategory(recallId, randomDocumentCategory())
    }
  }

  @Test
  fun `getDocumentContentWithCategory ignores all but one document matching recall ID and document category`() {
    val theDocumentCategory = randomDocumentCategory()
    val documentOne = RecallDocument(id = UUID.randomUUID(), recallId = recallId.value, category = theDocumentCategory, fileName = randomString())
    val documentTwo = RecallDocument(id = UUID.randomUUID(), recallId = recallId.value, category = theDocumentCategory, fileName = randomString())
    val aRecallWithDocument = aRecallWithoutDocuments.copy(documents = setOf(documentOne, documentTwo))
    val fileBytes = randomString().toByteArray()

    every { recallRepository.getByRecallId(recallId) } returns aRecallWithDocument
    every { s3Service.downloadFile(any()) } returns fileBytes

    val actualBytes = underTest.getDocumentContentWithCategory(recallId, theDocumentCategory)

    assertThat(actualBytes, equalTo(fileBytes))
  }
}
