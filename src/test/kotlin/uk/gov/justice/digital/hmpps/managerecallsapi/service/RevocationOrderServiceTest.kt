package uk.gov.justice.digital.hmpps.managerecallsapi.service

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.thymeleaf.spring5.SpringTemplateEngine
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerator
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerOffenderSearchClient
import uk.gov.justice.digital.hmpps.managerecallsapi.storage.S3BulkResponseEntity
import uk.gov.justice.digital.hmpps.managerecallsapi.storage.S3Service
import java.util.UUID

internal class RevocationOrderServiceTest {

  private val pdfDocumentGenerator = mockk<PdfDocumentGenerator>()
  private val prisonerOffenderSearchClient = mockk<PrisonerOffenderSearchClient>()
  private val thymeleafConfig = mockk<SpringTemplateEngine>()
  private val s3Service = mockk<S3Service>()
  private val recallRepository = mockk<RecallRepository>()

  private val underTest = RevocationOrderService(
    pdfDocumentGenerator,
    prisonerOffenderSearchClient,
    thymeleafConfig,
    s3Service,
    recallRepository
  )

  @Test
  fun `generates a revocation order for a Noms number`() {
    val expectedPdf = "Some pdf".toByteArray()

    underTest.bucketName = "a-bucket"

    every { prisonerOffenderSearchClient.prisonerSearch(any()) } returns Mono.just(listOf(Prisoner()))
    every { pdfDocumentGenerator.makePdf(any()) } returns Mono.just(expectedPdf)
    every { thymeleafConfig.process("revocation-order", any()) } returns "Some html, honest"

    val result = underTest.getRevocationOrder("My Noms Number")

    StepVerifier
      .create(result)
      .assertNext {
        assertThat(it, equalTo(expectedPdf))
      }
      .verifyComplete()
  }

  @Test
  fun `generates a revocation order for a recall without an existing revocation order`() {
    val recallId = UUID.randomUUID()
    val expectedBytes = "Some pdf".toByteArray()
    val s3Bucket = "a-bucket"

    underTest.bucketName = s3Bucket

    every { prisonerOffenderSearchClient.prisonerSearch(any()) } returns Mono.just(listOf(Prisoner()))
    every { pdfDocumentGenerator.makePdf(any()) } returns Mono.just(expectedBytes)
    every { thymeleafConfig.process("revocation-order", any()) } returns "Some html, honest"
    val aRecall = Recall(recallId, nomsNumber = "aNumber")
    val revocationOrderDocS3Key = UUID.randomUUID()
    val aRecallWithRevocationOrder =
      Recall(recallId, nomsNumber = "aNumber", revocationOrderDocS3Key = revocationOrderDocS3Key)
    every { recallRepository.getById(recallId) } returns aRecall
    every { recallRepository.save(aRecallWithRevocationOrder) } returns aRecallWithRevocationOrder
    every { s3Service.uploadFile(any(), any(), any()) } returns S3BulkResponseEntity(
      s3Bucket,
      revocationOrderDocS3Key,
      "myFile.pdf",
      true,
      200
    )

    val result = underTest.getRevocationOrder(recallId)

    StepVerifier
      .create(result)
      .assertNext {
        assertThat(it, equalTo(expectedBytes))
        verify { recallRepository.save(aRecallWithRevocationOrder) }
        verify { s3Service.uploadFile(s3Bucket, expectedBytes, "$recallId-revocation-order.pdf") }
      }
      .verifyComplete()
  }

  @Test
  fun `gets existing revocation order for a recall when one exists`() {
    val recallId = UUID.randomUUID()
    val revocationOrderDocS3Key = UUID.randomUUID()
    val expectedBytes = "Some pdf".toByteArray()
    val s3Bucket = "a-bucket"

    underTest.bucketName = s3Bucket

    val aRecall = Recall(recallId, nomsNumber = "aNumber", revocationOrderDocS3Key)
    val aRecallWithRevocationOrder =
      Recall(recallId, nomsNumber = "aNumber", revocationOrderDocS3Key = revocationOrderDocS3Key)
    every { recallRepository.getById(recallId) } returns aRecall
    every { recallRepository.save(aRecallWithRevocationOrder) } returns aRecallWithRevocationOrder
    every { s3Service.downloadFile(s3Bucket, revocationOrderDocS3Key) } returns expectedBytes

    val result = underTest.getRevocationOrder(recallId)

    StepVerifier
      .create(result)
      .assertNext {
        assertThat(it, equalTo(expectedBytes))
        verify { prisonerOffenderSearchClient wasNot Called }
        verify { pdfDocumentGenerator wasNot Called }
        verify { thymeleafConfig wasNot Called }
        verify(exactly = 0) { recallRepository.save(aRecallWithRevocationOrder) }
        verify(exactly = 0) { s3Service.uploadFile(s3Bucket, expectedBytes, "$recallId-revocation-order.pdf") }
        verify { recallRepository.getById(recallId) }
        verify { s3Service.downloadFile(s3Bucket, revocationOrderDocS3Key) }
      }
      .verifyComplete()
  }
}