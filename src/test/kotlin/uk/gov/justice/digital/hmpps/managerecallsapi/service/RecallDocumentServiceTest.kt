package uk.gov.justice.digital.hmpps.managerecallsapi.service

import com.natpryce.hamkrest.allElements
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.managerecallsapi.component.randomDocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.component.randomNoms
import uk.gov.justice.digital.hmpps.managerecallsapi.component.randomString
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocument
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory
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
  private val aRecall = Recall(
    id = recallId.value,
    nomsNumber = randomNoms()
  )

  @Test
  fun `throws a custom 'document not found' error if document with id is not found in recall`() {
    val theDocumentId = UUID.randomUUID()
    val otherDocumentId = UUID.randomUUID()
    val otherDocument = RecallDocument(
      id = otherDocumentId,
      recallId = recallId.value,
      category = randomDocumentCategory(),
      fileName = randomString()
    )
    val aRecallWithAnotherDocument = aRecall.copy(documents = setOf(otherDocument))
    every { recallRepository.getByRecallId(recallId) } returns aRecallWithAnotherDocument

    assertThrows<RecallDocumentNotFoundException> {
      underTest.getDocument(recallId, theDocumentId)
    }
  }

  @Test
  fun `uploads a document to S3 and adds it to persisted recall`() {
    val documentCategory = RecallDocumentCategory.PART_A_RECALL_REPORT
    val fileName = randomString()

    every { recallRepository.existsById(recallId.value) } returns true
    every { recallRepository.getByRecallId(recallId) } returns aRecall

    val documentId = UUID.randomUUID()
    every { s3Service.uploadFile(documentBytes) } returns documentId

    every { recallRepository.save(any()) } returns Recall(recallId.value, NomsNumber("A12345B"))

    val actualDocumentId = underTest.addDocumentToRecall(recallId, documentBytes, documentCategory, fileName)

    assertThat(actualDocumentId, equalTo(documentId))

    verify { s3Service.uploadFile(documentBytes) }

    verify {
      recallRepository.save(
        withArg { recall ->
          assertThat(
            recall.documents,
            allElements(equalTo(RecallDocument(documentId, recallId.value, documentCategory, fileName)))
          )
        }
      )
    }
  }

  @Test
  fun `add document to recall throws NotFoundException if recall does not exist`() {
    every { recallRepository.existsById(recallId.value) } returns false

    assertThrows<RecallNotFoundException> {
      underTest.addDocumentToRecall(
        recallId,
        documentBytes,
        RecallDocumentCategory.PART_A_RECALL_REPORT,
        randomString()
      )
    }

    verify(exactly = 0) { s3Service.uploadFile(any()) }
    verify(exactly = 0) { recallRepository.save(any()) }
  }

  @Test
  fun `gets a document by recall ID and document ID`() {
    val aDocumentId = UUID.randomUUID()
    val aDocument = RecallDocument(
      id = aDocumentId,
      recallId = recallId.value,
      category = RecallDocumentCategory.PART_A_RECALL_REPORT,
      fileName = randomString()
    )
    val aRecallWithDocument = aRecall.copy(documents = setOf(aDocument))
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
    val aRecallWithDocument = aRecall.copy(documents = setOf(aDocument))
    val fileBytes = randomString().toByteArray()

    every { recallRepository.getByRecallId(recallId) } returns aRecallWithDocument
    every { s3Service.downloadFile(documentId) } returns fileBytes

    val (actualDocument, actualBytes) = underTest.getDocumentWithCategory(recallId, aDocumentCategory)

    assertThat(actualDocument, equalTo(aDocument))
    assertThat(actualBytes, equalTo(fileBytes))
  }

  @Test
  fun `throws a custom 'document with category not found' error if no document of category is found for recall`() {
    val aRecallWithoutDocuments = aRecall.copy(documents = emptySet())
    every { recallRepository.getByRecallId(recallId) } returns aRecallWithoutDocuments

    assertThrows<RecallDocumentWithCategoryNotFoundException> {
      underTest.getDocumentWithCategory(recallId, randomDocumentCategory())
    }
  }

  @Test
  fun `ignores all but one document matching recall ID and document category`() {
    val theDocumentCategory = randomDocumentCategory()
    val documentOne = RecallDocument(id = UUID.randomUUID(), recallId = recallId.value, category = theDocumentCategory, fileName = randomString())
    val documentTwo = RecallDocument(id = UUID.randomUUID(), recallId = recallId.value, category = theDocumentCategory, fileName = randomString())
    val aRecallWithDocument = aRecall.copy(documents = setOf(documentOne, documentTwo))
    val fileBytes = randomString().toByteArray()

    every { recallRepository.getByRecallId(recallId) } returns aRecallWithDocument
    every { s3Service.downloadFile(any()) } returns fileBytes

    val (actualDocument, actualBytes) = underTest.getDocumentWithCategory(recallId, theDocumentCategory)

    assertThat(actualDocument.recallId, equalTo(recallId.value))
    assertThat(actualBytes, equalTo(fileBytes))
  }
}
