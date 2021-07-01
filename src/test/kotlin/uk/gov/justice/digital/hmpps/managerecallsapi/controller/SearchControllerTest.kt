package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isEmpty
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.SearchController
import uk.gov.justice.digital.hmpps.managerecallsapi.SearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.SearchResult
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerOffenderSearchClient
import java.time.LocalDate

class SearchControllerTest {

  private val prisonerOffenderSearchClient = mockk<PrisonerOffenderSearchClient>()

  private val underTest = SearchController(prisonerOffenderSearchClient)

  private val nomsNumber = "A1234AA"
  private val searchRequest = SearchRequest(nomsNumber)

  @Test
  fun `prisonerSearch returns null results`() {
    every { prisonerOffenderSearchClient.prisonerSearch(searchRequest) } returns null

    val results = underTest.prisonerSearch(searchRequest)

    assertThat(results.body, isEmpty)
  }

  @Test
  fun `prisonerSearch returns empty results`() {
    every { prisonerOffenderSearchClient.prisonerSearch(searchRequest) } returns emptyList()

    val results = underTest.prisonerSearch(searchRequest)

    assertThat(results.body, isEmpty)
  }

  @Test
  fun `prisonerSearch returns prisoner results`() {
    val prisoner1 = prisoner("A1234AA", "Jim", "Smith", LocalDate.of(1994, 10, 15))
    val prisoner2 = prisoner("A1234ZZ", "Bob", "Smith", LocalDate.of(1994, 10, 16))
    every { prisonerOffenderSearchClient.prisonerSearch(searchRequest) } returns listOf(prisoner1, prisoner2)

    val results = underTest.prisonerSearch(searchRequest)

    assertThat(
      results.body,
      equalTo(
        listOf(
          SearchResult(prisoner1.firstName, prisoner1.lastName, prisoner1.prisonerNumber, prisoner1.dateOfBirth),
          SearchResult(prisoner2.firstName, prisoner2.lastName, prisoner2.prisonerNumber, prisoner2.dateOfBirth)
        )
      )
    )
  }

  private fun prisoner(nomsNumber: String?, firstName: String?, lastName: String?, dateOfBirth: LocalDate?) = Prisoner(
    prisonerNumber = nomsNumber,
    firstName = firstName,
    lastName = lastName,
    dateOfBirth = dateOfBirth,
  )
}
