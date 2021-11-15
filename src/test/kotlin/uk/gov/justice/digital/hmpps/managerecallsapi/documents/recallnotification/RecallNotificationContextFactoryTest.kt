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
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.LocalDeliveryUnit
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.PreviousConvictionMainNameCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.SearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.ProbationInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentenceLength
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentencingInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Email
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhoneNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerOffenderSearchClient
import uk.gov.justice.digital.hmpps.managerecallsapi.service.CourtLookupService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.PrisonLookupService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.UserDetailsService
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RecallNotificationContextFactoryTest {
  private val recallRepository = mockk<RecallRepository>()
  private val prisonLookupService = mockk<PrisonLookupService>()
  private val prisonerOffenderSearchClient = mockk<PrisonerOffenderSearchClient>()
  private val userDetailsService = mockk<UserDetailsService>()
  private val courtLookupService = mockk<CourtLookupService>()

  private val underTest = RecallNotificationContextFactory(
    recallRepository, prisonLookupService, prisonerOffenderSearchClient, userDetailsService, courtLookupService
  )

  @Test
  fun `create RecallNotificationContext with required details`() {
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
    val prisoner = mockk<Prisoner>()
    val recall = Recall(
      recallId,
      nomsNumber,
      ::UserId.random(),
      OffsetDateTime.now(),
      OffsetDateTime.now(),
      lastReleasePrison = lastReleasePrisonId,
      sentencingInfo = sentencingInfo,
      currentPrison = currentPrisonId
    )
    val userDetails = mockk<UserDetails>()

    every { sentencingInfo.sentencingCourt } returns sentencingCourtId
    every { recallRepository.getByRecallId(recallId) } returns recall
    every { prisonerOffenderSearchClient.prisonerSearch(SearchRequest(nomsNumber)) } returns Mono.just(listOf(prisoner))
    every { prisonLookupService.getPrisonName(currentPrisonId) } returns currentPrisonName
    every { prisonLookupService.getPrisonName(lastReleasePrisonId) } returns lastReleasePrisonName
    every { courtLookupService.getCourtName(sentencingCourtId) } returns sentencingCourtName
    every { userDetailsService.get(userIdGeneratingRecallNotification) } returns userDetails

    val result = underTest.createContext(recallId, userIdGeneratingRecallNotification)

    assertThat(
      result,
      equalTo(RecallNotificationContext(recall, prisoner, userDetails, currentPrisonName, lastReleasePrisonName, sentencingCourtName))
    )
  }

  private fun prevConsMainNameOptions(): Stream<Arguments> {
    return Stream.of(
      Arguments.of(true, "Bobby Badger", null, "Bobby Badger"),
      Arguments.of(true, "Bobby Badger", PreviousConvictionMainNameCategory.FIRST_LAST, "Andy Badger"),
      Arguments.of(true, "Bobby Badger", PreviousConvictionMainNameCategory.FIRST_MIDDLE_LAST, "Andy Bertie Badger"),
      Arguments.of(true, "Bobby Badger", PreviousConvictionMainNameCategory.OTHER, "Bobby Badger"),
      Arguments.of(false, "", null, "Andy Badger"),
      Arguments.of(false, "", PreviousConvictionMainNameCategory.FIRST_LAST, "Andy Badger"),
      Arguments.of(false, "", PreviousConvictionMainNameCategory.FIRST_MIDDLE_LAST, "Andy Bertie Badger"),
      Arguments.of(false, "Other Name", PreviousConvictionMainNameCategory.OTHER, "Other Name"),
      Arguments.of(null, "", PreviousConvictionMainNameCategory.FIRST_LAST, "Andy Badger"),
      Arguments.of(null, "", PreviousConvictionMainNameCategory.FIRST_MIDDLE_LAST, "Andy Bertie Badger"),
      Arguments.of(null, "", PreviousConvictionMainNameCategory.OTHER, ""),
    )
  }

  @ParameterizedTest(name = "create RecallSummaryContext when hasPreviousConvictionMainName is {0} & category is {2}")
  @MethodSource("prevConsMainNameOptions")
  fun `create RecallSummaryContext with required details`(hasPrevConsMainName: Boolean?, prevConsMainName: String?, prevConsMainNameCategory: PreviousConvictionMainNameCategory?, expectedName: String) {
    val recallId = ::RecallId.random()
    val nomsNumber = NomsNumber("nomsNumber")
    val userIdGeneratingRecallNotification = ::UserId.random()
    val currentPrisonId = PrisonId("AAA")
    val currentPrisonName = PrisonName("Current Prison Name")
    val lastReleasePrisonId = PrisonId("XXX")
    val lastReleasePrisonName = PrisonName("Last Prison Name")
    val prisoner = Prisoner(
      firstName = "Andy",
      middleNames = "Bertie",
      lastName = "Badger",
      dateOfBirth = LocalDate.of(2000, 1, 10)
    )
    val probationInfo = ProbationInfo("", "", "", LocalDeliveryUnit.CENTRAL_AUDIT_TEAM, "")
    val sentencingCourtId = CourtId("ABCDE")
    val sentencingInfo =
      SentencingInfo(LocalDate.now(), LocalDate.now(), LocalDate.now(), sentencingCourtId, "", SentenceLength(3, 1, 0))
    val recall = Recall(
      recallId, nomsNumber, ::UserId.random(), OffsetDateTime.now(), OffsetDateTime.now(),
      lastReleasePrison = lastReleasePrisonId,
      lastReleaseDate = LocalDate.now(),
      localPoliceForce = "A Force",
      contraband = true,
      vulnerabilityDiversity = true,
      mappaLevel = MappaLevel.LEVEL_2,
      sentencingInfo = sentencingInfo,
      bookingNumber = "1243A",
      probationInfo = probationInfo,
      currentPrison = currentPrisonId,
      hasOtherPreviousConvictionMainName = hasPrevConsMainName,
      previousConvictionMainNameCategory = prevConsMainNameCategory,
      previousConvictionMainName = prevConsMainName,
    )
    val userDetails =
      UserDetails(
        ::UserId.random(), FirstName("Sue"), LastName("Smith"), "", Email("s@smith.com"), PhoneNumber("0123"),
        OffsetDateTime.now()
      )

    every { recallRepository.getByRecallId(recallId) } returns recall
    every { prisonerOffenderSearchClient.prisonerSearch(SearchRequest(nomsNumber)) } returns Mono.just(listOf(prisoner))
    every { prisonLookupService.getPrisonName(currentPrisonId) } returns currentPrisonName
    every { prisonLookupService.getPrisonName(lastReleasePrisonId) } returns lastReleasePrisonName
    every { courtLookupService.getCourtName(sentencingCourtId) } returns CourtName("County Court")
    every { userDetailsService.get(userIdGeneratingRecallNotification) } returns userDetails

    val result = underTest.createContext(recallId, userIdGeneratingRecallNotification).getRecallSummaryContext()

    assertThat(result.previousConvictionMainName, equalTo(expectedName))
  }
}
