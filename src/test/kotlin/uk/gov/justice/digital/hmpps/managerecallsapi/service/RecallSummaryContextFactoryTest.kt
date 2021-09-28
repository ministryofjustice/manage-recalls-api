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
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerOffenderSearchClient

class RecallSummaryContextFactoryTest {
  private val recallRepository = mockk<RecallRepository>()
  private val prisonLookupService = mockk<PrisonLookupService>()
  private val prisonerOffenderSearchClient = mockk<PrisonerOffenderSearchClient>()

  private val underTest = RecallSummaryContextFactory(
    recallRepository,
    prisonLookupService,
    prisonerOffenderSearchClient
  )

  private val recallId = ::RecallId.random()

  @Test
  fun `context contains required recall and prisoner information`() {
    val nomsNumber = NomsNumber("nomsNumber")
    val currentPrison = "AAA"
    val lastReleasePrison = "ZZZ"
    val prisoner = mockk<Prisoner>()
    val recall = Recall(recallId, nomsNumber, currentPrison = currentPrison, lastReleasePrison = lastReleasePrison)
    val currentPrisonName = "Current Prison Name"
    val lastReleasePrisonName = "Last Release Prison Name"

    every { recallRepository.getByRecallId(recallId) } returns recall
    every { prisonLookupService.getPrisonName(currentPrison) } returns currentPrisonName
    every { prisonLookupService.getPrisonName(lastReleasePrison) } returns lastReleasePrisonName
    every { prisonerOffenderSearchClient.prisonerSearch(SearchRequest(nomsNumber)) } returns Mono.just(listOf(prisoner))

    val result = underTest.createRecallSummaryContext(recallId).block()!!

    assertThat(
      result,
      equalTo(
        RecallSummaryContext(recall, prisoner, lastReleasePrisonName, currentPrisonName)
      )
    )
  }
}
