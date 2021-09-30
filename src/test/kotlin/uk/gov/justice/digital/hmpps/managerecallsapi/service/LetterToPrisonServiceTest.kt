package uk.gov.justice.digital.hmpps.managerecallsapi.service

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.LETTER_TO_PRISON
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ImageData.Companion.recallImage
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Email
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhoneNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomNoms
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomString
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerOffenderSearchClient
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallImage.RevocationOrderLogo
import java.io.File
import java.util.Base64
import java.util.UUID

@Suppress("ReactiveStreamsUnusedPublisher")
internal class LetterToPrisonServiceTest {

  private val pdfDocumentGenerationService = mockk<PdfDocumentGenerationService>()
  private val prisonerOffenderSearchClient = mockk<PrisonerOffenderSearchClient>()
  private val recallDocumentService = mockk<RecallDocumentService>()
  private val recallRepository = mockk<RecallRepository>()
  private val revocationOrderGenerator = mockk<RevocationOrderGenerator>()
  private val letterToPrisonGenerator = mockk<LetterToPrisonGenerator>()
  private val userDetailsService = mockk<UserDetailsService>()

  private val underTest = LetterToPrisonService(
    recallDocumentService,
    letterToPrisonGenerator,
    pdfDocumentGenerationService
  )

  // TODO: will be good to agree as the dev team whether we prefer this model or local vals per test fun
  private val recallId = ::RecallId.random()
  private val expectedBytes = randomString().toByteArray()
  private val nomsNumber = randomNoms()

  @Test
  fun `creates a revocation order for a recall `() {
    val aRecall = Recall(recallId, nomsNumber)
    val revocationOrderId = UUID.randomUUID()
    val userId = UserId(UUID.randomUUID())
    val userSignature = Base64.getEncoder().encodeToString(File("src/test/resources/signature.jpg").readBytes())

    every { recallRepository.getByRecallId(recallId) } returns aRecall
    val generatedHtml = "Some html, honest"
    every { userDetailsService.get(userId) } returns UserDetails(
      userId, FirstName("Bob"), LastName("Badger"), userSignature, Email("bertie@badger.org"), PhoneNumber("01234567890")
    )
    every { recallDocumentService.getDocumentContentWithCategoryIfExists(recallId, LETTER_TO_PRISON) } returns null
    every { letterToPrisonGenerator.generateHtml() } returns generatedHtml
    every {
      pdfDocumentGenerationService.generatePdf(
        generatedHtml,
        recallImage(RevocationOrderLogo)
      )
    } returns Mono.just(expectedBytes)
    every {
      recallDocumentService.uploadAndAddDocumentForRecall(recallId, expectedBytes, LETTER_TO_PRISON)
    } returns revocationOrderId

    val result = underTest.getDocument(recallId)

    StepVerifier
      .create(result)
      .assertNext {
        assertThat(it, equalTo(expectedBytes))
        verify { recallDocumentService.uploadAndAddDocumentForRecall(recallId, expectedBytes, LETTER_TO_PRISON) }
      }.verifyComplete()
  }
}
