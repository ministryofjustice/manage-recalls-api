package uk.gov.justice.digital.hmpps.managerecallsapi.service

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.REVOCATION_ORDER
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ImageData.Companion.recallImage
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ImageData.Companion.signature
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Email
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhoneNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomNoms
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomString
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallImage.RevocationOrderLogo
import java.io.File
import java.util.Base64
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
  private val nomsNumber = randomNoms()

  @Test
  fun `creates a revocation order for a recall `() {
    val recall = Recall(recallId, nomsNumber)
    val prisoner = mockk<Prisoner>()
    val userId = UserId(UUID.randomUUID())
    val userSignature = Base64.getEncoder().encodeToString(File("src/test/resources/signature.jpg").readBytes())

    val userDetails = UserDetails(
      userId,
      FirstName("Bob"),
      LastName("Badger"),
      userSignature,
      Email("bertie@badger.org"),
      PhoneNumber("01234567890")
    )
    val generatedHtml = "Some html, honest"
    every { revocationOrderGenerator.generateHtml(prisoner, recall) } returns generatedHtml
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

    val recallNotificationContext =
      RecallNotificationContext(recall, prisoner, userDetails, PrisonName("currentPrisonName"), PrisonName("lastReleasePrisonName"))
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
