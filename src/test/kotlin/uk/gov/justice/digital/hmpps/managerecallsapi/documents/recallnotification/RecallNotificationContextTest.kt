package uk.gov.justice.digital.hmpps.managerecallsapi.documents.recallnotification

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.LocalDeliveryUnit.PS_TOWER_HAMLETS
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel.LEVEL_3
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.PreviousConvictionMainNameCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ReasonForRecall.ELM_FURTHER_OFFENCE
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength
import uk.gov.justice.digital.hmpps.managerecallsapi.db.ProbationInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentenceLength
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentencingInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PersonName
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallLengthDescription
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Email
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhoneNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PoliceForceId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.zeroes
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
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
    recallId, NomsNumber("AA1234A"), ::UserId.random(), OffsetDateTime.now(), OffsetDateTime.now(),
    recallLength = recallLength,
    lastReleaseDate = lastReleaseDate,
    localPoliceForce = "London",
    localPoliceForceId = PoliceForceId("metropolitan"),
    contraband = true,
    contrabandDetail = "I believe that they will bring contraband to prison",
    vulnerabilityDiversity = true,
    vulnerabilityDiversityDetail = "Some stuff",
    mappaLevel = LEVEL_3,
    sentencingInfo = SentencingInfo(
      lastReleaseDate,
      LocalDate.of(2020, 11, 1),
      LocalDate.of(2020, 10, 29),
      CourtId("ABCDE"),
      "Some offence",
      SentenceLength(2, 3, 10),
    ),
    bookingNumber = recallBookingNumber,
    probationInfo = ProbationInfo(
      probationOfficerName,
      "01234567890",
      "officer@myprobation.com",
      PS_TOWER_HAMLETS,
      "Ms Authoriser"
    ),
    reasonsForRecall = setOf(ELM_FURTHER_OFFENCE),
    previousConvictionMainNameCategory = PreviousConvictionMainNameCategory.OTHER,
    previousConvictionMainName = "Bryan Badger",
    assessedByUserId = assessedByUserId
  )

  private val prisoner = Prisoner(
    croNumber = prisonerCroNumber,
    firstName = "Bertie",
    lastName = "Badger",
    dateOfBirth = dateOfBirth,
    bookNumber = "prisonerBookNumber"
  )
  private val assessedByUserDetails = UserDetails(
    assessedByUserId,
    FirstName("Maria"),
    LastName("Badger"),
    userSignature,
    Email("maria@thebadgers.set"),
    PhoneNumber("09876543210"),
    OffsetDateTime.now()
  )
  private val currentPrisonName = PrisonName("Prison B")

  private val lastReleasePrisonName = PrisonName("Prison A")
  private val sentencingCourtName = CourtName("Court 1")

  private val underTest = RecallNotificationContext(
    recall,
    prisoner,
    assessedByUserDetails,
    currentPrisonName,
    lastReleasePrisonName,
    sentencingCourtName,
    fixedClock
  )

  @Test
  fun getRevocationOrderContext() {
    val expectedPersonName =
      PersonName("Bertie", lastName = "Badger")
    val expectedRevocationOrderContext = RevocationOrderContext(
      recallId,
      expectedPersonName,
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
          PersonName("Bertie", lastName = "Badger"),
          recallBookingNumber,
          currentPrisonName,
          PersonName("Maria", lastName = "Badger")
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
      sentencingCourtName,
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
      sentencingCourtName,
      fixedClock
    )

    assertDoesNotThrow { underTest.getRecallSummaryContext() }
  }
}
