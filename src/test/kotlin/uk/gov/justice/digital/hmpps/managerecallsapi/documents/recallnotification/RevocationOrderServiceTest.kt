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
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.REVOCATION_ORDER
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ImageData.Companion.recallImage
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ImageData.Companion.signature
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PersonName
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallImage.RevocationOrderLogo
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.MiddleNames
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomString
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallDocumentService
import java.time.LocalDate
import java.util.UUID

@Suppress("ReactiveStreamsUnusedPublisher")
internal class RevocationOrderServiceTest {

  private val pdfDocumentGenerationService = mockk<PdfDocumentGenerationService>()
  private val recallDocumentService = mockk<RecallDocumentService>()
  private val revocationOrderGenerator = mockk<RevocationOrderGenerator>()

  private val underTest = RevocationOrderService(
    pdfDocumentGenerationService,
    recallDocumentService,
    revocationOrderGenerator
  )

  private val recallId = ::RecallId.random()
  private val expectedBytes = randomString().toByteArray()

  @Test
  fun `creates a revocation order for a recall `() {
    val userSignature = "base64EncodedUserSignature"
    val revocationOrderContext =
      RevocationOrderContext(
        recallId,
        PersonName(FirstName("Bertie"), MiddleNames("Basset"), LastName("Basset")),
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
      recallDocumentService.uploadAndAddDocumentForRecall(recallId, expectedBytes, REVOCATION_ORDER)
    } returns UUID.randomUUID()

    val result = underTest.createPdf(recallNotificationContext)

    StepVerifier
      .create(result)
      .assertNext {
        assertThat(it, equalTo(expectedBytes))
        verify { recallDocumentService.uploadAndAddDocumentForRecall(recallId, expectedBytes, REVOCATION_ORDER) }
      }.verifyComplete()
  }

  @Test
  fun `gets existing revocation order for a recall`() {

    every { recallDocumentService.getDocumentContentWithCategory(recallId, REVOCATION_ORDER) } returns expectedBytes

    val result = underTest.getPdf(recallId)

    StepVerifier
      .create(result)
      .assertNext {
        assertThat(it, equalTo(expectedBytes))
        verify { pdfDocumentGenerationService wasNot Called }
        verify { revocationOrderGenerator wasNot Called }
        verify { recallDocumentService.getDocumentContentWithCategory(recallId, REVOCATION_ORDER) }
      }
      .verifyComplete()
  }
}
