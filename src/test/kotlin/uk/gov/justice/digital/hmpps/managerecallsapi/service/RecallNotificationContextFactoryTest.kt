package uk.gov.justice.digital.hmpps.managerecallsapi.service

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.PrisonLookupService
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.SearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerOffenderSearchClient

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
    val currentPrison = "AAA"
    val currentPrisonName = "Current Prison Name"
    val lastReleasePrison = "XXX"
    val lastReleasePrisonName = "Last Prison Name"
    val prisoner = mockk<Prisoner>()
    val recall = Recall(recallId, nomsNumber, currentPrison = currentPrison, lastReleasePrison = lastReleasePrison)
    val userDetails = mockk<UserDetails>()

    every { recallRepository.getByRecallId(recallId) } returns recall
    every { prisonerOffenderSearchClient.prisonerSearch(SearchRequest(nomsNumber)) } returns Mono.just(listOf(prisoner))
    every { prisonLookupService.getPrisonName(currentPrison) } returns currentPrisonName
    every { prisonLookupService.getPrisonName(lastReleasePrison) } returns lastReleasePrisonName
    every { userDetailsService.get(userIdGeneratingRecallNotification) } returns userDetails

    val result = underTest.createContext(recallId, userIdGeneratingRecallNotification)

    assertThat(result, equalTo(RecallNotificationContext(recall, prisoner, userDetails, currentPrisonName, lastReleasePrisonName)))
  }
}
