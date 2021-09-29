package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.SearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ImageData.Companion.recallImage
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerOffenderSearchClient
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallImage.HmppsLogo

@Service
class ReasonsForRecallService(
  @Autowired private val pdfDocumentGenerationService: PdfDocumentGenerationService,
  @Autowired private val reasonsForRecallGenerator: ReasonsForRecallGenerator,
  @Autowired private val recallRepository: RecallRepository,
  @Autowired private val prisonerOffenderSearchClient: PrisonerOffenderSearchClient,
) {

  fun getPdf(recallId: RecallId): Mono<ByteArray> {
    val recall = recallRepository.getByRecallId(recallId)

    return prisonerOffenderSearchClient.prisonerSearch(SearchRequest(recall.nomsNumber))
      .flatMap { prisoners ->
        val recallSummaryHtml = reasonsForRecallGenerator.generateHtml(ReasonsForRecallContext(recall, prisoners.first()))

        pdfDocumentGenerationService.generatePdf(
          recallSummaryHtml,
          recallImage(HmppsLogo)
        )
      }
  }
}

data class ReasonsForRecallContext(val recall: Recall, val prisoner: Prisoner)
