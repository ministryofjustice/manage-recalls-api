package uk.gov.justice.digital.hmpps.managerecallsapi.service

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.PrisonLookupService
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.SearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerOffenderSearchClient

class LetterToPrisonContextFactoryTest {
  private val recallRepository = mockk<RecallRepository>()
  private val prisonLookupService = mockk<PrisonLookupService>()
  private val prisonerOffenderSearchClient = mockk<PrisonerOffenderSearchClient>()

  val underTest = LetterToPrisonContextFactory(recallRepository, prisonLookupService, prisonerOffenderSearchClient)

  @Test
  fun `create LetterToPrisonContext with all required data`() {

    val recallId = ::RecallId.random()
    val recall = Recall(recallId, NomsNumber("AA1234A"), currentPrison = "WIM")

    every { recallRepository.getByRecallId(recallId) } returns recall
    every { prisonLookupService.getPrisonName(recall.currentPrison!!) } returns PrisonName("WIM Prison")
    val prisoner = Prisoner()
    every { prisonerOffenderSearchClient.prisonerSearch(SearchRequest(recall.nomsNumber)) } returns Mono.just(listOf(prisoner))

    underTest.createContext(recallId)
  }
}
