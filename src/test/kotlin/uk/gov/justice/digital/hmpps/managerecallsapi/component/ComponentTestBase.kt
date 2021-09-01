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
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.AddDocumentRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.AddDocumentResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.GetDocumentResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Pdf
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.integration.JwtAuthenticationHelper
import uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers.GotenbergMockServer
import uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers.HmppsAuthMockServer
import uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers.PrisonerOffenderSearchMockServer
import uk.gov.justice.digital.hmpps.managerecallsapi.storage.S3Service
import java.util.UUID

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

  @Autowired
  lateinit var gotenbergMockServer: GotenbergMockServer

  @MockkBean
  lateinit var s3Service: S3Service

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

  protected fun authenticatedPatchRequest(path: String, request: Any): RecallResponse =
    sendAuthenticatedPatchRequestWithBody(path, request)
      .expectStatus().isOk
      .expectBody(RecallResponse::class.java)
      .returnResult()
      .responseBody!!

  protected fun authenticatedPostRequest(path: String, request: Any): RecallResponse =
    sendAuthenticatedPostRequestWithBody(path, request)
      .expectStatus().isCreated
      .expectBody(RecallResponse::class.java)
      .returnResult()
      .responseBody!!

  protected fun getRecall(recallId: RecallId): RecallResponse =
    authenticatedGetRequest("/recalls/$recallId", RecallResponse::class.java)

  protected fun getRevocationOrder(recallId: RecallId): Pdf =
    authenticatedGetRequest("/recalls/$recallId/revocationOrder", Pdf::class.java)

  protected fun getAllRecalls(): List<RecallResponse> =
    authenticatedGetRequest("/recalls", object : ParameterizedTypeReference<List<RecallResponse>>() {})

  protected fun uploadRecallDocument(recallId: RecallId, addDocumentRequest: AddDocumentRequest): AddDocumentResponse =
    authenticatedPostRequest("/recalls/$recallId/documents", addDocumentRequest, AddDocumentResponse::class.java)

  protected fun getRecallDocument(recallId: RecallId, documentId: UUID): GetDocumentResponse =
    authenticatedGetRequest("/recalls/$recallId/documents/$documentId", GetDocumentResponse::class.java)

  protected fun <T> authenticatedPostRequest(path: String, request: Any, responseClass: Class<T>): T =
    authenticatedPost(path, request)
      .expectBody(responseClass)
      .returnResult()
      .responseBody!!

  protected fun authenticatedPost(path: String, request: Any): WebTestClient.ResponseSpec = webTestClient
    .post()
    .uri(path)
    .bodyValue(request)
    .headers {
      it.add(CONTENT_TYPE, APPLICATION_JSON_VALUE)
      it.withBearerAuthToken(testJwt("ROLE_MANAGE_RECALLS"))
    }
    .exchange()
    .expectStatus().isCreated

  protected fun <T> authenticatedGetRequest(path: String, responseClass: Class<T>): T =
    authenticatedGet(path)
      .expectBody(responseClass)
      .returnResult()
      .responseBody!!

  protected fun <T> authenticatedGetRequest(path: String, responseClass: ParameterizedTypeReference<T>): T =
    authenticatedGet(path)
      .expectBody(responseClass)
      .returnResult()
      .responseBody!!

  protected fun authenticatedGet(path: String): WebTestClient.ResponseSpec = webTestClient.get().uri(path)
    .headers {
      it.add(CONTENT_TYPE, APPLICATION_JSON_VALUE)
      it.withBearerAuthToken(testJwt("ROLE_MANAGE_RECALLS"))
    }
    .exchange()
    .expectStatus().isOk

  protected fun authenticatedPostRequest(path: String, request: Any, expectedStatus: HttpStatus) {
    sendAuthenticatedPostRequestWithBody(path, request)
      .expectStatus().isEqualTo(expectedStatus)
  }
}
