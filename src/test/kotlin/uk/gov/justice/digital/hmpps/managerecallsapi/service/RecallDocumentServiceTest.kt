package uk.gov.justice.digital.hmpps.managerecallsapi.service

import com.natpryce.hamkrest.allElements
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
  private val documentBytes = "a document".toByteArray()
  private val aRecall = Recall(
    id = recallId.value,
    nomsNumber = NomsNumber("A1235B")
  )

  @Test
  fun `throws 'document not found' error if document is not found in recall`() {
    val aRecallWithoutDocuments = aRecall.copy(documents = emptySet())
    every { recallRepository.getByRecallId(recallId) } returns aRecallWithoutDocuments

    assertThrows<RecallDocumentNotFoundException> {
      underTest.getDocument(recallId, UUID.randomUUID())
    }
  }

  @Test
  fun `uploads a document to S3 and adds it to persisted recall`() {
    val documentCategory = RecallDocumentCategory.PART_A_RECALL_REPORT

    every { recallRepository.getByRecallId(recallId) } returns aRecall

    val documentId = UUID.randomUUID()
    every { s3Service.uploadFile(documentBytes) } returns documentId

    every { recallRepository.save(any()) } returns Recall(recallId.value, NomsNumber("A12345B"))

    val actualDocumentId = underTest.addDocumentToRecall(recallId, documentBytes, documentCategory)

    assertThat(actualDocumentId, equalTo(documentId))

    verify { s3Service.uploadFile(documentBytes) }

    verify {
      recallRepository.save(
        withArg { recall ->
          assertThat(
            recall.documents,
            allElements(equalTo(RecallDocument(documentId, recallId.value, documentCategory)))
          )
        }
      )
    }
  }

  @Test
  fun `gets a document by recall ID and document ID`() {
    val aDocumentId = UUID.randomUUID()
    val aDocument = RecallDocument(
      id = aDocumentId,
      recallId = recallId.value,
      category = RecallDocumentCategory.PART_A_RECALL_REPORT
    )
    val aRecallWithDocument = aRecall.copy(documents = setOf(aDocument))
    val fileBytes = "Hello".toByteArray()

    every { recallRepository.getByRecallId(recallId) } returns aRecallWithDocument
    every { s3Service.downloadFile(aDocumentId) } returns fileBytes

    val (actualDocument, actualBytes) = underTest.getDocument(recallId, aDocumentId)

    assertThat(actualDocument, equalTo(aDocument))
    assertThat(actualBytes, equalTo(fileBytes))
  }
}
