package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.PrisonLookupService
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.SearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.MiddleNames
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerOffenderSearchClient
import java.time.Clock
import java.time.LocalDate

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
    val currentPrisonName = prisonLookupService.getPrisonName(recall.currentPrison!!)
    val lastReleasePrisonName = prisonLookupService.getPrisonName(recall.lastReleasePrison!!)
    return RecallNotificationContext(recall, prisoner, userDetails, currentPrisonName, lastReleasePrisonName)
  }
}

data class RecallNotificationContext(
  val recall: Recall,
  val prisoner: Prisoner,
  val assessedByUserDetails: UserDetails,
  val currentPrisonName: PrisonName,
  val lastReleasePrisonName: PrisonName,
  private val clock: Clock = Clock.systemUTC()
) {
  init {
    /*
     validate required fields?
     e.g.
     prisoner.firstName != null
     prisoner.lastName != null
     etc
     */
  }

  fun getRevocationOrderContext(): RevocationOrderContext {
    return RevocationOrderContext(
      recall.recallId(),
      FirstAndMiddleNames(FirstName(prisoner.firstName!!), prisoner.middleNames?.let { MiddleNames(it) }),
      LastName(prisoner.lastName!!),
      prisoner.dateOfBirth!!,
      recall.bookingNumber!!,
      prisoner.croNumber!!,
      LocalDate.now(clock),
      recall.lastReleaseDate!!,
      assessedByUserDetails.signature
    )
  }
}

data class RevocationOrderContext(
  val recallId: RecallId,
  val firstAndMiddleNames: FirstAndMiddleNames,
  val lastName: LastName,
  val dateOfBirth: LocalDate,
  val bookingNumber: String,
  val croNumber: String,
  val today: LocalDate,
  val lastReleaseDate: LocalDate,
  val assessedByUserSignature: String
)
