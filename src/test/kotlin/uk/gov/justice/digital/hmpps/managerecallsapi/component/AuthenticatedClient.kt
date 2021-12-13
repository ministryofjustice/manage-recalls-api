package uk.gov.justice.digital.hmpps.managerecallsapi.component

import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.HttpStatus.OK
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.AddDocumentRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.AddDocumentResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.AddUserDetailsRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Api
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.BookRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.CreateDocumentRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.GetDocumentResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MissingDocumentsRecordRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Pdf
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallSearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.SearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.SearchResult
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UpdateDocumentRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UpdateDocumentResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UpdateRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UserDetailsResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random

class AuthenticatedClient(
  private val webTestClient: WebTestClient,
  private val jwtAuthenticationHelper: JwtAuthenticationHelper
) {

  val userId: UserId = ::UserId.random()

  private val addHeaders: (headers: HttpHeaders) -> Unit = {
    it.add(CONTENT_TYPE, APPLICATION_JSON_VALUE)
    it.withBearerAuthToken(testJwt("ROLE_MANAGE_RECALLS", userId))
  }

  fun post(path: String, json: String): WebTestClient.ResponseSpec =
    sendPostRequest(path, json)

  fun patch(path: String, json: String): WebTestClient.ResponseSpec =
    sendPatchRequest(path, json)

  fun get(path: String): WebTestClient.ResponseSpec =
    sendGetRequest(path)

  fun delete(path: String, expectedStatus: HttpStatus): WebTestClient.ResponseSpec =
    deleteRequest(path, expectedStatus)

  fun bookRecall(bookRecallRequest: BookRecallRequest): RecallResponse =
    postRequest("/recalls", bookRecallRequest, RecallResponse::class.java)

  fun getRecall(recallId: RecallId): RecallResponse =
    getRequest("/recalls/$recallId", RecallResponse::class.java)

  fun getRecallDocuments(recallId: RecallId, category: DocumentCategory): List<Api.RecallDocument> =
    webTestClient.get().uri { uriBuilder ->
      uriBuilder.path("/recalls/$recallId/documents")
        .queryParam("category", "{category}")
        .build(category)
    }
      .headers(addHeaders)
      .exchange()
      .expectStatus().isOk
      .expectBody(object : ParameterizedTypeReference<List<Api.RecallDocument>>() {})
      .returnResult()
      .responseBody!!

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

  fun getDossier(recallId: RecallId): Pdf =
    getRequest("/recalls/$recallId/dossier", Pdf::class.java)

  fun getLetterToPrison(recallId: RecallId): Pdf =
    getRequest("/recalls/$recallId/letter-to-prison", Pdf::class.java)

  fun uploadDocument(recallId: RecallId, addDocumentRequest: AddDocumentRequest): AddDocumentResponse =
    postRequest("/recalls/$recallId/documents", addDocumentRequest, AddDocumentResponse::class.java)

  fun uploadDocument(
    recallId: RecallId,
    addDocumentRequest: AddDocumentRequest,
    expectedStatus: HttpStatus
  ): WebTestClient.ResponseSpec {
    return sendPostRequest("/recalls/$recallId/documents", addDocumentRequest, expectedStatus)
  }

  fun updateDocumentCategory(
    recallId: RecallId,
    documentId: DocumentId,
    updateDocumentRequest: UpdateDocumentRequest
  ): UpdateDocumentResponse =
    patchRequest("/recalls/$recallId/documents/$documentId", updateDocumentRequest, UpdateDocumentResponse::class.java)

  fun getDocument(recallId: RecallId, documentId: DocumentId): GetDocumentResponse =
    getRequest("/recalls/$recallId/documents/$documentId", GetDocumentResponse::class.java)

  fun getDocument(recallId: RecallId, documentId: DocumentId, expectedStatus: HttpStatus) {
    sendGetRequest("/recalls/$recallId/documents/$documentId", expectedStatus)
  }

  fun deleteDocument(recallId: RecallId, documentId: DocumentId, expectedStatus: HttpStatus = NO_CONTENT) =
    deleteRequest("/recalls/$recallId/documents/$documentId", expectedStatus)

  fun createDocument(recallId: RecallId, category: DocumentCategory, detail: String) =
    postRequest("/recalls/$recallId/documents/create", CreateDocumentRequest(category, detail), AddDocumentResponse::class.java)

  fun updateRecall(recallId: RecallId, updateRecallRequest: UpdateRecallRequest): RecallResponse =
    patchRequest("/recalls/$recallId", updateRecallRequest, RecallResponse::class.java)

  fun assignRecall(recallId: RecallId, assignee: UserId): RecallResponse =
    postWithoutBody("/recalls/$recallId/assignee/$assignee", responseClass = RecallResponse::class.java)

  fun unassignRecall(recallId: RecallId, assignee: UserId) =
    deleteRequestWithResponse("/recalls/$recallId/assignee/$assignee", RecallResponse::class.java)

  fun unassignRecall(recallId: RecallId, assignee: UserId, expectedStatus: HttpStatus) =
    deleteRequest("/recalls/$recallId/assignee/$assignee", expectedStatus)

  fun <T> missingDocumentsRecord(
    request: MissingDocumentsRecordRequest,
    expectedStatus: HttpStatus = CREATED,
    responseClass: Class<T>
  ) =
    postRequest("/missing-documents-records", request, responseClass, expectedStatus)

  fun search(searchRequest: SearchRequest, expectedStatus: HttpStatus) =
    sendPostRequest("/search", searchRequest, expectedStatus)

  fun search(searchRequest: SearchRequest) =
    sendPostRequest("/search", searchRequest, OK)
      .expectBody(object : ParameterizedTypeReference<List<SearchResult>>() {})
      .returnResult()
      .responseBody!!

  fun searchRecalls(searchRequest: RecallSearchRequest): List<RecallResponse> =
    sendPostRequest("/recalls/search", searchRequest, OK)
      .expectBody(object : ParameterizedTypeReference<List<RecallResponse>>() {})
      .returnResult()
      .responseBody!!

  fun <T> addUserDetails(
    addUserDetailsRequest: AddUserDetailsRequest,
    responseClass: Class<T>,
    expectedStatus: HttpStatus = CREATED
  ): T =
    postRequest("/users", addUserDetailsRequest, responseClass, expectedStatus)

  fun getUserDetails(userId: UserId) =
    getRequest("/users/${userId.value}", UserDetailsResponse::class.java)

  private fun <T> patchRequest(path: String, request: Any, responseClass: Class<T>): T =
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
    responseClass: Class<T>,
    responseStatus: HttpStatus = CREATED
  ): T =
    sendPostRequest(path, request)
      .expectStatus().isEqualTo(responseStatus)
      .expectBody(responseClass)
      .returnResult()
      .responseBody!!

  private fun <T> postWithoutBody(
    path: String,
    responseClass: Class<T>
  ): T =
    webTestClient.post()
      .uri(path)
      .headers(addHeaders)
      .exchange()
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

  private fun <T> deleteRequestWithResponse(
    path: String,
    responseClass: Class<T>,
  ) =
    webTestClient.delete().uri(path)
      .headers(addHeaders)
      .exchange()
      .expectStatus().isOk
      .expectBody(responseClass)
      .returnResult()
      .responseBody!!

  private fun deleteRequest(
    path: String,
    expectedStatus: HttpStatus
  ) =
    webTestClient.delete().uri(path)
      .headers(addHeaders)
      .exchange()
      .expectStatus().isEqualTo(expectedStatus)

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
    sendGetRequest(path)
      .expectBody(responseClass)
      .returnResult()
      .responseBody!!

  fun testJwt(role: String, userId: UserId) = jwtAuthenticationHelper.createTestJwt(userId, role = role)

  private fun HttpHeaders.withBearerAuthToken(jwt: String) = this.add(AUTHORIZATION, "Bearer $jwt")
}
