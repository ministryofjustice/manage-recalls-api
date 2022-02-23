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
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CroNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FileName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FullName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomString
import uk.gov.justice.digital.hmpps.managerecallsapi.service.DocumentService
import java.time.LocalDate

@Suppress("ReactiveStreamsUnusedPublisher")
internal class RevocationOrderServiceTest {

  private val recallNotificationContextFactory = mockk<RecallNotificationContextFactory>()
  private val pdfDocumentGenerationService = mockk<PdfDocumentGenerationService>()
  private val documentService = mockk<DocumentService>()
  private val revocationOrderGenerator = mockk<RevocationOrderGenerator>()

  private val underTest = RevocationOrderService(
    recallNotificationContextFactory,
    pdfDocumentGenerationService,
    documentService,
    revocationOrderGenerator
  )

  private val recallId = ::RecallId.random()
  private val expectedBytes = randomString().toByteArray()
  private val fileName = FileName("REVOCATION_ORDER.pdf")

  @Test
  fun `getOrGeneratePdf generates a revocation order for a recall if one does not exist`() {
    val createdByUserId = ::UserId.random()
    val userSignature = "base64EncodedUserSignature"
    val revocationOrderContext =
      RevocationOrderContext(
        recallId,
        FullName("Bertie Badger"),
        LocalDate.of(1995, 10, 3),
        "bookNumber",
        CroNumber("croNumber"),
        LocalDate.of(2017, 8, 29),
        LocalDate.of(2020, 9, 1),
        userSignature,
        createdByUserId,
        "Badger Bertie"
      )

    val fileName = FileName("Badger Bertie bookNumber REVOCATION ORDER.pdf")

    val generatedHtml = "Some html, honest"

    every { documentService.getLatestVersionedDocumentContentWithCategoryIfExists(recallId, REVOCATION_ORDER) } returns null
    every { revocationOrderGenerator.generateHtml(revocationOrderContext) } returns generatedHtml
    every {
      pdfDocumentGenerationService.generatePdf(
        generatedHtml,
        recallImage(RevocationOrderLogo),
        signature(userSignature)
      )
    } returns Mono.just(expectedBytes)
    every {
      documentService.storeDocument(recallId, createdByUserId, expectedBytes, REVOCATION_ORDER, fileName)
    } returns ::DocumentId.random()

    val result = underTest.getOrGeneratePdf(revocationOrderContext)

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
            fileName
          )
        }
      }.verifyComplete()
  }

  @Test
  fun `generateAndStorePdf generates a new revocation order for a recall`() {
    val createdByUserId = ::UserId.random()
    val userSignature = "base64EncodedUserSignature"
    val revocationOrderContext =
      RevocationOrderContext(
        recallId,
        FullName("Bertie Badger"),
        LocalDate.of(1995, 10, 3),
        "bookNumber",
        CroNumber("croNumber"),
        LocalDate.of(2017, 8, 29),
        LocalDate.of(2020, 9, 1),
        userSignature,
        createdByUserId,
        "Badger Bertie"
      )

    val generatedHtml = "Some html, honest"
    val details = "Blah, Blah, Blah"
    val recallNotificationContext = mockk<RecallNotificationContext>()
    every { recallNotificationContextFactory.createContext(recallId, createdByUserId) } returns recallNotificationContext
    every { recallNotificationContext.getRevocationOrderContext() } returns revocationOrderContext
    every { revocationOrderGenerator.generateHtml(revocationOrderContext) } returns generatedHtml
    every {
      pdfDocumentGenerationService.generatePdf(
        generatedHtml,
        recallImage(RevocationOrderLogo),
        signature(userSignature)
      )
    } returns Mono.just(expectedBytes)
    val documentId = ::DocumentId.random()
    every {
      documentService.storeDocument(recallId, createdByUserId, expectedBytes, REVOCATION_ORDER, fileName, details)
    } returns documentId

    val result = underTest.generateAndStorePdf(recallId, createdByUserId, fileName, details)

    StepVerifier
      .create(result)
      .assertNext {
        assertThat(it, equalTo(documentId))
        verify {
          documentService.storeDocument(
            recallId,
            createdByUserId,
            expectedBytes,
            REVOCATION_ORDER,
            fileName,
            details
          )
        }
      }.verifyComplete()

    verify(exactly = 0) { documentService.getLatestVersionedDocumentContentWithCategoryIfExists(recallId, REVOCATION_ORDER) }
  }

  @Test
  fun `getOrGeneratePdf gets existing revocation order if one exists for the recall`() {
    val createdByUserId = ::UserId.random()
    val userSignature = "base64EncodedUserSignature"
    val revocationOrderContext =
      RevocationOrderContext(
        recallId,
        FullName("Bertie Badger"),
        LocalDate.of(1995, 10, 3),
        "bookNumber",
        CroNumber("croNumber"),
        LocalDate.of(2017, 8, 29),
        LocalDate.of(2020, 9, 1),
        userSignature,
        createdByUserId,
        "Badger Bertie"
      )

    every { documentService.getLatestVersionedDocumentContentWithCategoryIfExists(recallId, REVOCATION_ORDER) } returns expectedBytes

    val result = underTest.getOrGeneratePdf(revocationOrderContext)

    StepVerifier
      .create(result)
      .assertNext {
        assertThat(it, equalTo(expectedBytes))
        verify { pdfDocumentGenerationService wasNot Called }
        verify { revocationOrderGenerator wasNot Called }
        verify { documentService.getLatestVersionedDocumentContentWithCategoryIfExists(recallId, REVOCATION_ORDER) }
      }
      .verifyComplete()
  }
}
