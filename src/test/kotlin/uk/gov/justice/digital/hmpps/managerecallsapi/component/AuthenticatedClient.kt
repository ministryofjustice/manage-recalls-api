package uk.gov.justice.digital.hmpps.managerecallsapi.component

import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.OK
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.AddDocumentRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.AddDocumentResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.BookRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.GetDocumentResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Pdf
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.SearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.SearchResult
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UpdateRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.integration.JwtAuthenticationHelper
import java.util.UUID

interface ManageRecallsApi {
  fun post(path: String, json: String): WebTestClient.ResponseSpec

  fun patch(path: String, json: String): WebTestClient.ResponseSpec

  fun bookRecall(bookRecallRequest: BookRecallRequest): RecallResponse

  fun getRecall(recallId: RecallId): RecallResponse

  fun getAllRecalls(): List<RecallResponse>

  fun getRevocationOrder(recallId: RecallId): Pdf

  fun uploadRecallDocument(recallId: RecallId, addDocumentRequest: AddDocumentRequest): AddDocumentResponse

  fun getRecallDocument(recallId: RecallId, documentId: UUID): GetDocumentResponse

  fun updateRecall(recallId: RecallId, updateRecallRequest: UpdateRecallRequest): RecallResponse

  fun search(searchRequest: SearchRequest): List<SearchResult>
}

class AuthenticatedClient(
  private val webTestClient: WebTestClient,
  private val jwtAuthenticationHelper: JwtAuthenticationHelper
) : ManageRecallsApi {
  override fun post(path: String, json: String): WebTestClient.ResponseSpec =
    sendAuthenticatedPostRequestWithBody(path, json)

  override fun patch(path: String, json: String): WebTestClient.ResponseSpec =
    sendAuthenticatedPatchRequestWithBody(path, json)

  override fun bookRecall(bookRecallRequest: BookRecallRequest): RecallResponse =
    authenticatedPostRequest("/recalls", bookRecallRequest, RecallResponse::class.java)

  override fun getRecall(recallId: RecallId): RecallResponse =
    authenticatedGetRequest("/recalls/$recallId", RecallResponse::class.java)

  override fun getAllRecalls(): List<RecallResponse> =
    authenticatedGetRequest("/recalls", object : ParameterizedTypeReference<List<RecallResponse>>() {})

  override fun getRevocationOrder(recallId: RecallId): Pdf =
    authenticatedGetRequest("/recalls/$recallId/revocationOrder", Pdf::class.java)

  override fun uploadRecallDocument(recallId: RecallId, addDocumentRequest: AddDocumentRequest): AddDocumentResponse =
    authenticatedPostRequest("/recalls/$recallId/documents", addDocumentRequest, AddDocumentResponse::class.java)

  override fun getRecallDocument(recallId: RecallId, documentId: UUID): GetDocumentResponse =
    authenticatedGetRequest("/recalls/$recallId/documents/$documentId", GetDocumentResponse::class.java)

  override fun updateRecall(recallId: RecallId, updateRecallRequest: UpdateRecallRequest): RecallResponse =
    authenticatedPatchRequest("/recalls/$recallId", updateRecallRequest, RecallResponse::class.java)

  override fun search(searchRequest: SearchRequest) =
    authenticatedPostRequest("/search", searchRequest, object : ParameterizedTypeReference<List<SearchResult>>() {}, OK)

  private fun authenticatedPatchRequest(
    path: String,
    request: Any,
    responseClass: Class<RecallResponse>
  ): RecallResponse =
    sendAuthenticatedPatchRequestWithBody(path, request)
      .expectStatus().isOk
      .expectBody(responseClass)
      .returnResult()
      .responseBody!!

  private inline fun <reified T> sendAuthenticatedPatchRequestWithBody(
    path: String,
    request: T
  ): WebTestClient.ResponseSpec =
    webTestClient.patch().sendAuthenticatedRequestWithBody(path, request)

  private fun <T> authenticatedPostRequest(
    path: String,
    request: Any,
    responseClass: ParameterizedTypeReference<T>,
    expectedStatus: HttpStatus = CREATED
  ): T =
    sendAuthenticatedPostRequestWithBody(path, request)
      .expectStatus().isEqualTo(expectedStatus)
      .expectBody(responseClass)
      .returnResult()
      .responseBody!!

  private fun <T> authenticatedPostRequest(path: String, request: Any, responseClass: Class<T>): T =
    authenticatedPost(path, request)
      .expectBody(responseClass)
      .returnResult()
      .responseBody!!

  private fun authenticatedPost(path: String, request: Any): WebTestClient.ResponseSpec = webTestClient
    .post()
    .uri(path)
    .bodyValue(request)
    .headers {
      it.add(CONTENT_TYPE, APPLICATION_JSON_VALUE)
      it.withBearerAuthToken(testJwt("ROLE_MANAGE_RECALLS"))
    }
    .exchange()
    .expectStatus().isCreated

  private inline fun <reified T> sendAuthenticatedPostRequestWithBody(
    path: String,
    request: T
  ): WebTestClient.ResponseSpec =
    webTestClient.post().sendAuthenticatedRequestWithBody(path, request)

  private inline fun <reified T> WebTestClient.RequestBodyUriSpec.sendAuthenticatedRequestWithBody(
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

  fun testJwt(role: String) = jwtAuthenticationHelper.createTestJwt(role = role)

  private fun <T> authenticatedGetRequest(path: String, responseClass: Class<T>): T =
    authenticatedGet(path)
      .expectBody(responseClass)
      .returnResult()
      .responseBody!!

  private fun <T> authenticatedGetRequest(path: String, responseClass: ParameterizedTypeReference<T>): T =
    authenticatedGet(path)
      .expectBody(responseClass)
      .returnResult()
      .responseBody!!

  internal fun authenticatedGet(path: String): WebTestClient.ResponseSpec =
    webTestClient.get().uri(path)
      .headers {
        it.add(CONTENT_TYPE, APPLICATION_JSON_VALUE)
        it.withBearerAuthToken(testJwt("ROLE_MANAGE_RECALLS"))
      }
      .exchange()
      .expectStatus().isOk
}

fun HttpHeaders.withBearerAuthToken(jwt: String) = this.add(AUTHORIZATION, "Bearer $jwt")
