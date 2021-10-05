package uk.gov.justice.digital.hmpps.managerecallsapi.service

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.LocalDeliveryUnit.PS_TOWER_HAMLETS
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel.LEVEL_3
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ReasonForRecall.ELM_FURTHER_OFFENCE
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength
import uk.gov.justice.digital.hmpps.managerecallsapi.db.ProbationInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentenceLength
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentencingInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Email
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.MiddleNames
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhoneNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.random.zeroes
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class RecallNotificationContextTest {
  private val fixedClock = Clock.fixed(Instant.parse("2021-10-04T13:15:50.00Z"), ZoneId.of("UTC"))
  private val assessedByUserId = ::UserId.zeroes()
  private val recallId = ::RecallId.zeroes()
  private val recallBookingNumber = "B1234"
  private val dateOfBirth = LocalDate.of(1995, 10, 3)
  private val prisonerCroNumber = "prisonerCroNumber"
  private val userSignature = "user signature"
  private val lastReleaseDate = LocalDate.of(2020, 10, 1)

  private val recallLength = RecallLength.TWENTY_EIGHT_DAYS
  private val probationOfficerName = "Mr Probation Officer"

  private val recall = Recall(
    recallId,
    NomsNumber("AA1234A"),
    contrabandDetail = "I believe that they will bring contraband to prison",
    mappaLevel = LEVEL_3,
    previousConvictionMainName = "Bryan Badger",
    bookingNumber = recallBookingNumber,
    lastReleaseDate = lastReleaseDate,
    reasonsForRecall = setOf(ELM_FURTHER_OFFENCE),
    sentencingInfo = SentencingInfo(
      lastReleaseDate,
      LocalDate.of(2020, 11, 1),
      LocalDate.of(2020, 10, 29),
      "High Court",
      "Some offence",
      SentenceLength(2, 3, 10),
    ),
    probationInfo = ProbationInfo(
      probationOfficerName,
      "01234567890",
      "officer@myprobation.com",
      PS_TOWER_HAMLETS,
      "Ms Authoriser"
    ),
    localPoliceForce = "London",
    vulnerabilityDiversityDetail = "Some stuff",
    assessedByUserId = assessedByUserId,
    recallLength = recallLength
  )

  private val prisoner = Prisoner(
    firstName = "Bertie",
    middleNames = "Basset",
    lastName = "Badger",
    dateOfBirth = dateOfBirth,
    bookNumber = "prisonerBookNumber",
    croNumber = prisonerCroNumber
  )
  private val assessedByUserDetails = UserDetails(
    assessedByUserId,
    FirstName("Maria"),
    LastName("Badger"),
    userSignature,
    Email("maria@thebadgers.set"),
    PhoneNumber("09876543210")
  )
  private val currentPrisonName = PrisonName("Prison B")

  private val lastReleasePrisonName = PrisonName("Prison A")

  private val underTest = RecallNotificationContext(
    recall,
    prisoner,
    assessedByUserDetails,
    currentPrisonName,
    lastReleasePrisonName,
    fixedClock
  )

  @Test
  fun getRevocationOrderContext() {
    val expectedFirstAndMiddleNames = FirstAndMiddleNames(FirstName("Bertie"), MiddleNames("Basset"))
    val expectedLastName = LastName("Badger")
    val expectedRevocationOrderContext = RevocationOrderContext(
      recallId,
      expectedFirstAndMiddleNames,
      expectedLastName,
      dateOfBirth,
      recallBookingNumber,
      prisonerCroNumber,
      LocalDate.of(2021, 10, 4),
      lastReleaseDate,
      userSignature
    )

    val result = underTest.getRevocationOrderContext()

    assertThat(result, equalTo(expectedRevocationOrderContext))
  }

  @Test
  fun `create LetterToProbationContext with all required data`() {
    val result = underTest.getLetterToProbationContext()

    assertThat(
      result,
      equalTo(
        LetterToProbationContext(
          LocalDate.of(2021, 10, 4),
          RecallLengthDescription(recallLength),
          probationOfficerName,
          PersonName(FirstName("Bertie"), MiddleNames("Basset"), LastName("Badger")),
          recallBookingNumber,
          currentPrisonName,
          PersonName(FirstName("Maria"), null, LastName("Badger"))
        )
      )
    )
  }

  @Test
  fun `can create RevocationOrderContext without croNumber`() {
    val prisonerWithoutCroNumber = prisoner.copy(croNumber = null)
    val underTest = RecallNotificationContext(
      recall,
      prisonerWithoutCroNumber,
      assessedByUserDetails,
      currentPrisonName,
      lastReleasePrisonName,
      fixedClock
    )

    assertDoesNotThrow { underTest.getRevocationOrderContext() }
  }

  @Test
  fun `can create RecallSummaryContext without croNumber`() {
    val prisonerWithoutCroNumber = prisoner.copy(croNumber = null)
    val underTest = RecallNotificationContext(
      recall,
      prisonerWithoutCroNumber,
      assessedByUserDetails,
      currentPrisonName,
      lastReleasePrisonName,
      fixedClock
    )

    assertDoesNotThrow { underTest.getRecallSummaryContext() }
  }
}
