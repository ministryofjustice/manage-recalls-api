package uk.gov.justice.digital.hmpps.managerecallsapi.service

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.LocalDeliveryUnit.ISLE_OF_MAN
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength.TWENTY_EIGHT_DAYS
import uk.gov.justice.digital.hmpps.managerecallsapi.db.ProbationInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Email
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.MiddleNames
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhoneNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class LetterToProbationContextFactoryTest {
  private val fixedClock = Clock.fixed(Instant.parse("2021-09-29T00:00:00.00Z"), ZoneId.of("UTC"))

  private val underTest = LetterToProbationContextFactory(fixedClock)

  // TODO:   What do we do if ANY of this data is missing?

  @Test
  fun `create LetterToProbationContext with all required data`() {
    val recallId = ::RecallId.random()
    val nomsNumber = NomsNumber("nomsNumber")
    val userIdGeneratingTheLetter = ::UserId.random()
    val currentPrison = "AAA"
    val currentPrisonName = "Current Prison Name"
    val prisoner = Prisoner(firstName = "Jimmy", lastName = "Offender", middleNames = "The Hand")
    val probationOfficerName = "Mr Probation Officer"
    val bookingNumber = "bookingNumber"
    val recall = Recall(
      recallId,
      nomsNumber,
      currentPrison = currentPrison,
      recallLength = TWENTY_EIGHT_DAYS,
      probationInfo = ProbationInfo(probationOfficerName, "N/A", "N/A", ISLE_OF_MAN, "N/A"),
      bookingNumber = bookingNumber
    )
    val userDetails = UserDetails(
      userIdGeneratingTheLetter,
      FirstName("Bertie"),
      LastName("Badger"),
      "",
      Email("b@b.com"),
      PhoneNumber("09876543210")
    )

    val recallNotificationContext = RecallNotificationContext(recall, prisoner, userDetails, currentPrisonName)
    val result = underTest.createContext(recallNotificationContext)

    assertThat(
      result,
      equalTo(
        LetterToProbationContext(
          LocalDate.of(2021, 9, 29),
          RecallLengthDescription(TWENTY_EIGHT_DAYS),
          probationOfficerName,
          PersonName(FirstName("Jimmy"), MiddleNames("The Hand"), LastName("Offender")),
          bookingNumber,
          currentPrisonName,
          PersonName(FirstName("Bertie"), null, LastName("Badger"))
        )
      )
    )
  }
}
