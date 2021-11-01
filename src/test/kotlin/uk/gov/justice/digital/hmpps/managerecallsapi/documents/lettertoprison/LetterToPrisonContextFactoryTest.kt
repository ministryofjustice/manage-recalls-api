package uk.gov.justice.digital.hmpps.managerecallsapi.documents.lettertoprison

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.SearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallLengthDescription
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerOffenderSearchClient
import uk.gov.justice.digital.hmpps.managerecallsapi.service.PrisonLookupService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.UserDetailsService
import java.time.OffsetDateTime

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
    val recallLength = RecallLength.TWENTY_EIGHT_DAYS
    val recall = Recall(
      recallId, NomsNumber("AA1234A"), ::UserId.random(),
      OffsetDateTime.now(), OffsetDateTime.now(),
      lastReleasePrison = PrisonId("BOB"),
      currentPrison = PrisonId("WIM"),
      assessedByUserId = assessedByUserId,
      recallLength = recallLength
    )
    val prisoner = mockk<Prisoner>()
    val assessedByUserDetails = mockk<UserDetails>()
    val currentPrisonName = PrisonName("WIM Prison")
    val lastReleasePrisonName = PrisonName("Bobbins Prison")
    val recallLengthDescription = RecallLengthDescription(recallLength)

    every { recallRepository.getByRecallId(recallId) } returns recall
    every { prisonLookupService.getPrisonName(recall.currentPrison!!) } returns currentPrisonName
    every { prisonLookupService.getPrisonName(recall.lastReleasePrison!!) } returns lastReleasePrisonName
    every { prisonerOffenderSearchClient.prisonerSearch(SearchRequest(recall.nomsNumber)) } returns Mono.just(listOf(prisoner))
    every { userDetailsService.get(recall.assessedByUserId()!!) } returns assessedByUserDetails

    val result = underTest.createContext(recallId)

    assertThat(
      result,
      equalTo(
        LetterToPrisonContext(
          recall,
          prisoner,
          currentPrisonName,
          lastReleasePrisonName,
          recallLengthDescription,
          assessedByUserDetails
        )
      )
    )
  }
}
