package uk.gov.justice.digital.hmpps.managerecallsapi.service

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.thymeleaf.spring5.SpringTemplateEngine
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
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

  private val underTest = RevocationOrderService(pdfDocumentGenerator, prisonerOffenderSearchClient, thymeleafConfig, s3Service)

  @Test
  fun `generates a revocation order using the pdf generator`() {
    val expectedPdf = "Some pdf".toByteArray()

    underTest.bucketName = "a-bucket"

    every { prisonerOffenderSearchClient.prisonerSearch(any()) } returns Mono.just(listOf(Prisoner()))
    every { pdfDocumentGenerator.makePdf(any()) } returns Mono.just(expectedPdf)
    every { thymeleafConfig.process("revocation-order", any()) } returns "Some html, honest"
    every { s3Service.uploadFile(any(), any(), any()) } returns S3BulkResponseEntity(
      "a-bucket",
      UUID.randomUUID(),
      "myFile.pdf",
      true,
      200
    )

    val result = underTest.generateRevocationOrder("My Noms Number")

    StepVerifier
      .create(result)
      .assertNext {
        assertThat(it, equalTo(expectedPdf))
      }
      .verifyComplete()
  }
}
