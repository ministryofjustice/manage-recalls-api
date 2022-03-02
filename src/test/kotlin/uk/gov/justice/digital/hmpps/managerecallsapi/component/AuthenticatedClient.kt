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
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.AddUserDetailsRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Api
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.BookRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ConfirmedRecallTypeRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.CreateLastKnownAddressRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.FieldAuditEntry
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.FieldAuditSummary
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.GenerateDocumentRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.GetDocumentResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MissingDocumentsRecordRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.NewDocumentResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.NoteController.CreateNoteRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallResponseLite
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallSearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecommendedRecallTypeRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RescindRecordController.RescindDecisionRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RescindRecordController.RescindRequestRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ReturnedToCustodyRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.SearchController
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.SearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.StopReason
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.StopRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UpdateDocumentRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UpdateDocumentResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UpdateRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UploadDocumentRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UserDetailsResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FieldPath
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FileName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastKnownAddressId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NoteId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RescindRecordId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import java.time.OffsetDateTime

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

  fun get(path: String, status: HttpStatus = OK): WebTestClient.ResponseSpec =
    sendGetRequest(path, status)

  fun delete(path: String, expectedStatus: HttpStatus): WebTestClient.ResponseSpec =
    deleteRequest(path, expectedStatus)

  fun bookRecall(bookRecallRequest: BookRecallRequest): RecallResponse =
    postRequest("/recalls", bookRecallRequest, RecallResponse::class.java)

  fun getRecall(recallId: RecallId): RecallResponse =
    getRequest("/recalls/$recallId", RecallResponse::class.java)

  fun requestRescind(recallId: RecallId, request: RescindRequestRequest, status: HttpStatus) =
    sendPostRequest("/recalls/$recallId/rescind-records", request).expectStatus().isEqualTo(status)

  fun requestRescind(recallId: RecallId, request: RescindRequestRequest): RescindRecordId =
    requestRescind(recallId, request, CREATED).expectBody(RescindRecordId::class.java)
      .returnResult()
      .responseBody!!

  fun createNote(recallId: RecallId, request: CreateNoteRequest, status: HttpStatus) =
    sendPostRequest("/recalls/$recallId/notes", request).expectStatus().isEqualTo(status)

  fun createNote(recallId: RecallId, request: CreateNoteRequest): NoteId =
    createNote(recallId, request, CREATED).expectBody(NoteId::class.java)
      .returnResult()
      .responseBody!!

  fun decideRescind(recallId: RecallId, rescindRecordId: RescindRecordId, request: RescindDecisionRequest, status: HttpStatus = OK) =
    patchRequest("/recalls/$recallId/rescind-records/$rescindRecordId", request, status)

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

  fun getAllRecalls(): List<RecallResponseLite> =
    sendGetRequest("/recalls")
      .expectBody(object : ParameterizedTypeReference<List<RecallResponseLite>>() {})
      .returnResult()
      .responseBody!!

  fun uploadDocument(recallId: RecallId, uploadDocumentRequest: UploadDocumentRequest): NewDocumentResponse =
    postRequest("/recalls/$recallId/documents/uploaded", uploadDocumentRequest, NewDocumentResponse::class.java)

  fun uploadDocument(
    recallId: RecallId,
    uploadDocumentRequest: UploadDocumentRequest,
    expectedStatus: HttpStatus
  ): WebTestClient.ResponseSpec {
    return sendPostRequest("/recalls/$recallId/documents/uploaded", uploadDocumentRequest, expectedStatus)
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

  fun generateDocument(recallId: RecallId, category: DocumentCategory, fileName: FileName, detail: String? = null) =
    postRequest("/recalls/$recallId/documents/generated", GenerateDocumentRequest(category, fileName, detail), NewDocumentResponse::class.java)

  fun updateRecall(recallId: RecallId, updateRecallRequest: UpdateRecallRequest): RecallResponse =
    patchRequest("/recalls/$recallId", updateRecallRequest, RecallResponse::class.java)

  fun updateRecommendedRecallType(recallId: RecallId, recallType: RecallType): RecallResponse =
    patchRequest("/recalls/$recallId/recommended-recall-type", RecommendedRecallTypeRequest(recallType), RecallResponse::class.java)

  fun updateConfirmedRecallType(recallId: RecallId, request: ConfirmedRecallTypeRequest): RecallResponse =
    patchRequest("/recalls/$recallId/confirmed-recall-type", request, RecallResponse::class.java)

  fun assignRecall(recallId: RecallId, assignee: UserId): RecallResponse =
    postWithoutBody("/recalls/$recallId/assignee/$assignee", responseClass = RecallResponse::class.java)

  fun unassignRecall(recallId: RecallId, assignee: UserId) =
    deleteRequestWithResponse("/recalls/$recallId/assignee/$assignee", RecallResponse::class.java)

  fun unassignRecall(recallId: RecallId, assignee: UserId, expectedStatus: HttpStatus) =
    deleteRequest("/recalls/$recallId/assignee/$assignee", expectedStatus)

  fun returnedToCustody(recallId: RecallId, returnedToCustodyDateTime: OffsetDateTime, notificationDateTime: OffsetDateTime) =
    sendPostRequest("/recalls/$recallId/returned-to-custody", ReturnedToCustodyRequest(returnedToCustodyDateTime, notificationDateTime))
      .expectStatus().isEqualTo(OK)

  fun stopRecall(recallId: RecallId, stopReason: StopReason) =
    sendPostRequest("/recalls/$recallId/stop", StopRecallRequest(stopReason))
      .expectStatus().isEqualTo(OK)

  fun <T> missingDocumentsRecord(
    request: MissingDocumentsRecordRequest,
    expectedStatus: HttpStatus = CREATED,
    responseClass: Class<T>
  ) =
    postRequest("/missing-documents-records", request, responseClass, expectedStatus)

  fun <T> missingDocumentsRecord(
    recallId: RecallId,
    request: MissingDocumentsRecordRequest,
    expectedStatus: HttpStatus = CREATED,
    responseClass: Class<T>
  ) =
    postRequest("/recalls/$recallId/missing-documents-records", request, responseClass, expectedStatus)

  // FIXME PUD-1364
  fun <T> addLastKnownAddress(
    request: CreateLastKnownAddressRequest,
    expectedStatus: HttpStatus = CREATED,
    responseClass: Class<T>
  ) =
    postRequest("/last-known-addresses", request, responseClass, expectedStatus)

  fun <T> addLastKnownAddress(
    recallId: RecallId,
    request: CreateLastKnownAddressRequest,
    expectedStatus: HttpStatus = CREATED,
    responseClass: Class<T>
  ) =
    postRequest("/recalls/$recallId/last-known-addresses", request, responseClass, expectedStatus)

  fun deleteLastKnownAddress(recallId: RecallId, lastKnownAddressId: LastKnownAddressId, expectedStatus: HttpStatus = NO_CONTENT) =
    deleteRequest("/recalls/$recallId/last-known-addresses/$lastKnownAddressId", expectedStatus)

  fun search(searchRequest: SearchRequest, expectedStatus: HttpStatus) =
    sendPostRequest("/search", searchRequest, expectedStatus)

  fun search(searchRequest: SearchRequest) =
    search(searchRequest, OK)
      .expectBody(object : ParameterizedTypeReference<List<SearchController.Api.Prisoner>>() {})
      .returnResult()
      .responseBody!!

  fun prisonerByNomsNumber(nomsNumber: NomsNumber, expectedStatus: HttpStatus) =
    sendGetRequest("/prisoner/$nomsNumber", expectedStatus)

  fun prisonerByNomsNumber(nomsNumber: NomsNumber) =
    prisonerByNomsNumber(nomsNumber, OK)
      .expectBody(object : ParameterizedTypeReference<SearchController.Api.Prisoner>() {})
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

  fun getCurrentUserDetails() =
    getRequest("/users/current", UserDetailsResponse::class.java)

  fun auditForField(recallId: RecallId, fieldPath: FieldPath) =
    sendGetRequest("/audit/$recallId/$fieldPath")
      .expectBody(object : ParameterizedTypeReference<List<FieldAuditEntry>>() {})
      .returnResult()
      .responseBody!!

  fun auditSummaryForRecall(recallId: RecallId) =
    sendGetRequest("/audit/$recallId")
      .expectBody(object : ParameterizedTypeReference<List<FieldAuditSummary>>() {})
      .returnResult()
      .responseBody!!

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

  private fun patchRequest(
    path: String,
    request: Any,
    responseStatus: HttpStatus = OK
  ) =
    webTestClient.patch()
      .sendRequestWithBody(path, request)
      .expectStatus().isEqualTo(responseStatus)

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
