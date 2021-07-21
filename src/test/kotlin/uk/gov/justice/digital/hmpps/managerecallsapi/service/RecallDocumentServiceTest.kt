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
import javax.persistence.EntityNotFoundException

internal class RecallDocumentServiceTest {

  private val s3Service = mockk<S3Service>()
  private val recallRepository = mockk<RecallRepository>()

  private val underTest = RecallDocumentService(s3Service, recallRepository)

  private val recallId = ::RecallId.random()
  private val documentBytes = "a document".toByteArray()

  @Test
  fun `throws 'recall not found' error if recall is missing`() {
    every { recallRepository.getByRecallId(recallId) } throws EntityNotFoundException("boom!")

    assertThrows<RecallNotFoundError> {
      underTest.addDocumentToRecall(recallId, documentBytes, RecallDocumentCategory.PART_A_RECALL_REPORT)
    }
  }

  @Test
  fun `uploads a document to S3 and adds it to persisted recall`() {
    val documentCategory = RecallDocumentCategory.PART_A_RECALL_REPORT
    val aRecall = Recall(
      id = recallId.value,
      nomsNumber = NomsNumber("A1235B")
    )

    every { recallRepository.getByRecallId(recallId) } returns aRecall

    val fileS3Key = UUID.randomUUID()
    every { s3Service.uploadFile(documentBytes) } returns fileS3Key

    every { recallRepository.save(any()) } returns Recall(recallId.value, NomsNumber("A12345B"))

    underTest.addDocumentToRecall(recallId, documentBytes, documentCategory)

    verify { s3Service.uploadFile(documentBytes) }

    verify {
      recallRepository.save(
        withArg { recall ->
          assertThat(recall.documents, allElements(equalTo(RecallDocument(fileS3Key, recallId.value, documentCategory))))
        }
      )
    }
  }
}
