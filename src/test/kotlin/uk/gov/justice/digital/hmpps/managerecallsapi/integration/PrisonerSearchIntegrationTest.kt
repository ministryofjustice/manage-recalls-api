package uk.gov.justice.digital.hmpps.managerecallsapi.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isEmpty
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockserver.client.MockServerClient
import org.mockserver.model.Header.header
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import org.mockserver.model.HttpStatusCode
import org.mockserver.model.HttpStatusCode.OK_200
import org.mockserver.model.HttpStatusCode.UNAUTHORIZED_401
import org.mockserver.springtest.MockServerTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders.ACCEPT
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Alias
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Offender
import uk.gov.justice.digital.hmpps.managerecallsapi.search.OffenderAlias
import uk.gov.justice.digital.hmpps.managerecallsapi.search.OffenderMatch
import uk.gov.justice.digital.hmpps.managerecallsapi.search.OffenderMatchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.search.OffenderMatches
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerMatch
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerMatchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerMatches
import uk.gov.justice.digital.hmpps.managerecallsapi.search.SearchAlias
import uk.gov.justice.digital.hmpps.managerecallsapi.search.SearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.search.SearchResult
import java.time.LocalDate
import java.time.Month
import java.util.Base64

@MockServerTest(
  "prisonerSearch.endpoint.url=http://localhost:\${mockServerPort}",
  "probationSearch.endpoint.url=http://localhost:\${mockServerPort}",
  "oauth.endpoint.url=http://localhost:\${mockServerPort}"
)
class PrisonerSearchIntegrationTest(
  @Autowired private val jwtAuthenticationHelper: JwtAuthenticationHelper,
  @Autowired private val objectMapper: ObjectMapper,
  @Value("\${spring.security.oauth2.client.registration.offender-search-client.client-id}") private val apiClientId: String,
  @Value("\${spring.security.oauth2.client.registration.offender-search-client.client-secret}") private val apiClientSecret: String
) :
  IntegrationTestBase() {

  private var mockServerClient: MockServerClient? = null

  private val apiClientJwt = jwtAuthenticationHelper.createTestJwt(subject = apiClientId)
  private val invalidUserJwt = jwtTokenWithRole("ROLE_UNKNOWN")
  private val validUserJwt = jwtTokenWithRole("ROLE_MANAGE_RECALLS")

  private val firstName = "John"
  private val middleNames = "Geoff"
  private val lastName = "Smith"
  private val dateOfBirth = LocalDate.of(1990, Month.FEBRUARY, 12)
  private val nomisNumber = "123456"
  private val aliasFirstName = "Bertie"
  private val aliasMiddleNames = "Jim"
  private val aliasLastName = "Badger"
  private val apiSearchRequest = SearchRequest(lastName)
  private val prisonerMatchRequest = PrisonerMatchRequest(null, lastName)
  private val offenderMatchRequest = OffenderMatchRequest(null, lastName)

  @BeforeEach
  fun stubJwt() {
    val oauthClientToken = Base64.getEncoder().encodeToString("$apiClientId:$apiClientSecret".toByteArray())
    mockServerClient?.`when`(
      request()
        .withMethod("POST")
        .withPath("/oauth/token")
        .withHeaders(
          header(AUTHORIZATION, "Basic $oauthClientToken")
        )
    )?.respond(
      response()
        .withStatusCode(OK_200.code())
        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
        .withBody(
          """{
              "token_type": "bearer",
              "access_token": "$apiClientJwt"
          }
          """.trimIndent()
        )
    )
  }

  @Test
  fun `should respond with 401 if user does not have the MANAGE_RECALLS role`() {
    sendAuthenticatedPostRequest("/search", apiSearchRequest, invalidUserJwt)
      .expectStatus().is4xxClientError
  }

  @Test
  fun `should handle unauthorized from prisoner search api`() {
    prisonerSearchRespondsWith(prisonerMatchRequest, UNAUTHORIZED_401)

    sendAuthenticatedPostRequest("/search", apiSearchRequest, validUserJwt)
      .expectStatus().is5xxServerError
  }

  @Test
  fun `can send search request to prisoner search api and retrieve no matches`() {
    prisonerSearchRespondsWith(prisonerMatchRequest, PrisonerMatches())
    probationSearchRespondsWith(offenderMatchRequest, OffenderMatches())

    val result = authenticatedPostRequest("/search", apiSearchRequest, validUserJwt)

    assertThat(result, isEmpty)
  }

  @Test
  fun `can send search request to prisoner search api and retrieve matches`() {
    val prisoner = Prisoner(
      prisonerNumber = nomisNumber,
      firstName = firstName,
      middleNames = middleNames,
      lastName = lastName,
      pncNumber = "pncNumber",
      croNumber = "croNumber",
      dateOfBirth = dateOfBirth,
      gender = "gender",
      status = "status",
      aliases = listOf(
        Alias(
          firstName = aliasFirstName,
          middleNames = aliasMiddleNames,
          lastName = aliasLastName,
          dateOfBirth = dateOfBirth,
          gender = "gender"
        )
      )
    )
    val offender = Offender(
      offenderId = 123L,
      firstName = firstName,
      middleNames = listOf(middleNames),
      lastName = lastName,
      dateOfBirth = dateOfBirth,
      gender = "gender",
      status = "status",
      aliases = listOf(
        OffenderAlias(
          firstName = aliasFirstName,
          middleNames = listOf(aliasMiddleNames),
          lastName = aliasLastName,
          dateOfBirth = dateOfBirth,
          gender = "gender"
        )
      )
    )
    prisonerSearchRespondsWith(
      prisonerMatchRequest,
      PrisonerMatches(listOf(PrisonerMatch(prisoner)))
    )
    probationSearchRespondsWith(
      offenderMatchRequest,
      OffenderMatches(listOf(OffenderMatch(offender)))
    )

    val response = authenticatedPostRequest("/search", apiSearchRequest, validUserJwt)

    assertThat(
      response,
      equalTo(
        listOf(
          SearchResult(
            firstName,
            lastName,
            nomisNumber,
            dateOfBirth,
            listOf(SearchAlias(aliasFirstName, aliasMiddleNames, aliasLastName, dateOfBirth))
          ),
          SearchResult(
            firstName,
            lastName,
            null,
            dateOfBirth,
            listOf(SearchAlias(aliasFirstName, aliasMiddleNames, aliasLastName, dateOfBirth))
          )
        )
      )
    )
  }

  private fun prisonerSearchRespondsWith(
    request: PrisonerMatchRequest,
    statusCode: HttpStatusCode
  ) {
    mockServerClient?.`when`(
      expectedPostRequest("/match-prisoners", request)
    )?.respond(
      response().withStatusCode(statusCode.code())
    )
  }

  private fun prisonerSearchRespondsWith(request: PrisonerMatchRequest, response: PrisonerMatches) {
    mockServerClient?.`when`(
      expectedPostRequest("/match-prisoners", request)
    )?.respond(
      response()
        .withStatusCode(OK_200.code())
        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
        .withBody(objectMapper.writeValueAsString(response))
    )
  }

  private fun probationSearchRespondsWith(request: OffenderMatchRequest, response: OffenderMatches) {
    mockServerClient?.`when`(
      expectedPostRequest("/match", request)
    )?.respond(
      response()
        .withStatusCode(OK_200.code())
        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
        .withBody(objectMapper.writeValueAsString(response))
    )
  }

  private fun expectedPostRequest(path: String, request: Any): HttpRequest? = request()
    .withMethod("POST")
    .withPath(path)
    .withHeaders(
      header(AUTHORIZATION, "Bearer $apiClientJwt"),
      header(CONTENT_TYPE, APPLICATION_JSON_VALUE),
      header(ACCEPT, APPLICATION_JSON_VALUE)
    )
    .withBody(objectMapper.writeValueAsString(request))

  private fun authenticatedPostRequest(
    path: String,
    request: Any,
    clientJwt: String
  ): List<SearchResult> {
    val responseType = object : ParameterizedTypeReference<List<SearchResult>>() {}
    return sendAuthenticatedPostRequest(path, request, clientJwt)
      .expectStatus().isOk
      .expectBody(responseType)
      .returnResult()
      .responseBody!!
  }

  private inline fun <reified T> sendAuthenticatedPostRequest(
    path: String,
    request: T,
    clientJwt: String
  ) = webTestClient.post()
    .uri(path)
    .body(Mono.just(request), T::class.java)
    .headers { it.add(AUTHORIZATION, "Bearer $clientJwt") }
    .exchange()

  private fun jwtTokenWithRole(role: String? = null) = jwtAuthenticationHelper.createTestJwt(role = role)
}
