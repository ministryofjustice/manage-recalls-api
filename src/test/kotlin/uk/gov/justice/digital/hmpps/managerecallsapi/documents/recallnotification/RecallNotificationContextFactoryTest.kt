package uk.gov.justice.digital.hmpps.managerecallsapi.documents.recallnotification

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.LastKnownAddressOption
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.LocalDeliveryUnit
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.NameFormatCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ReasonForRecall.BREACH_EXCLUSION_ZONE
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ReasonForRecall.ELM_FURTHER_OFFENCE
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ReasonForRecall.FAILED_HOME_VISIT
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ReasonForRecall.OTHER
import uk.gov.justice.digital.hmpps.managerecallsapi.db.AddressSource
import uk.gov.justice.digital.hmpps.managerecallsapi.db.CaseworkerBand
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Document
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.RECALL_NOTIFICATION
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.LastKnownAddress
import uk.gov.justice.digital.hmpps.managerecallsapi.db.ProbationInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentenceLength
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentencingInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CroNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Email
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FullName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastKnownAddressId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.MiddleNames
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhoneNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PoliceForceId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PoliceForceName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.service.CourtLookupService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.PoliceForceLookupService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.PrisonLookupService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.UserDetailsService
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RecallNotificationContextFactoryTest {
  private val fixedClock = Clock.fixed(Instant.parse("2021-10-04T13:15:50.00Z"), ZoneId.of("UTC"))
  private val recallRepository = mockk<RecallRepository>()
  private val prisonLookupService = mockk<PrisonLookupService>()
  private val userDetailsService = mockk<UserDetailsService>()
  private val courtLookupService = mockk<CourtLookupService>()
  private val policeForceLookupService = mockk<PoliceForceLookupService>()
  private val documentRepository = mockk<DocumentRepository>()

  private val underTest = RecallNotificationContextFactory(
    recallRepository,
    prisonLookupService,
    userDetailsService,
    courtLookupService,
    policeForceLookupService,
    documentRepository,
    fixedClock
  )

  val userDetails =
    UserDetails(
      ::UserId.random(), FirstName("Sue"), LastName("Smith"), "", Email("s@smith.com"), PhoneNumber("0123"),
      CaseworkerBand.FOUR_PLUS,
      OffsetDateTime.now()
    )

  @Test
  fun `create RecallNotificationContext with required details with licenseRevocationDate from first revocation order`() {
    val recallId = ::RecallId.random()
    val nomsNumber = NomsNumber("nomsNumber")
    val userIdGeneratingRecallNotification = ::UserId.random()
    val currentPrisonId = PrisonId("AAA")
    val currentPrisonName = PrisonName("Current Prison Name")
    val lastReleasePrisonId = PrisonId("XXX")
    val lastReleasePrisonName = PrisonName("Last Prison Name")
    val sentencingInfo = mockk<SentencingInfo>()
    val sentencingCourtId = CourtId("ABCDEF")
    val sentencingCourtName = CourtName("A Court")
    val localPoliceForceId = PoliceForceId("local-police-service")
    val localPoliceForceName = PoliceForceName("Police Service of Northern Ireland")
    val recall = Recall(
      recallId,
      nomsNumber,
      ::UserId.random(),
      OffsetDateTime.now(), FirstName("Barrie"), null, LastName("Badger"),
      CroNumber("ABC/1234A"),
      LocalDate.of(1999, 12, 1),
      licenceNameCategory = NameFormatCategory.FIRST_LAST,
      currentPrison = currentPrisonId,
      inCustodyAtBooking = true,
      lastReleasePrison = lastReleasePrisonId,
      localPoliceForceId = localPoliceForceId,
      sentencingInfo = sentencingInfo
    )
    val userDetails = mockk<UserDetails>()
    val document = mockk<Document>()
    val threeDaysAgo = OffsetDateTime.now().minusDays(3)

    every { sentencingInfo.sentencingCourt } returns sentencingCourtId
    every { recallRepository.getByRecallId(recallId) } returns recall
    every { prisonLookupService.getPrisonName(currentPrisonId) } returns currentPrisonName
    every { prisonLookupService.getPrisonName(lastReleasePrisonId) } returns lastReleasePrisonName
    every { courtLookupService.getCourtName(sentencingCourtId) } returns sentencingCourtName
    every { policeForceLookupService.getPoliceForceName(localPoliceForceId) } returns localPoliceForceName
    every { userDetailsService.get(userIdGeneratingRecallNotification) } returns userDetails
    every { documentRepository.findByRecallIdAndCategoryAndVersion(recall.id, RECALL_NOTIFICATION, 1) } returns document
    every { document.createdDateTime } returns threeDaysAgo

    val result = underTest.createContext(recallId, userIdGeneratingRecallNotification)

    assertThat(
      result,
      equalTo(
        RecallNotificationContext(
          recall,
          userDetails,
          currentPrisonName,
          lastReleasePrisonName,
          sentencingCourtName,
          localPoliceForceName,
          threeDaysAgo
        )
      )
    )
  }

  @Test
  fun `create RecallNotificationContext with required details uses now as revocationOrderCreationDateTime when no existing revocation order`() {
    val recallId = ::RecallId.random()
    val nomsNumber = NomsNumber("nomsNumber")
    val userIdGeneratingRecallNotification = ::UserId.random()
    val currentPrisonId = PrisonId("AAA")
    val currentPrisonName = PrisonName("Current Prison Name")
    val lastReleasePrisonId = PrisonId("XXX")
    val lastReleasePrisonName = PrisonName("Last Prison Name")
    val sentencingInfo = mockk<SentencingInfo>()
    val sentencingCourtId = CourtId("ABCDEF")
    val sentencingCourtName = CourtName("A Court")
    val localPoliceForceId = PoliceForceId("local-police-service")
    val localPoliceForceName = PoliceForceName("Police Service of Northern Ireland")
    val recall = Recall(
      recallId,
      nomsNumber,
      ::UserId.random(),
      OffsetDateTime.now(), FirstName("Barrie"), null, LastName("Badger"),
      CroNumber("ABC/1234A"),
      LocalDate.of(1999, 12, 1),
      currentPrison = currentPrisonId,
      inCustodyAtBooking = true,
      lastReleasePrison = lastReleasePrisonId,
      localPoliceForceId = localPoliceForceId,
      sentencingInfo = sentencingInfo
    )
    val userDetails = mockk<UserDetails>()

    every { sentencingInfo.sentencingCourt } returns sentencingCourtId
    every { recallRepository.getByRecallId(recallId) } returns recall
    every { prisonLookupService.getPrisonName(currentPrisonId) } returns currentPrisonName
    every { prisonLookupService.getPrisonName(lastReleasePrisonId) } returns lastReleasePrisonName
    every { courtLookupService.getCourtName(sentencingCourtId) } returns sentencingCourtName
    every { policeForceLookupService.getPoliceForceName(localPoliceForceId) } returns localPoliceForceName
    every { userDetailsService.get(userIdGeneratingRecallNotification) } returns userDetails
    every { documentRepository.findByRecallIdAndCategoryAndVersion(recall.id, RECALL_NOTIFICATION, 1) } returns null

    val result = underTest.createContext(recallId, userIdGeneratingRecallNotification)

    assertThat(
      result,
      equalTo(
        RecallNotificationContext(
          recall,
          userDetails,
          currentPrisonName,
          lastReleasePrisonName,
          sentencingCourtName,
          localPoliceForceName,
          OffsetDateTime.now(fixedClock)
        )
      )
    )
  }

  private fun prevConsMainNameOptions(): Stream<Arguments> {
    return Stream.of(
      Arguments.of("Bobby Badger", NameFormatCategory.FIRST_LAST, "Andy Badger"),
      Arguments.of("Bobby Badger", NameFormatCategory.FIRST_MIDDLE_LAST, "Andy Bertie Badger"),
      Arguments.of("Other Name", NameFormatCategory.OTHER, "Other Name"),
      Arguments.of("", NameFormatCategory.FIRST_LAST, "Andy Badger"),
      Arguments.of("", NameFormatCategory.FIRST_MIDDLE_LAST, "Andy Bertie Badger"),
      Arguments.of("", NameFormatCategory.OTHER, "")
    )
  }

  @ParameterizedTest(name = "create RecallSummaryContext when preCons Main Name Category is {1}")
  @MethodSource("prevConsMainNameOptions")
  fun `create RecallSummaryContext with required details`(prevConsMainName: String?, prevConsMainNameCategory: NameFormatCategory?, expectedName: String) {
    val recallId = ::RecallId.random()
    val nomsNumber = NomsNumber("nomsNumber")
    val userIdGeneratingRecallNotification = ::UserId.random()
    val lastReleasePrisonId = PrisonId("XXX")
    val lastReleasePrisonName = PrisonName("Last Prison Name")
    val probationInfo = ProbationInfo("", "", "", LocalDeliveryUnit.CENTRAL_AUDIT_TEAM, "")
    val sentencingCourtId = CourtId("ABCDE")
    val sentencingInfo =
      SentencingInfo(LocalDate.now(), LocalDate.now(), LocalDate.now(), sentencingCourtId, "", SentenceLength(3, 1, 0))
    val localPoliceForceId = PoliceForceId("XYZ")
    val recall = Recall(
      recallId,
      nomsNumber,
      ::UserId.random(),
      OffsetDateTime.now(),
      FirstName("Andy"),
      MiddleNames("Bertie"),
      LastName("Badger"),
      CroNumber("ABC/1234A"),
      LocalDate.of(1999, 12, 1),
      lastKnownAddresses = setOf(
        LastKnownAddress(::LastKnownAddressId.random(), recallId, "Line 1", null, "A Town", null, AddressSource.MANUAL, 1, ::UserId.random(), OffsetDateTime.now()),
        LastKnownAddress(::LastKnownAddressId.random(), recallId, "Line 1", "Line 2", "Another Town", "AB12 3CD", AddressSource.LOOKUP, 2, ::UserId.random(), OffsetDateTime.now()),
      ),
      arrestIssues = false,
      bookingNumber = "1243A",
      contraband = true,
      inCustodyAtAssessment = false,
      inCustodyAtBooking = false,
      lastKnownAddressOption = LastKnownAddressOption.YES,
      lastReleaseDate = LocalDate.now(),
      lastReleasePrison = lastReleasePrisonId,
      licenceNameCategory = NameFormatCategory.FIRST_LAST,
      localPoliceForceId = localPoliceForceId,
      mappaLevel = MappaLevel.LEVEL_2,
      previousConvictionMainName = prevConsMainName,
      previousConvictionMainNameCategory = prevConsMainNameCategory,
      probationInfo = probationInfo,
      sentencingInfo = sentencingInfo,
      vulnerabilityDiversity = true
    )

    every { recallRepository.getByRecallId(recallId) } returns recall
    every { prisonLookupService.getPrisonName(lastReleasePrisonId) } returns lastReleasePrisonName
    every { courtLookupService.getCourtName(sentencingCourtId) } returns CourtName("County Court")
    every { policeForceLookupService.getPoliceForceName(localPoliceForceId) } returns PoliceForceName("Whatever Constabulary")
    every { userDetailsService.get(userIdGeneratingRecallNotification) } returns userDetails
    every { documentRepository.findByRecallIdAndCategoryAndVersion(recallId.value, RECALL_NOTIFICATION, 1) } returns null

    val result = underTest.createContext(recallId, userIdGeneratingRecallNotification).getRecallSummaryContext()

    assertThat(result.previousConvictionMainName, equalTo(expectedName))
    assertThat(result.inCustodyRecall, equalTo(false))
    assertThat(
      result.lastKnownAddress,
      equalTo(
        """
          |Line 1; A Town
          |Line 1; Line 2; Another Town; AB12 3CD
        """.trimMargin()
      )
    )
  }

  @Test
  fun `create OffenderNotificationContext with other reasons for recall detail added to end of list`() {
    val recallId = ::RecallId.random()
    val userIdGeneratingRecallNotification = ::UserId.random()
    val nomsNumber = NomsNumber("nomsNumber")
    val bookingNumber = "A12345"
    val lastReleasePrisonId = PrisonId("XXX")
    val lastReleasePrisonName = PrisonName("Last Prison Name")
    val probationInfo = ProbationInfo("", "", "", LocalDeliveryUnit.CENTRAL_AUDIT_TEAM, "")
    val sentencingCourtId = CourtId("ABCDE")
    val sentencingInfo =
      SentencingInfo(LocalDate.now(), LocalDate.now(), LocalDate.now(), sentencingCourtId, "", SentenceLength(3, 1, 0))
    val localPoliceForceId = PoliceForceId("XYZ")

    val recall = Recall(
      recallId,
      nomsNumber,
      ::UserId.random(),
      OffsetDateTime.now(),
      FirstName("Andy"),
      MiddleNames("Bertie"),
      LastName("Badger"),
      CroNumber("ABC/1234A"),
      LocalDate.of(1999, 12, 1),
      reasonsForRecall = setOf(ELM_FURTHER_OFFENCE, BREACH_EXCLUSION_ZONE, FAILED_HOME_VISIT, OTHER),
      bookingNumber = bookingNumber,
      inCustodyAtAssessment = false,
      inCustodyAtBooking = false,
      lastReleasePrison = lastReleasePrisonId,
      licenceNameCategory = NameFormatCategory.FIRST_LAST,
      localPoliceForceId = localPoliceForceId,
      probationInfo = probationInfo,
      reasonsForRecallOtherDetail = "Another reason",
      sentencingInfo = sentencingInfo,
    )

    every { recallRepository.getByRecallId(recallId) } returns recall
    every { prisonLookupService.getPrisonName(lastReleasePrisonId) } returns lastReleasePrisonName
    every { courtLookupService.getCourtName(sentencingCourtId) } returns CourtName("County Court")
    every { policeForceLookupService.getPoliceForceName(localPoliceForceId) } returns PoliceForceName("Whatever Constabulary")
    every { userDetailsService.get(userIdGeneratingRecallNotification) } returns userDetails
    every { documentRepository.findByRecallIdAndCategoryAndVersion(recallId.value, RECALL_NOTIFICATION, 1) } returns null

    val context = underTest.createContext(recallId, userIdGeneratingRecallNotification)
    assertThat(
      context.getOffenderNotificationContext(),
      equalTo(
        OffenderNotificationContext(
          FullName("Andy Badger"),
          bookingNumber,
          OffsetDateTime.now(fixedClock).toLocalDate(),
          listOf(
            "Breach of exclusion zone",
            "Electronic locking and monitoring (ELM) - Charged with a further offence - detected by ELM",
            "Failed home visit",
            "Another reason"
          )
        )
      )
    )
  }
}
