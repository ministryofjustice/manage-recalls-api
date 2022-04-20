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
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.NoteController.CreateNoteRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.PartBRecordController.PartBRecordRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallSearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecommendedRecallTypeRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RescindRecordController.RescindDecisionRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RescindRecordController.RescindRequestRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ReturnedToCustodyRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.StopReason
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.StopRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UpdateDocumentRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UpdateRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UploadDocumentRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.AddressSource
import uk.gov.justice.digital.hmpps.managerecallsapi.db.CaseworkerBand
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.encodeToBase64String
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CroNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Email
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FileName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastKnownAddressId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhoneNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import java.util.stream.Stream

class EndpointSecurityComponentTest : ComponentTestBase() {

  private val nomsNumber = NomsNumber("123456")
  private val details = "Some details"
  private val bookRecallRequest = BookRecallRequest(
    nomsNumber,
    FirstName("Barrie"),
    null,
    LastName("Badger"),
    CroNumber("1234/56A"),
    LocalDate.now()
  )
  private val fileBytes = "content".toByteArray()
  private val category = DocumentCategory.values().random()
  private val uploadDocumentRequest = UploadDocumentRequest(category, fileBytes.encodeToBase64String(), FileName("part_a.pdf"), details)
  private val updateRecallRequest = UpdateRecallRequest()
  private val recallSearchRequest = RecallSearchRequest(nomsNumber)
  private val userDetailsRequest = AddUserDetailsRequest(
    FirstName("John"),
    LastName("Badger"),
    "",
    Email("blah@badgers.com"),
    PhoneNumber("09876543210"),
    CaseworkerBand.FOUR_PLUS
  )
  private val updateDocumentRequest = UpdateDocumentRequest(DocumentCategory.values().random())
  private val generateDocumentRequest = GenerateDocumentRequest(DocumentCategory.RECALL_NOTIFICATION, FileName("RECALL_NOTIFICATION.pdf"), "some more detail")
  private val missingDocumentsRecordRequest = MissingDocumentsRecordRequest(
    listOf(DocumentCategory.values().random()),
    "some detail",
    "content",
    FileName("email.msg")
  )
  private val partBRecordRequest = PartBRecordRequest("some detail", LocalDate.now(), FileName("partB.pdf"), "part B content", FileName("email.msg"), "email content", FileName("oasys.pdf"), "oasys content")
  private val createLastKnownAddressRequest = CreateLastKnownAddressRequest(
    "address line 1",
    "address line 2",
    "some town",
    "some postcode",
    AddressSource.LOOKUP
  )
  private val rescindRequestRequest = RescindRequestRequest("details", LocalDate.now(), "some content", FileName("filename"))
  private val rescindDecisionRequest = RescindDecisionRequest(true, "details", LocalDate.now(), "some content", FileName("filename"))
  private val returnedToCustodyRequest = ReturnedToCustodyRequest(OffsetDateTime.now().minusDays(1), OffsetDateTime.now().minusMinutes(1))
  private val createNoteRequest = CreateNoteRequest("subject", "details", FileName("filename"), "some content")
  private val stopRecallRequest = StopRecallRequest(StopReason.values().random())
  private val recommendedRecallTypeRequest = RecommendedRecallTypeRequest(RecallType.values().random())

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
      webTestClient.get().uri("/statistics/summary"),
      webTestClient.get().uri("/reports/weekly-recalls-new"),
      webTestClient.get().uri("/users/current"),
      webTestClient.get().uri("/audit/${UUID.randomUUID()}/currentPrison"),
      webTestClient.patch().uri("/recalls/${UUID.randomUUID()}").bodyValue(updateRecallRequest),
      webTestClient.patch().uri("/recalls/${UUID.randomUUID()}/documents/${::DocumentId.random()}").bodyValue(updateDocumentRequest),
      webTestClient.patch().uri("/recalls/${UUID.randomUUID()}/rescind-records/${UUID.randomUUID()}").bodyValue(rescindDecisionRequest),
      webTestClient.patch().uri("/recalls/${UUID.randomUUID()}/recommended-recall-type").bodyValue(recommendedRecallTypeRequest),
      webTestClient.post().uri("/recalls/search").bodyValue(recallSearchRequest),
      webTestClient.post().uri("/recalls/${UUID.randomUUID()}/documents/uploaded").bodyValue(uploadDocumentRequest),
      webTestClient.post().uri("/recalls/${UUID.randomUUID()}/documents/generated").bodyValue(generateDocumentRequest),
      webTestClient.post().uri("/recalls/${UUID.randomUUID()}/assignee/${::UserId.random()}"),
      webTestClient.post().uri("/users/").bodyValue(userDetailsRequest),
      webTestClient.post().uri("/recalls/${UUID.randomUUID()}/missing-documents-records").bodyValue(missingDocumentsRecordRequest),
      webTestClient.post().uri("/recalls/${UUID.randomUUID()}/partb-records").bodyValue(partBRecordRequest),
      webTestClient.post().uri("/recalls/${UUID.randomUUID()}/rescind-records").bodyValue(rescindRequestRequest),
      webTestClient.post().uri("/recalls/${UUID.randomUUID()}/notes").bodyValue(createNoteRequest),
      webTestClient.post().uri("/recalls/${UUID.randomUUID()}/stop").bodyValue(stopRecallRequest),
      webTestClient.post().uri("/recalls/${UUID.randomUUID()}/last-known-addresses").bodyValue(createLastKnownAddressRequest),
      webTestClient.post().uri("/recalls/${UUID.randomUUID()}/returned-to-custody").bodyValue(returnedToCustodyRequest),
      webTestClient.post().uri("/missing-documents-records").bodyValue(missingDocumentsRecordRequest),
      webTestClient.post().uri("/last-known-addresses").bodyValue(createLastKnownAddressRequest),
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
      webTestClient.get().uri("/reference-data/courts"),
      webTestClient.get().uri("/reference-data/local-delivery-units"),
      webTestClient.get().uri("/reference-data/mappa-levels"),
      webTestClient.get().uri("/reference-data/police-forces"),
      webTestClient.get().uri("/reference-data/prisons"),
      webTestClient.get().uri("/reference-data/recall-reasons"),
      webTestClient.get().uri("/reference-data/stop-reasons"),
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
