package uk.gov.justice.digital.hmpps.managerecallsapi.service

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.PrisonLookupService
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.SearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ImageData.Companion.recallImage
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerOffenderSearchClient
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallImage.HmppsLogo

class RecallSummaryServiceTest {
  private val pdfDocumentGenerationService = mockk<PdfDocumentGenerationService>()
  private val recallSummaryGenerator = mockk<RecallSummaryGenerator>()
  private val recallRepository = mockk<RecallRepository>()
  private val prisonLookupService = mockk<PrisonLookupService>()
  private val prisonerOffenderSearchClient = mockk<PrisonerOffenderSearchClient>()

  private val underTest = RecallSummaryService(
    pdfDocumentGenerationService,
    recallSummaryGenerator, recallRepository, prisonLookupService, prisonerOffenderSearchClient
  )

  @Test
  fun `generates the recall summary PDF with required information`() {
    val recallId = ::RecallId.random()
    val nomsNumber = NomsNumber("nomsNumber")
    val currentPrison = "AAA"
    val lastReleasePrison = "ZZZ"
    val prisoner = mockk<Prisoner>()
    val recall = Recall(recallId, nomsNumber, currentPrison = currentPrison, lastReleasePrison = lastReleasePrison)
    val currentPrisonName = "Current Prison Name"
    val lastReleasePrisonName = "Last Release Prison Name"
    val recallSummaryHtml = "generated Html"
    val expectedPdfBytes = "some bytes".toByteArray()

    every { recallRepository.getByRecallId(recallId) } returns recall
    every { prisonLookupService.getPrisonName(currentPrison) } returns currentPrisonName
    every { prisonLookupService.getPrisonName(lastReleasePrison) } returns lastReleasePrisonName
    every { prisonerOffenderSearchClient.prisonerSearch(SearchRequest(nomsNumber)) } returns Mono.just(listOf(prisoner))
    every { recallSummaryGenerator.generateHtml(RecallSummaryContext(recall, prisoner, lastReleasePrisonName, currentPrisonName, MINIMUM_NUMBER_OF_PAGES_IN_RECALL_NOTIFICATION)) } returns recallSummaryHtml
    every { pdfDocumentGenerationService.generatePdf(recallSummaryHtml, recallImage(HmppsLogo)) } returns Mono.just(expectedPdfBytes)

    val result = underTest.getPdf(recallId).block()!!

    assertArrayEquals(expectedPdfBytes, result)
  }
}
