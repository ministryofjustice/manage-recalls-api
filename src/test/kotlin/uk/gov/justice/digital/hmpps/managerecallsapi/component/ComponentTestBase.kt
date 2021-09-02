package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.ninjasquad.springmockk.MockkBean
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.OK
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers.GotenbergMockServer
import uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers.HmppsAuthMockServer
import uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers.PrisonerOffenderSearchMockServer
import uk.gov.justice.digital.hmpps.managerecallsapi.storage.S3Service

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("db-test")
@TestInstance(PER_CLASS)
abstract class ComponentTestBase {

  @Autowired
  private lateinit var jwtAuthenticationHelper: JwtAuthenticationHelper

  @Autowired
  private lateinit var hmppsAuthMockServer: HmppsAuthMockServer

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var recallRepository: RecallRepository

  @Autowired
  protected lateinit var prisonerOffenderSearch: PrisonerOffenderSearchMockServer

  @Autowired
  protected lateinit var gotenbergMockServer: GotenbergMockServer

  @MockkBean
  protected lateinit var s3Service: S3Service

  protected val authenticatedClient: AuthenticatedClient by lazy {
    AuthenticatedClient(webTestClient, jwtAuthenticationHelper)
  }

  @BeforeAll
  fun startMocks() {
    hmppsAuthMockServer.start()
    prisonerOffenderSearch.start()
    gotenbergMockServer.start()
  }

  @AfterAll
  fun stopMocks() {
    hmppsAuthMockServer.stop()
    prisonerOffenderSearch.stop()
    gotenbergMockServer.stop()
  }

  @BeforeEach
  fun resetMocksAndStubClientToken() {
    hmppsAuthMockServer.resetAll()
    hmppsAuthMockServer.stubClientToken()
    prisonerOffenderSearch.resetAll()
  }

  @Configuration
  class TestConfig {
    @Bean
    fun cleanDatabase(): FlywayMigrationStrategy =
      FlywayMigrationStrategy { flyway ->
        flyway.clean()
        flyway.migrate()
      }
  }

  protected fun testJwt(role: String) = authenticatedClient.testJwt(role)

  protected fun unauthenticatedGet(path: String, expectedStatus: HttpStatus = OK): WebTestClient.BodyContentSpec =
    webTestClient.get().uri(path)
      .headers { it.add(CONTENT_TYPE, APPLICATION_JSON_VALUE) }
      .exchange()
      .expectStatus().isEqualTo(expectedStatus)
      .expectBody()
}
