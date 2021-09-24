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
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.SearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.REVOCATION_ORDER
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.Base64EncodedImageData
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ClassPathImageData
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomNoms
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomString
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerOffenderSearchClient
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallImage.RevocationOrderLogo
import java.io.File
import java.util.Base64
import java.util.UUID

@Suppress("ReactiveStreamsUnusedPublisher")
internal class RevocationOrderServiceTest {

  private val pdfDocumentGenerationService = mockk<PdfDocumentGenerationService>()
  private val prisonerOffenderSearchClient = mockk<PrisonerOffenderSearchClient>()
  private val recallDocumentService = mockk<RecallDocumentService>()
  private val recallRepository = mockk<RecallRepository>()
  private val revocationOrderGenerator = mockk<RevocationOrderGenerator>()
  private val userDetailsService = mockk<UserDetailsService>()

  private val underTest = RevocationOrderService(
    pdfDocumentGenerationService,
    prisonerOffenderSearchClient,
    recallDocumentService,
    recallRepository,
    revocationOrderGenerator,
    userDetailsService
  )

  // TODO: will be good to agree as the dev team whether we prefer this model or local vals per test fun
  private val recallId = ::RecallId.random()
  private val expectedBytes = randomString().toByteArray()
  private val nomsNumber = randomNoms()

  @Test
  fun `creates a revocation order for a recall `() {
    val aRecall = Recall(recallId, nomsNumber)
    val prisoner = mockk<Prisoner>()
    val revocationOrderId = UUID.randomUUID()
    val userId = UserId(UUID.randomUUID())
    val userSignature = Base64.getEncoder().encodeToString(File("src/test/resources/signature.jpg").readBytes())

    every { recallRepository.getByRecallId(recallId) } returns aRecall
    every { prisonerOffenderSearchClient.prisonerSearch(SearchRequest(nomsNumber)) } returns Mono.just(listOf(prisoner))
    val generatedHtml = "Some html, honest"
    every { userDetailsService.get(userId) } returns UserDetails(userId, FirstName("Bob"), LastName("Badger"), userSignature)
    every { revocationOrderGenerator.generateHtml(prisoner, aRecall) } returns generatedHtml
    every {
      pdfDocumentGenerationService.generatePdf(
        generatedHtml,
        ClassPathImageData(RevocationOrderLogo),
        Base64EncodedImageData("signature.jpg", userSignature)
      )
    } returns Mono.just(expectedBytes)
    every {
      recallDocumentService.uploadAndAddDocumentForRecall(recallId, expectedBytes, REVOCATION_ORDER)
    } returns revocationOrderId

    val result = underTest.createPdf(recallId, userId)

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
        verify { prisonerOffenderSearchClient wasNot Called }
        verify { pdfDocumentGenerationService wasNot Called }
        verify { revocationOrderGenerator wasNot Called }
        verify { recallRepository wasNot Called }
        verify { userDetailsService wasNot Called }
        verify { recallDocumentService.getDocumentContentWithCategory(recallId, REVOCATION_ORDER) }
      }
      .verifyComplete()
  }
}
