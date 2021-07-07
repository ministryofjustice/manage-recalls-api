package uk.gov.justice.digital.hmpps.managerecallsapi.integration

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers.HmppsAuthMockServer
import uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers.PrisonerOffenderSearchMockServer

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class IntegrationTestBase {

  @Autowired
  lateinit var jwtAuthenticationHelper: JwtAuthenticationHelper
  @Autowired
  lateinit var prisonerOffenderSearch: PrisonerOffenderSearchMockServer
  @Autowired
  lateinit var hmppsAuthMockServer: HmppsAuthMockServer
  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired lateinit var webTestClient: WebTestClient

  @BeforeAll
  fun startMocks() {
    prisonerOffenderSearch.start()
    hmppsAuthMockServer.start()
  }

  @AfterAll
  fun stopMocks() {
    prisonerOffenderSearch.stop()
    hmppsAuthMockServer.stop()
  }

  @BeforeEach
  fun resetMocksAndStubClientToken() {
    prisonerOffenderSearch.resetAll()
    hmppsAuthMockServer.resetAll()
    hmppsAuthMockServer.stubClientToken()
  }

  protected fun testJwt(role: String) = jwtAuthenticationHelper.createTestJwt(role = role)

  protected final fun sendAuthenticatedPostRequest(
    path: String,
    userJwt: String = testJwt("ROLE_MANAGE_RECALLS")
  ): WebTestClient.ResponseSpec =
    webTestClient.post()
      .uri(path)
      .headers { it.withBearerAuthToken(userJwt) }
      .exchange()

  protected final inline fun <reified T> sendAuthenticatedPostRequestWithBody(
    path: String,
    request: T,
    userJwt: String = testJwt("ROLE_MANAGE_RECALLS")
  ): WebTestClient.ResponseSpec =
    webTestClient.post()
      .uri(path)
      .body(Mono.just(request), T::class.java)
      .headers { it.withBearerAuthToken(userJwt) }
      .exchange()

  fun HttpHeaders.withBearerAuthToken(jwt: String) = this.add(AUTHORIZATION, "Bearer $jwt")
}
