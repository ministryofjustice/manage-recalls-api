package uk.gov.justice.digital.hmpps.managerecallsapi.service

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
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
    val recallSummaryHtmlWithoutPageCount = "generated Html without page count"
    val recallSummaryHtmlWithPageCount = "generated Html with page count"
    val pdfWith3Pages = ClassPathResource("/document/3_pages_unnumbered.pdf").file.readBytes()
    val recallNotificationTotalNumberOfPages = 5

    every { recallRepository.getByRecallId(recallId) } returns recall
    every { prisonLookupService.getPrisonName(currentPrison) } returns currentPrisonName
    every { prisonLookupService.getPrisonName(lastReleasePrison) } returns lastReleasePrisonName
    every { prisonerOffenderSearchClient.prisonerSearch(SearchRequest(nomsNumber)) } returns Mono.just(listOf(prisoner))

    every { recallSummaryGenerator.generateHtml(RecallSummaryContext(recall, prisoner, lastReleasePrisonName, currentPrisonName)) } returns recallSummaryHtmlWithoutPageCount
    every { pdfDocumentGenerationService.generatePdf(recallSummaryHtmlWithoutPageCount, recallImage(HmppsLogo)) } returns Mono.just(pdfWith3Pages)
    every { recallSummaryGenerator.generateHtml(RecallSummaryContext(recall, prisoner, lastReleasePrisonName, currentPrisonName, recallNotificationTotalNumberOfPages)) } returns recallSummaryHtmlWithPageCount
    every { pdfDocumentGenerationService.generatePdf(recallSummaryHtmlWithPageCount, recallImage(HmppsLogo)) } returns Mono.just(pdfWith3Pages)

    val result = underTest.getPdf(recallId).block()!!

    assertArrayEquals(pdfWith3Pages, result)
  }
}
