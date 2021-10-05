package uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier

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
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerOffenderSearchClient

class DossierContextFactoryTest {
  private val recallRepository = mockk<RecallRepository>()
  private val prisonLookupService = mockk<PrisonLookupService>()
  private val prisonerOffenderSearchClient = mockk<PrisonerOffenderSearchClient>()

  private val underTest = DossierContextFactory(
    recallRepository, prisonLookupService, prisonerOffenderSearchClient
  )

  @Test
  fun `create DossierContextFactory with required details`() {
    val recallId = ::RecallId.random()
    val nomsNumber = NomsNumber("nomsNumber")
    val currentPrison = PrisonId("AAA")
    val currentPrisonName = PrisonName("Current Prison Name")
    val prisoner = mockk<Prisoner>()
    val recall = Recall(recallId, nomsNumber, currentPrison = currentPrison)

    every { recallRepository.getByRecallId(recallId) } returns recall
    every { prisonerOffenderSearchClient.prisonerSearch(SearchRequest(nomsNumber)) } returns Mono.just(listOf(prisoner))
    every { prisonLookupService.getPrisonName(currentPrison) } returns currentPrisonName

    val result = underTest.createContext(recallId)

    assertThat(result, equalTo(DossierContext(recall, prisoner, currentPrisonName)))
  }
}
