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
import uk.gov.justice.digital.hmpps.managerecallsapi.db.CaseworkerBand
import uk.gov.justice.digital.hmpps.managerecallsapi.db.ProbationInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentenceLength
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentencingInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PersonName
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallLengthDescription
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
  private val recallBookingNumber = "B1234"
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
    recallLength = recallLength,
    lastReleaseDate = lastReleaseDate,
    localPoliceForceId = PoliceForceId("metropolitan"),
    inCustody = inCustody,
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
    previousConvictionMainNameCategory = NameFormatCategory.OTHER,
    previousConvictionMainName = "Bryan Badger",
    assessedByUserId = userId,
    lastKnownAddressOption = if (!inCustody) LastKnownAddressOption.NO_FIXED_ABODE else null
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
      currentUserDetails.userId()
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
          FullName("Barrie Badger"),
          recallBookingNumber,
          currentPrisonName,
          PersonName("Maria", lastName = "Badger"),
          inCustody,
        )
      )
    )
  }
}
