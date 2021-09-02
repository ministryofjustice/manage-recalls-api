package uk.gov.justice.digital.hmpps.managerecallsapi.component

import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.test.web.reactive.server.WebTestClient
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

  fun get(path: String): WebTestClient.ResponseSpec

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

  private val addHeaders: (headers: HttpHeaders) -> Unit = {
    it.add(CONTENT_TYPE, APPLICATION_JSON_VALUE)
    it.withBearerAuthToken(testJwt("ROLE_MANAGE_RECALLS"))
  }

  override fun post(path: String, json: String): WebTestClient.ResponseSpec =
    sendPostRequest(path, json)

  override fun patch(path: String, json: String): WebTestClient.ResponseSpec =
    sendPatchRequest(path, json)

  override fun get(path: String): WebTestClient.ResponseSpec =
    sendGetRequest(path)

  override fun bookRecall(bookRecallRequest: BookRecallRequest): RecallResponse =
    postRequest("/recalls", bookRecallRequest, RecallResponse::class.java)

  override fun getRecall(recallId: RecallId): RecallResponse =
    getRequest("/recalls/$recallId", RecallResponse::class.java)

  override fun getAllRecalls(): List<RecallResponse> =
    sendGetRequest("/recalls")
      .expectBody(object : ParameterizedTypeReference<List<RecallResponse>>() {})
      .returnResult()
      .responseBody!!

  override fun getRevocationOrder(recallId: RecallId): Pdf =
    getRequest("/recalls/$recallId/revocationOrder", Pdf::class.java)

  override fun uploadRecallDocument(recallId: RecallId, addDocumentRequest: AddDocumentRequest): AddDocumentResponse =
    postRequest("/recalls/$recallId/documents", addDocumentRequest, AddDocumentResponse::class.java)

  override fun getRecallDocument(recallId: RecallId, documentId: UUID): GetDocumentResponse =
    getRequest("/recalls/$recallId/documents/$documentId", GetDocumentResponse::class.java)

  override fun updateRecall(recallId: RecallId, updateRecallRequest: UpdateRecallRequest): RecallResponse =
    patchRequest("/recalls/$recallId", updateRecallRequest, RecallResponse::class.java)

  override fun search(searchRequest: SearchRequest) =
    sendPostRequest("/search", searchRequest)
      .expectStatus().isOk
      .expectBody(object : ParameterizedTypeReference<List<SearchResult>>() {})
      .returnResult()
      .responseBody!!

  private fun patchRequest(path: String, request: Any, responseClass: Class<RecallResponse>): RecallResponse =
    sendPatchRequest(path, request)
      .expectStatus().isOk
      .expectBody(responseClass)
      .returnResult()
      .responseBody!!

  private fun sendPatchRequest(
    path: String,
    request: Any
  ): WebTestClient.ResponseSpec =
    webTestClient.patch().sendAuthenticatedRequestWithBody(path, request)

  private fun <T> postRequest(
    path: String,
    request: Any,
    responseClass: Class<T>
  ): T =
    sendPostRequest(path, request)
      .expectStatus().isCreated
      .expectBody(responseClass)
      .returnResult()
      .responseBody!!

  private fun sendPostRequest(
    path: String,
    request: Any
  ): WebTestClient.ResponseSpec =
    webTestClient.post().sendAuthenticatedRequestWithBody(path, request)

  private fun WebTestClient.RequestBodyUriSpec.sendAuthenticatedRequestWithBody(
    path: String,
    request: Any
  ): WebTestClient.ResponseSpec =
    this.uri(path)
      .bodyValue(request)
      .headers(addHeaders)
      .exchange()

  private fun sendGetRequest(path: String) =
    webTestClient.get().uri(path)
      .headers(addHeaders)
      .exchange()
      .expectStatus().isOk

  private fun <T> getRequest(path: String, responseClass: Class<T>): T =
    sendGetRequest(path)
      .expectBody(responseClass)
      .returnResult()
      .responseBody!!

  fun testJwt(role: String) = jwtAuthenticationHelper.createTestJwt(role = role)
}

fun HttpHeaders.withBearerAuthToken(jwt: String) = this.add(AUTHORIZATION, "Bearer $jwt")
