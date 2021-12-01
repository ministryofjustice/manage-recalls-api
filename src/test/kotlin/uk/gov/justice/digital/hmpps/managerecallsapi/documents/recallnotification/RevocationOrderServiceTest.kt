package uk.gov.justice.digital.hmpps.managerecallsapi.documents.recallnotification

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.REVOCATION_ORDER
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ImageData.Companion.recallImage
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ImageData.Companion.signature
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallImage.RevocationOrderLogo
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FullName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomString
import uk.gov.justice.digital.hmpps.managerecallsapi.service.DocumentService
import java.time.LocalDate

@Suppress("ReactiveStreamsUnusedPublisher")
internal class RevocationOrderServiceTest {

  private val pdfDocumentGenerationService = mockk<PdfDocumentGenerationService>()
  private val documentService = mockk<DocumentService>()
  private val revocationOrderGenerator = mockk<RevocationOrderGenerator>()

  private val underTest = RevocationOrderService(
    pdfDocumentGenerationService,
    documentService,
    revocationOrderGenerator
  )

  private val recallId = ::RecallId.random()
  private val expectedBytes = randomString().toByteArray()

  @Test
  fun `creates a revocation order for a recall `() {
    val createdByUserId = ::UserId.random()
    val userSignature = "base64EncodedUserSignature"
    val revocationOrderContext =
      RevocationOrderContext(
        recallId,
        FullName("Bertie Badger"),
        LocalDate.of(1995, 10, 3),
        "bookNumber",
        "croNumber",
        LocalDate.of(2017, 8, 29),
        LocalDate.of(2020, 9, 1),
        userSignature
      )
    val recallNotificationContext = mockk<RecallNotificationContext>()

    every { recallNotificationContext.getRevocationOrderContext() } returns revocationOrderContext
    val generatedHtml = "Some html, honest"
    every { revocationOrderGenerator.generateHtml(revocationOrderContext) } returns generatedHtml
    every {
      pdfDocumentGenerationService.generatePdf(
        generatedHtml,
        recallImage(RevocationOrderLogo),
        signature(userSignature)
      )
    } returns Mono.just(expectedBytes)
    every {
      documentService.storeDocument(recallId, createdByUserId, expectedBytes, REVOCATION_ORDER, "$REVOCATION_ORDER.pdf")
    } returns ::DocumentId.random()

    val result = underTest.createPdf(recallNotificationContext, createdByUserId)

    StepVerifier
      .create(result)
      .assertNext {
        assertThat(it, equalTo(expectedBytes))
        verify {
          documentService.storeDocument(
            recallId,
            createdByUserId,
            expectedBytes,
            REVOCATION_ORDER,
            "$REVOCATION_ORDER.pdf"
          )
        }
      }.verifyComplete()
  }

  @Test
  fun `gets existing revocation order for a recall`() {

    every { documentService.getLatestVersionedDocumentContentWithCategory(recallId, REVOCATION_ORDER) } returns expectedBytes

    val result = underTest.getPdf(recallId)

    StepVerifier
      .create(result)
      .assertNext {
        assertThat(it, equalTo(expectedBytes))
        verify { pdfDocumentGenerationService wasNot Called }
        verify { revocationOrderGenerator wasNot Called }
        verify { documentService.getLatestVersionedDocumentContentWithCategory(recallId, REVOCATION_ORDER) }
      }
      .verifyComplete()
  }
}
