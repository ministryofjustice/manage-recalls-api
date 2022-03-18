package uk.gov.justice.digital.hmpps.managerecallsapi.documents.recallnotification

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.LastKnownAddressOption
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.LocalDeliveryUnit.PS_TOWER_HAMLETS
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel.LEVEL_3
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.NameFormatCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ReasonForRecall.ELM_FURTHER_OFFENCE
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType.FIXED
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType.STANDARD
import uk.gov.justice.digital.hmpps.managerecallsapi.db.CaseworkerBand
import uk.gov.justice.digital.hmpps.managerecallsapi.db.ProbationInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentenceLength
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentencingInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PersonName
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallDescription
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.BookingNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CroNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Email
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FullName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhoneNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PoliceForceId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PoliceForceName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.zeroes
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import kotlin.random.Random

class RecallNotificationContextTest {
  private val fixedClock = Clock.fixed(Instant.parse("2021-10-04T13:15:50.00Z"), ZoneId.of("UTC"))
  private val userId = ::UserId.zeroes()
  private val recallId = ::RecallId.zeroes()
  private val recallBookingNumber = BookingNumber("B1234")
  private val dateOfBirth = LocalDate.of(1995, 10, 3)
  private val prisonerCroNumber = CroNumber("ABC/1234A")
  private val userSignature = "user signature"
  private val lastReleaseDate = LocalDate.of(2020, 10, 1)

  private val recallLength = RecallLength.TWENTY_EIGHT_DAYS
  private val probationOfficerName = "Mr Probation Officer"
  private val inCustody = Random.nextBoolean()

  private val recall = Recall(
    recallId,
    NomsNumber("AA1234A"),
    ::UserId.random(),
    OffsetDateTime.now(),
    FirstName("Barrie"),
    null,
    LastName("Badger"),
    prisonerCroNumber,
    dateOfBirth,
    licenceNameCategory = NameFormatCategory.FIRST_LAST,
    reasonsForRecall = setOf(ELM_FURTHER_OFFENCE),
    assessedByUserId = userId,
    bookingNumber = recallBookingNumber,
    confirmedRecallType = FIXED,
    contraband = true,
    contrabandDetail = "I believe that they will bring contraband to prison",
    inCustodyAtAssessment = if (inCustody) null else false,
    inCustodyAtBooking = inCustody,
    lastKnownAddressOption = if (!inCustody) LastKnownAddressOption.NO_FIXED_ABODE else null,
    lastReleaseDate = lastReleaseDate,
    localPoliceForceId = PoliceForceId("metropolitan"),
    mappaLevel = LEVEL_3,
    previousConvictionMainName = "Bryan Badger",
    previousConvictionMainNameCategory = NameFormatCategory.OTHER,
    probationInfo = ProbationInfo(
      probationOfficerName,
      "01234567890",
      "officer@myprobation.com",
      PS_TOWER_HAMLETS,
      "Ms Authoriser"
    ),
    recallLength = recallLength,
    recommendedRecallType = STANDARD,
    sentencingInfo = SentencingInfo(
      lastReleaseDate,
      LocalDate.of(2020, 11, 1),
      LocalDate.of(2020, 10, 29),
      CourtId("ABCDE"),
      "Some offence",
      SentenceLength(2, 3, 10),
    ),
    vulnerabilityDiversity = true,
    vulnerabilityDiversityDetail = "Some stuff"
  )

  private val currentUserDetails = UserDetails(
    userId,
    FirstName("Maria"),
    LastName("Badger"),
    userSignature,
    Email("maria@thebadgers.set"),
    PhoneNumber("09876543210"),
    CaseworkerBand.FOUR_PLUS,
    OffsetDateTime.now()
  )
  private val currentPrisonName = PrisonName("Prison B")

  private val lastReleasePrisonName = PrisonName("Prison A")
  private val sentencingCourtName = CourtName("Court 1")
  private val localPoliceForceName = PoliceForceName("Police Service of Northern Ireland")

  private val underTest = RecallNotificationContext(
    recall,
    currentUserDetails,
    currentPrisonName,
    lastReleasePrisonName,
    sentencingCourtName,
    localPoliceForceName,
    OffsetDateTime.now(fixedClock)
  )

  @Test
  fun getRevocationOrderContext() {
    val expectedRevocationOrderContext = RevocationOrderContext(
      recallId,
      FullName("Barrie Badger"),
      dateOfBirth,
      recallBookingNumber,
      prisonerCroNumber,
      LocalDate.of(2021, 10, 4),
      lastReleaseDate,
      userSignature,
      currentUserDetails.userId(),
      "Badger Barrie"
    )

    val result = underTest.getRevocationOrderContext()

    assertThat(result, equalTo(expectedRevocationOrderContext))
  }

  @Test
  fun `create LetterToProbationContext for recall with all required data`() {
    val result = underTest.getLetterToProbationContext()

    assertThat(
      result,
      equalTo(
        LetterToProbationContext(
          LocalDate.of(2021, 10, 4),
          RecallDescription(FIXED, recallLength),
          probationOfficerName,
          FullName("Barrie Badger"),
          recallBookingNumber,
          currentPrisonName,
          PersonName("Maria", lastName = "Badger"),
          inCustody,
          FIXED
        )
      )
    )
  }
}
