package uk.gov.justice.digital.hmpps.managerecallsapi.service

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.PrisonLookupService
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.SearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
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

class LetterToPrisonContextFactoryTest {
  private val recallRepository = mockk<RecallRepository>()
  private val prisonLookupService = mockk<PrisonLookupService>()
  private val prisonerOffenderSearchClient = mockk<PrisonerOffenderSearchClient>()
  private val userDetailsService = mockk<UserDetailsService>()

  val underTest = LetterToPrisonContextFactory(recallRepository, prisonLookupService, prisonerOffenderSearchClient, userDetailsService)

  @Test
  fun `create LetterToPrisonContext with all required data`() {

    val recallId = ::RecallId.random()
    val assessedByUserId = ::UserId.random()
    val recall = Recall(recallId, NomsNumber("AA1234A"), currentPrison = PrisonId("WIM"), assessedByUserId = assessedByUserId)
    val prisoner = Prisoner()
    val userDetails = UserDetails(assessedByUserId, FirstName("Mandy"), LastName("Pandy"), "", Email("mandy@pandy.com"), PhoneNumber("09876543210"))

    every { recallRepository.getByRecallId(recallId) } returns recall
    every { prisonLookupService.getPrisonName(recall.currentPrison()!!) } returns PrisonName("WIM Prison")
    every { prisonerOffenderSearchClient.prisonerSearch(SearchRequest(recall.nomsNumber)) } returns Mono.just(listOf(prisoner))
    every { userDetailsService.get(recall.assessedByUserId()!!) } returns userDetails

    underTest.createContext(recallId)
  }
}
