package uk.gov.justice.digital.hmpps.managerecallsapi.integration

import com.natpryce.hamkrest.allOf
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.isEmpty
import com.natpryce.hamkrest.present
import org.junit.jupiter.api.Test
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.UNAUTHORIZED
import uk.gov.justice.digital.hmpps.managerecallsapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.SearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.SearchResult
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerSearchRequest

class PrisonerSearchIntegrationTest : IntegrationTestBase() {

  private val firstName = "John"
  private val middleNames = "Geoff"
  private val lastName = "Smith"

  private val nomsNumber = NomsNumber("123456")
  private val apiSearchRequest = SearchRequest(nomsNumber)
  private val prisonerSearchRequest = PrisonerSearchRequest(nomsNumber)

  @Test
  fun `should handle unauthorized from prisoner search api`() {
    prisonerOffenderSearch.prisonerSearchRespondsWith(prisonerSearchRequest, UNAUTHORIZED)

    sendAuthenticatedPostRequestWithBody("/search", apiSearchRequest)
      .expectStatus().is5xxServerError
  }

  @Test
  fun `can send search request to prisoner search api and retrieve no matches`() {
    prisonerOffenderSearch.prisonerSearchRespondsWith(prisonerSearchRequest, emptyList())

    val responseBody = authenticatedPostRequest("/search", apiSearchRequest)

    assertThat(responseBody, isEmpty)
  }

  @Test
  fun `search request with blank noms number returns 400`() {
    val result = sendAuthenticatedPostRequestWithBody("/search", "{\"nomsNumber\":\"\"}")
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult()

    assertThat(
      result.responseBody,
      present(
        allOf(
          has(ErrorResponse::status, equalTo(BAD_REQUEST)),
          has(
            ErrorResponse::message,
            present(containsSubstring("'' violated uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber rule"))
          )
        )
      )
    )
  }

  @Test
  fun `can send search request to prisoner search api and retrieve matches`() {
    val prisoner1 = testPrisoner(nomsNumber, firstName, lastName)
    val prisoner2 = testPrisoner(null, firstName, lastName)
    prisonerOffenderSearch.prisonerSearchRespondsWith(
      prisonerSearchRequest,
      listOf(
        prisoner1,
        prisoner2
      )
    )

    val response = authenticatedPostRequest("/search", apiSearchRequest)

    assertThat(
      response,
      equalTo(
        listOf(
          searchResult(prisoner1),
          searchResult(prisoner2),
        )
      )
    )
  }

  private fun searchResult(p: Prisoner): SearchResult = SearchResult(
    p.firstName,
    p.middleNames,
    p.lastName,
    p.dateOfBirth,
    p.gender,
    p.prisonerNumber,
    p.pncNumber,
    p.croNumber,
  )

  private fun testPrisoner(nomsNumber: NomsNumber?, firstName: String?, lastName: String?) = Prisoner(
    firstName = firstName,
    middleNames = middleNames,
    lastName = lastName,
    dateOfBirth = randomAdultDateOfBirth(),
    gender = randomString(),
    prisonerNumber = nomsNumber?.value,
    pncNumber = randomString(),
    croNumber = randomString(),
  )

  private fun authenticatedPostRequest(path: String, request: Any): List<SearchResult> =
    sendAuthenticatedPostRequestWithBody(path, request)
      .expectStatus().isOk
      .expectBody(object : ParameterizedTypeReference<List<SearchResult>>() {})
      .returnResult()
      .responseBody!!
}
