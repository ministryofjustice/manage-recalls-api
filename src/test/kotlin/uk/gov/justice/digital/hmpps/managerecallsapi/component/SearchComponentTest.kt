package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isEmpty
import org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.GATEWAY_TIMEOUT
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.UNAUTHORIZED
import uk.gov.justice.digital.hmpps.managerecallsapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.SearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.SearchResult
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomAdultDateOfBirth
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomNoms
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerSearchRequest

class SearchComponentTest : ComponentTestBase() {

  private val nomsNumber = NomsNumber("123456")
  private val apiSearchRequest = SearchRequest(nomsNumber)
  private val prisonerSearchRequest = PrisonerSearchRequest(nomsNumber)

  @Test
  fun `returns 500 if prisoner search api returns unauthorized`() {
    prisonerOffenderSearchMockServer.prisonerSearchRespondsWith(prisonerSearchRequest, UNAUTHORIZED)

    val result = authenticatedClient.post("/search", "{\"nomsNumber\":\"123456\"}")
      .expectStatus().isEqualTo(INTERNAL_SERVER_ERROR)
      .expectBody(ErrorResponse::class.java).returnResult().responseBody!!

    assertThat(
      result,
      equalTo(
        ErrorResponse(
          INTERNAL_SERVER_ERROR,
          "ClientException: PrisonerOffenderSearchClient: [401 Unauthorized from POST http://localhost:9092/prisoner-search/match-prisoners]"
        )
      )
    )
  }

  @Test
  fun `can send search request to prisoner search api and retrieve no matches`() {
    prisonerOffenderSearchMockServer.prisonerSearchRespondsWith(prisonerSearchRequest, emptyList())

    val responseBody = authenticatedClient.search(apiSearchRequest)

    assertThat(responseBody, isEmpty)
  }

  @Test
  fun `search request with blank noms number returns 400`() {
    val result = authenticatedClient.post("/search", "{\"nomsNumber\":\"\"}")
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    assertThat(
      result,
      equalTo(
        ErrorResponse(
          BAD_REQUEST,
          "JSON parse error: '' violated uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber rule; nested exception is com.fasterxml.jackson.databind.JsonMappingException: '' violated uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber rule (through reference chain: uk.gov.justice.digital.hmpps.managerecallsapi.controller.SearchRequest[\"nomsNumber\"])"
        )
      )
    )
  }

  @Test
  fun `can send search request to prisoner search api and retrieve matches`() {
    val prisoner1 = testPrisoner(nomsNumber)
    val prisoner2 = testPrisoner(null)
    prisonerOffenderSearchMockServer.prisonerSearchRespondsWith(prisonerSearchRequest, listOf(prisoner1, prisoner2))

    val response = authenticatedClient.search(apiSearchRequest)

    assertThat(response, equalTo(listOf(prisoner1.searchResult(), prisoner2.searchResult())))
  }

  @Test
  fun `prisoner offender timeout is handled gracefully`() {
    val nomsNumber = randomNoms()
    prisonerOffenderSearchMockServer.delaySearch(PrisonerSearchRequest(nomsNumber), 3000)

    val result = authenticatedClient.search(SearchRequest(nomsNumber), GATEWAY_TIMEOUT)
      .expectBody(ErrorResponse::class.java).returnResult().responseBody!!

    assertThat(
      result,
      equalTo(ErrorResponse(GATEWAY_TIMEOUT, "PrisonerOffenderSearchClient: [java.util.concurrent.TimeoutException]"))
    )
  }

  private fun Prisoner.searchResult(): SearchResult = SearchResult(
    firstName,
    middleNames,
    lastName,
    dateOfBirth,
    gender,
    prisonerNumber,
    pncNumber,
    croNumber,
  )

  private fun testPrisoner(nomsNumber: NomsNumber?) = Prisoner(
    prisonerNumber = nomsNumber?.value,
    pncNumber = randomAlphanumeric(1, 32),
    croNumber = randomAlphanumeric(1, 32),
    firstName = randomAlphanumeric(1, 32),
    middleNames = randomAlphanumeric(1, 32),
    lastName = randomAlphanumeric(1, 32),
    dateOfBirth = randomAdultDateOfBirth(),
    gender = randomAlphanumeric(1, 32),
  )
}
