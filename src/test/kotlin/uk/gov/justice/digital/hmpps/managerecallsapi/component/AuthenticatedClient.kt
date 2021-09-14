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
import java.util.UUID

class AuthenticatedClient(
  private val webTestClient: WebTestClient,
  private val jwtAuthenticationHelper: JwtAuthenticationHelper
) {

  private val addHeaders: (headers: HttpHeaders) -> Unit = {
    it.add(CONTENT_TYPE, APPLICATION_JSON_VALUE)
    it.withBearerAuthToken(testJwt("ROLE_MANAGE_RECALLS"))
  }

  fun post(path: String, json: String): WebTestClient.ResponseSpec =
    sendPostRequest(path, json)

  fun patch(path: String, json: String): WebTestClient.ResponseSpec =
    sendPatchRequest(path, json)

  fun get(path: String): WebTestClient.ResponseSpec =
    sendGetRequest(path)

  fun bookRecall(bookRecallRequest: BookRecallRequest): RecallResponse =
    postRequest("/recalls", bookRecallRequest, RecallResponse::class.java)

  fun getRecall(recallId: RecallId): RecallResponse =
    getRequest("/recalls/$recallId", RecallResponse::class.java)

  fun getRecall(recallId: RecallId, expectedStatus: HttpStatus) {
    sendGetRequest("/recalls/$recallId", expectedStatus)
  }

  fun getAllRecalls(): List<RecallResponse> =
    sendGetRequest("/recalls")
      .expectBody(object : ParameterizedTypeReference<List<RecallResponse>>() {})
      .returnResult()
      .responseBody!!

  fun getRecallNotification(recallId: RecallId): Pdf =
    getRequest("/recalls/$recallId/recallNotification", Pdf::class.java)

  // TODO PUD-521: un-used for now ... for starters just return the revocation order
  fun getDossier(recallId: RecallId): Pdf =
    getRequest("/recalls/$recallId/dossier", Pdf::class.java)

  fun uploadRecallDocument(recallId: RecallId, addDocumentRequest: AddDocumentRequest): AddDocumentResponse =
    postRequest("/recalls/$recallId/documents", addDocumentRequest, AddDocumentResponse::class.java)

  fun uploadRecallDocument(recallId: RecallId, addDocumentRequest: AddDocumentRequest, expectedStatus: HttpStatus) {
    sendPostRequest("/recalls/$recallId/documents", addDocumentRequest, expectedStatus)
  }

  fun getRecallDocument(recallId: RecallId, documentId: UUID): GetDocumentResponse =
    getRequest("/recalls/$recallId/documents/$documentId", GetDocumentResponse::class.java)

  fun getRecallDocument(recallId: RecallId, documentId: UUID, expectedStatus: HttpStatus) {
    sendGetRequest("/recalls/$recallId/documents/$documentId", expectedStatus)
  }

  fun updateRecall(recallId: RecallId, updateRecallRequest: UpdateRecallRequest): RecallResponse =
    patchRequest("/recalls/$recallId", updateRecallRequest, RecallResponse::class.java)

  fun search(searchRequest: SearchRequest) =
    sendPostRequest("/search", searchRequest, OK)
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
    webTestClient.patch().sendRequestWithBody(path, request)

  private fun <T> postRequest(
    path: String,
    request: Any,
    responseClass: Class<T>
  ): T =
    sendPostRequest(path, request)
      .expectBody(responseClass)
      .returnResult()
      .responseBody!!

  private fun sendPostRequest(
    path: String,
    request: Any,
    expectedStatus: HttpStatus = CREATED
  ): WebTestClient.ResponseSpec =
    sendPostRequest(path, request)
      .expectStatus().isEqualTo(expectedStatus)

  private fun sendPostRequest(path: String, request: Any): WebTestClient.ResponseSpec =
    webTestClient.post().sendRequestWithBody(path, request)

  private fun WebTestClient.RequestBodyUriSpec.sendRequestWithBody(
    path: String,
    request: Any
  ): WebTestClient.ResponseSpec =
    this.uri(path)
      .bodyValue(request)
      .headers(addHeaders)
      .exchange()

  private fun sendGetRequest(path: String, expectedStatus: HttpStatus = OK) =
    webTestClient.get().uri(path)
      .headers(addHeaders)
      .exchange()
      .expectStatus().isEqualTo(expectedStatus)

  private fun <T> getRequest(path: String, responseClass: Class<T>): T =
    sendGetRequest(path, OK)
      .expectBody(responseClass)
      .returnResult()
      .responseBody!!

  fun testJwt(role: String) = jwtAuthenticationHelper.createTestJwt(role = role)

  private fun HttpHeaders.withBearerAuthToken(jwt: String) = this.add(AUTHORIZATION, "Bearer $jwt")
}
