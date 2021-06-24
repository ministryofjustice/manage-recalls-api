package uk.gov.justice.digital.hmpps.managerecallsapi.integration

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isEmpty
import org.junit.jupiter.api.Test
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpStatus.UNAUTHORIZED
import uk.gov.justice.digital.hmpps.managerecallsapi.SearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.SearchResult
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
    val invalidUserJwt = jwtAuthenticationHelper.createTestJwt(role = "ROLE_UNKNOWN")
    sendAuthenticatedPostRequestWithBody("/search", apiSearchRequest, invalidUserJwt)
      .expectStatus().isUnauthorized
  }

  @Test
  fun `should handle unauthorized from prisoner search api`() {
    prisonerOffenderSearch.prisonerSearchRespondsWith(prisonerSearchRequest, UNAUTHORIZED)

    sendAuthenticatedPostRequestWithBody(
      "/search",
      apiSearchRequest,
      jwtAuthenticationHelper.createTestJwt(role = "ROLE_MANAGE_RECALLS")
    )
      .expectStatus().is5xxServerError
  }

  @Test
  fun `can send search request to prisoner search api and retrieve no matches`() {
    prisonerOffenderSearch.prisonerSearchRespondsWith(prisonerSearchRequest, emptyList())

    val result = authenticatedPostRequest(
      "/search",
      apiSearchRequest,
      jwtAuthenticationHelper.createTestJwt(role = "ROLE_MANAGE_RECALLS")
    )

    assertThat(result, isEmpty)
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

    val response = authenticatedPostRequest(
      "/search",
      apiSearchRequest,
      jwtAuthenticationHelper.createTestJwt(role = "ROLE_MANAGE_RECALLS")
    )

    assertThat(
      response,
      equalTo(
        listOf(
          SearchResult(
            firstName,
            lastName,
            nomsNumber,
            dateOfBirth,
          ),
          SearchResult(
            firstName,
            lastName,
            null,
            dateOfBirth,
          )
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

  private fun authenticatedPostRequest(
    path: String,
    request: Any,
    userJwt: String
  ): List<SearchResult> {
    val responseType = object : ParameterizedTypeReference<List<SearchResult>>() {}
    return sendAuthenticatedPostRequestWithBody(path, request, userJwt)
      .expectStatus().isOk
      .expectBody(responseType)
      .returnResult()
      .responseBody!!
  }
}
