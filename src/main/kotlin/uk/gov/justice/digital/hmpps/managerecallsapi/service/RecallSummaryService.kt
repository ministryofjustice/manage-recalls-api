package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.PrisonLookupService
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.SearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ImageData.Companion.recallImage
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerOffenderSearchClient
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallImage.HmppsLogo

const val MINIMUM_NUMBER_OF_PAGES_IN_RECALL_NOTIFICATION = 3

@Service
class RecallSummaryService(
  @Autowired private val pdfDocumentGenerationService: PdfDocumentGenerationService,
  @Autowired private val recallSummaryGenerator: RecallSummaryGenerator,
  @Autowired private val recallRepository: RecallRepository,
  @Autowired private val prisonLookupService: PrisonLookupService,
  @Autowired private val prisonerOffenderSearchClient: PrisonerOffenderSearchClient,
) {

  fun getPdf(recallId: RecallId): Mono<ByteArray> {
    val recall = recallRepository.getByRecallId(recallId)
    val currentPrisonName = prisonLookupService.getPrisonName(recall.currentPrison)!!
    val lastReleasePrisonName = prisonLookupService.getPrisonName(recall.lastReleasePrison)!!

    return prisonerOffenderSearchClient.prisonerSearch(SearchRequest(recall.nomsNumber))
      .flatMap { prisoners ->
        val recallSummaryHtml = recallSummaryGenerator.generateHtml(
          RecallSummaryContext(recall, prisoners.first(), lastReleasePrisonName, currentPrisonName, MINIMUM_NUMBER_OF_PAGES_IN_RECALL_NOTIFICATION)
        )

        pdfDocumentGenerationService.generatePdf(
          recallSummaryHtml,
          recallImage(HmppsLogo)
        )
      }
  }
}

data class RecallSummaryContext(
  val recall: Recall,
  val prisoner: Prisoner,
  val lastReleasePrisonName: String,
  val currentPrisonName: String,
  val recallNotificationTotalNumberOfPages: Int
)
