package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.PrisonLookupService
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.SearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerOffenderSearchClient

@Component
class RecallSummaryContextFactory(
  @Autowired private val recallRepository: RecallRepository,
  @Autowired private val prisonLookupService: PrisonLookupService,
  @Autowired private val prisonerOffenderSearchClient: PrisonerOffenderSearchClient,
) {
  fun createRecallSummaryContext(recallId: RecallId): Mono<RecallSummaryContext> {
    val recall = recallRepository.getByRecallId(recallId)
    val currentPrisonName = prisonLookupService.getPrisonName(recall.currentPrison)!!
    val lastReleasePrisonName = prisonLookupService.getPrisonName(recall.lastReleasePrison)!!
    return prisonerOffenderSearchClient.prisonerSearch(SearchRequest(recall.nomsNumber))
      .map { prisoners ->
        RecallSummaryContext(recall, prisoners.first(), lastReleasePrisonName, currentPrisonName)
      }
  }
}
