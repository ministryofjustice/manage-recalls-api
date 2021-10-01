package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.MiddleNames
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import java.time.Clock
import java.time.LocalDate

@Component
class LetterToProbationContextFactory(
  @Autowired private val clock: Clock
) {
  fun createContext(recallNotificationContext: RecallNotificationContext): LetterToProbationContext {
    // TODO:  Ensure all the required data is present, if not throw a meaningful exception (should be applied in a consistent manner)
    val recall = recallNotificationContext.recall
    val currentPrisonName = recallNotificationContext.currentPrisonName
    val assessedByUserDetails = recallNotificationContext.assessedByUserDetails
    val prisoner = recallNotificationContext.prisoner

    return LetterToProbationContext(
      LocalDate.now(clock),
      RecallLengthDescription(recall.recallLength!!),
      recall.probationInfo!!.probationOfficerName,
      PersonName(FirstName(prisoner.firstName!!), prisoner.middleNames?.let { MiddleNames(it) }, LastName(prisoner.lastName!!)),
      recall.bookingNumber!!,
      currentPrisonName,
      PersonName(assessedByUserDetails.firstName, null, assessedByUserDetails.lastName)
    )
  }
}

data class LetterToProbationContext(
  val licenceRevocationDate: LocalDate,
  val recallLengthDescription: RecallLengthDescription,
  val probationOfficerName: String,
  val offenderName: PersonName,
  val bookingNumber: String,
  val currentPrisonName: PrisonName,
  val assessedByUserName: PersonName
)
