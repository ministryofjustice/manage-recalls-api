package uk.gov.justice.digital.hmpps.managerecallsapi.integration

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isEmpty
import org.junit.jupiter.api.Test
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.UNAUTHORIZED
import uk.gov.justice.digital.hmpps.managerecallsapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.SearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.SearchResult
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerSearchRequest
import java.time.LocalDate
import java.time.Month

class PrisonerSearchIntegrationTest : IntegrationTestBase() {

  private val firstName = "John"
  private val middleNames = "Geoff"
  private val lastName = "Smith"
  private val dateOfBirth = LocalDate.of(1990, Month.FEBRUARY, 12)
  private val nomsNumber = "123456"
  private val apiSearchRequest = SearchRequest(nomsNumber)
  private val prisonerSearchRequest = PrisonerSearchRequest(nomsNumber)

  @Test
  fun `should respond with 401 if user does not have the MANAGE_RECALLS role`() {
    val invalidUserJwt = testJwt("ROLE_UNKNOWN")
    sendAuthenticatedPostRequestWithBody("/search", apiSearchRequest, invalidUserJwt)
      .expectStatus().isUnauthorized
  }

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
    val result = sendAuthenticatedPostRequestWithBody("/search", SearchRequest(""))
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult()

    assertThat(
      result.responseBody,
      equalTo(ErrorResponse(BAD_REQUEST, "nomsNumber: must not be blank"))
    )
  }

  @Test
  fun `can send search request to prisoner search api and retrieve matches`() {
    prisonerOffenderSearch.prisonerSearchRespondsWith(
      prisonerSearchRequest,
      listOf(
        testPrisoner(nomsNumber, firstName, lastName),
        testPrisoner(null, firstName, lastName)
      )
    )

    val response = authenticatedPostRequest("/search", apiSearchRequest)

    assertThat(response, equalTo(
        listOf(
          SearchResult(firstName, lastName, nomsNumber, dateOfBirth),
          SearchResult(firstName, lastName, null, dateOfBirth)
        )
      )
    )
  }

  private fun testPrisoner(nomsNumber: String?, firstName: String?, lastName: String?) = Prisoner(
    prisonerNumber = nomsNumber,
    firstName = firstName,
    middleNames = middleNames,
    lastName = lastName,
    pncNumber = "pncNumber",
    croNumber = "croNumber",
    dateOfBirth = dateOfBirth,
    gender = "gender",
    status = "status",
  )

  private fun authenticatedPostRequest(path: String, request: Any): List<SearchResult> =
    sendAuthenticatedPostRequestWithBody(path, request)
      .expectStatus().isOk
      .expectBody(object : ParameterizedTypeReference<List<SearchResult>>() {})
      .returnResult()
      .responseBody!!
}
