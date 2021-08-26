package uk.gov.justice.digital.hmpps.managerecallsapi.component

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.integration.JwtAuthenticationHelper
import uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers.HmppsAuthMockServer
import uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers.PrisonerOffenderSearchMockServer

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("db-test")
@TestInstance(PER_CLASS)
abstract class ComponentTestBase {

  @Autowired
  protected lateinit var recallRepository: RecallRepository

  @Autowired
  lateinit var jwtAuthenticationHelper: JwtAuthenticationHelper

  @Autowired
  lateinit var hmppsAuthMockServer: HmppsAuthMockServer

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  lateinit var prisonerOffenderSearch: PrisonerOffenderSearchMockServer

  @BeforeAll
  fun startMocks() {
    hmppsAuthMockServer.start()
    prisonerOffenderSearch.start()
  }

  @AfterAll
  fun stopMocks() {
    hmppsAuthMockServer.stop()
    prisonerOffenderSearch.stop()
  }

  @BeforeEach
  fun resetMocksAndStubClientToken() {
    hmppsAuthMockServer.resetAll()
    hmppsAuthMockServer.stubClientToken()
  }

  protected fun testJwt(role: String) = jwtAuthenticationHelper.createTestJwt(role = role)

  protected final inline fun <reified T> sendAuthenticatedPatchRequestWithBody(
    path: String,
    request: T
  ): WebTestClient.ResponseSpec =
    webTestClient.patch().sendAuthenticatedRequestWithBody(path, request)

  protected final inline fun <reified T> WebTestClient.RequestBodyUriSpec.sendAuthenticatedRequestWithBody(
    path: String,
    request: T,
    userJwt: String = testJwt("ROLE_MANAGE_RECALLS")
  ): WebTestClient.ResponseSpec =
    this.uri(path)
      .body(Mono.just(request), T::class.java)
      .headers {
        it.add(CONTENT_TYPE, APPLICATION_JSON_VALUE)
        it.withBearerAuthToken(userJwt)
      }
      .exchange()

  protected final inline fun <reified T> sendAuthenticatedPostRequestWithBody(
    path: String,
    request: T
  ): WebTestClient.ResponseSpec =
    webTestClient.post().sendAuthenticatedRequestWithBody(path, request)

  fun HttpHeaders.withBearerAuthToken(jwt: String) = this.add(AUTHORIZATION, "Bearer $jwt")
}
