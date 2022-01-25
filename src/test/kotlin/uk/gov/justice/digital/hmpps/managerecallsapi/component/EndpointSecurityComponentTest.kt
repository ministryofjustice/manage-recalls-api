package uk.gov.justice.digital.hmpps.managerecallsapi.component

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.WebTestClient.RequestBodySpec
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.AddUserDetailsRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.BookRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.CreateLastKnownAddressRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.GenerateDocumentRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MissingDocumentsRecordRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallSearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.SearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UpdateDocumentRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UpdateRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UploadDocumentRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.AddressSource
import uk.gov.justice.digital.hmpps.managerecallsapi.db.CaseworkerBand
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.encodeToBase64String
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Email
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastKnownAddressId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhoneNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import java.util.UUID
import java.util.stream.Stream

class EndpointSecurityComponentTest : ComponentTestBase() {

  private val nomsNumber = NomsNumber("123456")
  private val details = "Some details"
  private val bookRecallRequest = BookRecallRequest(nomsNumber, FirstName("Barrie"), null, LastName("Badger"))
  private val fileBytes = "content".toByteArray()
  private val category = DocumentCategory.values().random()
  private val uploadDocumentRequest = UploadDocumentRequest(category, fileBytes.encodeToBase64String(), "part_a.pdf", details)
  private val updateRecallRequest = UpdateRecallRequest()
  private val recallSearchRequest = RecallSearchRequest(nomsNumber)
  private val apiSearchRequest = SearchRequest(nomsNumber)
  private val userDetailsRequest = AddUserDetailsRequest(
    FirstName("John"),
    LastName("Badger"),
    "",
    Email("blah@badgers.com"),
    PhoneNumber("09876543210"),
    CaseworkerBand.FOUR_PLUS
  )
  private val updateDocumentRequest = UpdateDocumentRequest(DocumentCategory.values().random())
  private val generateDocumentRequest = GenerateDocumentRequest(DocumentCategory.RECALL_NOTIFICATION, "some more detail")
  private val missingDocumentsRecordRequest = MissingDocumentsRecordRequest(::RecallId.random(), listOf(DocumentCategory.values().random()), "some detail", "content", "email.msg")
  private val createLastKnownAddressRequest = CreateLastKnownAddressRequest(::RecallId.random(), "address line 1", "address line 2", "some town", "some postcode", AddressSource.LOOKUP)

  // TODO:  MD get all the secured endpoints and make sure they are all included here (or get them all and automagically create the requests?)
  // Can this be a more lightweight test?  i.e. something other than a SpringBootTest. WebMVC test?
  // Are these for testing the absence of the ROLE or the absence of any user object at all, i.e. to carry roles etc?
  @Suppress("unused")
  private fun roleManageRecallsRequiredRequestBodySpecs(): Stream<WebTestClient.RequestHeadersSpec<*>>? {
    return Stream.of(
      webTestClient.post().uri("/recalls").bodyValue(bookRecallRequest),
      webTestClient.get().uri("/recalls/${UUID.randomUUID()}"),
      webTestClient.get().uri("/recalls"),
      webTestClient.get().uri("/recalls/${UUID.randomUUID()}/recallNotification"),
      webTestClient.get().uri("/recalls/${UUID.randomUUID()}/dossier"),
      webTestClient.get().uri("/recalls/${UUID.randomUUID()}/documents/${::DocumentId.random()}"),
      webTestClient.get().uri("/recalls/${UUID.randomUUID()}/documents?category=${DocumentCategory.values().random()}"),
      webTestClient.get().uri("/users/current"),
      webTestClient.get().uri("/audit/${UUID.randomUUID()}/currentPrison"),
      webTestClient.patch().uri("/recalls/${UUID.randomUUID()}").bodyValue(updateRecallRequest),
      webTestClient.patch().uri("/recalls/${UUID.randomUUID()}/documents/${::DocumentId.random()}").bodyValue(updateDocumentRequest),
      webTestClient.post().uri("/recalls/search").bodyValue(recallSearchRequest),
      webTestClient.post().uri("/recalls/${UUID.randomUUID()}/documents/uploaded").bodyValue(uploadDocumentRequest),
      webTestClient.post().uri("/recalls/${UUID.randomUUID()}/documents/generated").bodyValue(generateDocumentRequest),
      webTestClient.post().uri("/search").bodyValue(apiSearchRequest),
      webTestClient.post().uri("/recalls/${UUID.randomUUID()}/assignee/${::UserId.random()}"),
      webTestClient.post().uri("/users/").bodyValue(userDetailsRequest),
      webTestClient.post().uri("/recalls/${UUID.randomUUID()}/missing-documents-records").bodyValue(missingDocumentsRecordRequest),
      webTestClient.post().uri("/missing-documents-records").bodyValue(missingDocumentsRecordRequest),
      webTestClient.post().uri("/last-known-addresses").bodyValue(createLastKnownAddressRequest),
      webTestClient.post().uri("/recalls/${UUID.randomUUID()}/last-known-addresses").bodyValue(createLastKnownAddressRequest),
      webTestClient.delete().uri("/recalls/${UUID.randomUUID()}/assignee/${::UserId.random()}"),
      webTestClient.delete().uri("/recalls/${UUID.randomUUID()}/documents/${::DocumentId.random()}"),
      webTestClient.delete().uri("/recalls/${UUID.randomUUID()}/last-known-addresses/${::LastKnownAddressId.random()}")
    )
  }

  @ParameterizedTest
  @MethodSource("roleManageRecallsRequiredRequestBodySpecs")
  fun `unauthorized when ROLE_MANAGE_RECALLS role is missing on request which requires it`(requestBodySpec: RequestBodySpec) {
    requestBodySpec
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Suppress("unused")
  private fun noRoleRequiredRequestBodySpecs(): Stream<WebTestClient.RequestHeadersSpec<*>>? {
    return Stream.of(
      webTestClient.get().uri("/reference-data/local-delivery-units"),
      webTestClient.get().uri("/reference-data/courts"),
      webTestClient.get().uri("/reference-data/prisons"),
      webTestClient.get().uri("/reference-data/police-forces"),
    )
  }

  @ParameterizedTest
  @MethodSource("noRoleRequiredRequestBodySpecs")
  fun `unauthenticated endpoint returns OK for request without credentials`(requestBodySpec: RequestBodySpec) {
    requestBodySpec
      .exchange()
      .expectStatus().isOk
  }
}
