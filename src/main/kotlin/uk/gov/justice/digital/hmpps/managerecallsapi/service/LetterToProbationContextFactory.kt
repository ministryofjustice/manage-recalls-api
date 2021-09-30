package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.PrisonLookupService
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.SearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerOffenderSearchClient
import java.time.Clock
import java.time.LocalDate

@Component
class LetterToProbationContextFactory(
  @Autowired private val recallRepository: RecallRepository,
  @Autowired private val prisonLookupService: PrisonLookupService,
  @Autowired private val prisonerOffenderSearchClient: PrisonerOffenderSearchClient,
  @Autowired private val userDetailsService: UserDetailsService,
  @Autowired private val clock: Clock
) {
  fun createContext(recallId: RecallId, userId: UserId): Mono<LetterToProbationContext> {
    // TODO:  Ensure all the required data is present, if not throw a meaningful exception (should be applied in a consistent manner)
    val recall = recallRepository.getByRecallId(recallId)
    val currentPrisonName = prisonLookupService.getPrisonName(recall.currentPrison)!!
    val assessedByUserDetails = userDetailsService.get(recall.assessedByUserId()!!)

    return prisonerOffenderSearchClient.prisonerSearch(SearchRequest(recall.nomsNumber))
      .map { prisoners -> prisoners.first() }
      .map { prisoner ->
        LetterToProbationContext(
          LocalDate.now(clock),
          RecallLengthDescription(recall.recallLength!!),
          recall.probationInfo!!.probationOfficerName,
          FirstAndLastName(FirstName(prisoner.firstName!!), LastName(prisoner.lastName!!)),
          recall.bookingNumber!!,
          currentPrisonName,
          FirstAndLastName(assessedByUserDetails.firstName, assessedByUserDetails.lastName)
        )
      }
  }
}

data class LetterToProbationContext(
  val licenceRevocationDate: LocalDate,
  val recallLengthDescription: RecallLengthDescription,
  val probationOfficerName: String,
  val offenderName: FirstAndLastName,
  val bookingNumber: String,
  val currentPrisonName: String,
  val assessedByUserName: FirstAndLastName
)
