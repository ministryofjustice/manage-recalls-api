package uk.gov.justice.digital.hmpps.managerecallsapi.integration

import com.ninjasquad.springmockk.MockkBean
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers.GotenbergMockServer
import uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers.HmppsAuthMockServer
import uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers.PrisonerOffenderSearchMockServer
import uk.gov.justice.digital.hmpps.managerecallsapi.storage.S3Service
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class IntegrationTestBase {

  @MockkBean
  lateinit var s3Service: S3Service

  @Autowired
  lateinit var jwtAuthenticationHelper: JwtAuthenticationHelper

  @Autowired
  lateinit var prisonerOffenderSearch: PrisonerOffenderSearchMockServer

  @Autowired
  lateinit var gotenbergMockServer: GotenbergMockServer

  @Autowired
  lateinit var hmppsAuthMockServer: HmppsAuthMockServer

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var webTestClient: WebTestClient

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

  protected final inline fun <reified T> sendAuthenticatedPostRequestWithBody(
    path: String,
    request: T,
    userJwt: String = testJwt("ROLE_MANAGE_RECALLS")
  ): WebTestClient.ResponseSpec =
    webTestClient.post()
      .uri(path)
      .body(Mono.just(request), T::class.java)
      .headers {
        it.add(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        it.withBearerAuthToken(userJwt)
      }
      .exchange()

  fun HttpHeaders.withBearerAuthToken(jwt: String) = this.add(AUTHORIZATION, "Bearer $jwt")

  protected fun randomString() = UUID.randomUUID().toString()

  protected fun randomAdultDateOfBirth(): LocalDate? {
    val age18 = LocalDate.now().minusYears(18)
    val endEpochDay = age18.toEpochDay()
    val startEpochDay = age18.minusYears(80).toEpochDay()
    val randomDay = ThreadLocalRandom.current().nextLong(startEpochDay, endEpochDay)
    return LocalDate.ofEpochDay(randomDay)
  }
}
