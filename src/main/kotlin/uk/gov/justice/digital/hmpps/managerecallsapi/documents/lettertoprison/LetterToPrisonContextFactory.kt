package uk.gov.justice.digital.hmpps.managerecallsapi.documents.lettertoprison

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallLengthDescription
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FullName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.service.PrisonLookupService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.UserDetailsService

@Component
class LetterToPrisonContextFactory(
  @Autowired private val recallRepository: RecallRepository,
  @Autowired private val prisonLookupService: PrisonLookupService,
  @Autowired private val userDetailsService: UserDetailsService,
) {
  fun createContext(recallId: RecallId, currentUserId: UserId): LetterToPrisonContext {
    val recall = recallRepository.getByRecallId(recallId)
    val currentPrisonName = prisonLookupService.getPrisonName(recall.currentPrison!!)
    val lastReleasePrisonName = prisonLookupService.getPrisonName(recall.lastReleasePrison!!)
    val currentUserDetails = userDetailsService.get(currentUserId)
    val recallLengthDescription = RecallLengthDescription(recall.recallLength!!)
    return LetterToPrisonContext(recall, recall.prisonerNameOnLicense(), currentPrisonName, lastReleasePrisonName, recallLengthDescription, currentUserDetails)
  }
}

data class LetterToPrisonContext(
  val recall: Recall,
  val prisonerNameOnLicense: FullName,
  val currentPrisonName: PrisonName,
  val lastReleasePrisonName: PrisonName,
  val recallLengthDescription: RecallLengthDescription,
  val currentUser: UserDetails
)
