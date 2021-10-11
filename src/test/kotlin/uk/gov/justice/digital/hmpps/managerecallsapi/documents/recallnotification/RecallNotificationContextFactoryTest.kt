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
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.SearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.ProbationInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentenceLength
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentencingInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
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
import uk.gov.justice.digital.hmpps.managerecallsapi.service.PrisonLookupService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.UserDetailsService
import java.time.LocalDate
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RecallNotificationContextFactoryTest {
  private val recallRepository = mockk<RecallRepository>()
  private val prisonLookupService = mockk<PrisonLookupService>()
  private val prisonerOffenderSearchClient = mockk<PrisonerOffenderSearchClient>()
  private val userDetailsService = mockk<UserDetailsService>()

  private val underTest = RecallNotificationContextFactory(
    recallRepository, prisonLookupService, prisonerOffenderSearchClient, userDetailsService
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
    val prisoner = mockk<Prisoner>()
    val recall = Recall(recallId, nomsNumber, currentPrison = currentPrisonId, lastReleasePrison = lastReleasePrisonId)
    val userDetails = mockk<UserDetails>()

    every { recallRepository.getByRecallId(recallId) } returns recall
    every { prisonerOffenderSearchClient.prisonerSearch(SearchRequest(nomsNumber)) } returns Mono.just(listOf(prisoner))
    every { prisonLookupService.getPrisonName(currentPrisonId) } returns currentPrisonName
    every { prisonLookupService.getPrisonName(lastReleasePrisonId) } returns lastReleasePrisonName
    every { userDetailsService.get(userIdGeneratingRecallNotification) } returns userDetails

    val result = underTest.createContext(recallId, userIdGeneratingRecallNotification)

    assertThat(
      result,
      equalTo(RecallNotificationContext(recall, prisoner, userDetails, currentPrisonName, lastReleasePrisonName))
    )
  }

  private fun prevConsMainNameOptions(): Stream<Arguments> {
    return Stream.of(
      Arguments.of(true, "Bobby Badger", "Bobby Badger"),
      Arguments.of(false, "", "Bobbie Badger")
    )
  }

  @ParameterizedTest(name = "create RecallSummaryContext when hasPreviousConvictionMainName is {0}")
  @MethodSource("prevConsMainNameOptions")
  fun `create RecallSummaryContext with required details`(hasPrevConsMainName: Boolean, prevConsMainName: String, policeFileName: String) {
    val recallId = ::RecallId.random()
    val nomsNumber = NomsNumber("nomsNumber")
    val userIdGeneratingRecallNotification = ::UserId.random()
    val currentPrisonId = PrisonId("AAA")
    val currentPrisonName = PrisonName("Current Prison Name")
    val lastReleasePrisonId = PrisonId("XXX")
    val lastReleasePrisonName = PrisonName("Last Prison Name")
    val prisoner = Prisoner(
      firstName = "Bobbie",
      middleNames = "Bertrude",
      lastName = "Badger",
      dateOfBirth = LocalDate.of(2000, 1, 10)
    )
    val probationInfo = ProbationInfo("", "", "", LocalDeliveryUnit.CENTRAL_AUDIT_TEAM, "")
    val sentencingInfo =
      SentencingInfo(LocalDate.now(), LocalDate.now(), LocalDate.now(), "", "", SentenceLength(3, 1, 0))
    val recall = Recall(
      recallId,
      nomsNumber,
      currentPrison = currentPrisonId,
      lastReleasePrison = lastReleasePrisonId,
      mappaLevel = MappaLevel.LEVEL_2,
      probationInfo = probationInfo,
      sentencingInfo = sentencingInfo,
      hasOtherPreviousConvictionMainName = hasPrevConsMainName,
      previousConvictionMainName = prevConsMainName,
      bookingNumber = "1243A",
      lastReleaseDate = LocalDate.now(),
      localPoliceForce = "A Force",
      contraband = true,
      vulnerabilityDiversity = true,
    )
    val userDetails =
      UserDetails(::UserId.random(), FirstName("Sue"), LastName("Smith"), "", Email("s@smith.com"), PhoneNumber("0123"))

    every { recallRepository.getByRecallId(recallId) } returns recall
    every { prisonerOffenderSearchClient.prisonerSearch(SearchRequest(nomsNumber)) } returns Mono.just(listOf(prisoner))
    every { prisonLookupService.getPrisonName(currentPrisonId) } returns currentPrisonName
    every { prisonLookupService.getPrisonName(lastReleasePrisonId) } returns lastReleasePrisonName
    every { userDetailsService.get(userIdGeneratingRecallNotification) } returns userDetails

    val result = underTest.createContext(recallId, userIdGeneratingRecallNotification).getRecallSummaryContext()

    assertThat(result.previousConvictionMainName, equalTo(policeFileName))
  }
}