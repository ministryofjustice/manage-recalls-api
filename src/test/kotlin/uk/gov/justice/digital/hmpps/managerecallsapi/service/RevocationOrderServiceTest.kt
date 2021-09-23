package uk.gov.justice.digital.hmpps.managerecallsapi.service

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.Called
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.SearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ClassPathDocumentDetail
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
import uk.gov.justice.digital.hmpps.managerecallsapi.storage.S3Service
import java.io.File
import java.util.Base64
import java.util.UUID

@Suppress("ReactiveStreamsUnusedPublisher")
internal class RevocationOrderServiceTest {

  private val pdfDocumentGenerationService = mockk<PdfDocumentGenerationService>()
  private val prisonerOffenderSearchClient = mockk<PrisonerOffenderSearchClient>()
  private val s3Service = mockk<S3Service>()
  private val recallRepository = mockk<RecallRepository>()
  private val revocationOrderGenerator = mockk<RevocationOrderGenerator>()
  private val userDetailsService = mockk<UserDetailsService>()

  private val underTest = RevocationOrderService(
    pdfDocumentGenerationService,
    prisonerOffenderSearchClient,
    s3Service,
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
    val revocationOrderIdSlot = slot<UUID>()
    val savedRecallSlot = slot<Recall>()
    val userId = UserId(UUID.randomUUID())

    every { recallRepository.getByRecallId(recallId) } returns aRecall
    every { prisonerOffenderSearchClient.prisonerSearch(SearchRequest(nomsNumber)) } returns Mono.just(listOf(prisoner))
    val generatedHtml = "Some html, honest"
    every { userDetailsService.get(userId) } returns UserDetails(userId, FirstName("Bob"), LastName("Badger"), Base64.getEncoder().encodeToString(File("src/test/resources/signature.jpg").readBytes()))
    every { revocationOrderGenerator.generateHtml(prisoner, aRecall) } returns generatedHtml
    every { pdfDocumentGenerationService.generatePdf(generatedHtml, ClassPathDocumentDetail("revocation-order-logo.png"), any()) } returns Mono.just(expectedBytes)
    every { s3Service.uploadFile(capture(revocationOrderIdSlot), expectedBytes) } just runs
    every { recallRepository.save(capture(savedRecallSlot)) } returns mockk()

    val result = underTest.createPdf(recallId, userId).block()!!

    assertThat(result, equalTo(expectedBytes))
    assertThat(savedRecallSlot.captured, equalTo(Recall(recallId, nomsNumber, revocationOrderIdSlot.captured)))
  }

  @Test
  fun `gets existing revocation order for a recall`() {

    val revocationOrderId = UUID.randomUUID()
    val theRecallWithRevocationOrder = Recall(recallId, nomsNumber, revocationOrderId)

    every { recallRepository.getByRecallId(recallId) } returns theRecallWithRevocationOrder
    every { recallRepository.save(theRecallWithRevocationOrder) } returns theRecallWithRevocationOrder
    every { s3Service.downloadFile(revocationOrderId) } returns expectedBytes

    val result = underTest.getPdf(recallId)

    StepVerifier
      .create(result)
      .assertNext {
        assertThat(it, equalTo(expectedBytes))
        verify { prisonerOffenderSearchClient wasNot Called }
        verify { pdfDocumentGenerationService wasNot Called }
        verify { revocationOrderGenerator wasNot Called }
        verify(exactly = 0) { recallRepository.save(theRecallWithRevocationOrder) }
        verify(exactly = 0) { s3Service.uploadFile(any(), any()) }
        verify { recallRepository.getByRecallId(recallId) }
        verify { s3Service.downloadFile(revocationOrderId) }
      }
      .verifyComplete()
  }
}
