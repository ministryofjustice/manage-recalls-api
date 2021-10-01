package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.PrisonLookupService
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.SearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerOffenderSearchClient

@Component
class RecallNotificationContextFactory(
  @Autowired private val recallRepository: RecallRepository,
  @Autowired private val prisonLookupService: PrisonLookupService,
  @Autowired private val prisonerOffenderSearchClient: PrisonerOffenderSearchClient,
  @Autowired private val userDetailsService: UserDetailsService
) {
  fun createContext(recallId: RecallId, userId: UserId): RecallNotificationContext {
    val recall = recallRepository.getByRecallId(recallId)
    val prisoner = prisonerOffenderSearchClient.prisonerSearch(SearchRequest(recall.nomsNumber)).block()!!.first()
    val userDetails = userDetailsService.get(userId)
    val currentPrisonName = prisonLookupService.getPrisonName(recall.currentPrison)!!
    val lastReleasePrisonName = prisonLookupService.getPrisonName(recall.lastReleasePrison)!!
    return RecallNotificationContext(recall, prisoner, userDetails, currentPrisonName, lastReleasePrisonName)
  }
}

data class RecallNotificationContext(
  val recall: Recall,
  val prisoner: Prisoner,
  val assessedByUserDetails: UserDetails,
  val currentPrisonName: String,
  val lastReleasePrisonName: String
)
