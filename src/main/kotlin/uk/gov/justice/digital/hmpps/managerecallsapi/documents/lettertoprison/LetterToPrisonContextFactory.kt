package uk.gov.justice.digital.hmpps.managerecallsapi.documents.lettertoprison

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.PrisonLookupService
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.SearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerOffenderSearchClient
import uk.gov.justice.digital.hmpps.managerecallsapi.service.UserDetailsService

@Component
class LetterToPrisonContextFactory(
  @Autowired private val recallRepository: RecallRepository,
  @Autowired private val prisonLookupService: PrisonLookupService,
  @Autowired private val prisonerOffenderSearchClient: PrisonerOffenderSearchClient,
  @Autowired private val userDetailsService: UserDetailsService,
) {
  fun createContext(recallId: RecallId): LetterToPrisonContext {
    val recall = recallRepository.getByRecallId(recallId)
    val currentPrisonName = prisonLookupService.getPrisonName(recall.currentPrison()!!)
    val lastReleasePrisonName = prisonLookupService.getPrisonName(recall.lastReleasePrison()!!)
    val prisoner = prisonerOffenderSearchClient.prisonerSearch(SearchRequest(recall.nomsNumber)).block()!!.first()
    val assessedByUserDetails = userDetailsService.get(recall.assessedByUserId()!!)
    return LetterToPrisonContext(recall, prisoner, currentPrisonName, lastReleasePrisonName, assessedByUserDetails)
  }
}

data class LetterToPrisonContext(
  val recall: Recall,
  val prisoner: Prisoner,
  val currentPrisonName: PrisonName,
  val lastReleasePrisonName: PrisonName,
  val assessor: UserDetails
)
