package uk.gov.justice.digital.hmpps.managerecallsapi.component

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.WebTestClient.RequestBodySpec
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.AddDocumentRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.AddUserDetailsRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.BookRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallSearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.SearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UpdateDocumentRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UpdateRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.encodeToBase64String
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Email
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhoneNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import java.util.UUID
import java.util.stream.Stream

class EndpointSecurityComponentTest : ComponentTestBase() {

  private val nomsNumber = NomsNumber("123456")
  private val bookRecallRequest = BookRecallRequest(nomsNumber)
  private val fileBytes = "content".toByteArray()
  private val category = RecallDocumentCategory.PART_A_RECALL_REPORT
  private val addDocumentRequest = AddDocumentRequest(category, fileBytes.encodeToBase64String(), "part_a.pdf")
  private val updateRecallRequest = UpdateRecallRequest()
  private val recallSearchRequest = RecallSearchRequest(nomsNumber)
  private val apiSearchRequest = SearchRequest(nomsNumber)
  private val userDetailsRequest = AddUserDetailsRequest(FirstName("John"), LastName("Badger"), "", Email("blah@badgers.com"), PhoneNumber("09876543210"))
  private val updateDocumentRequest = UpdateDocumentRequest(RecallDocumentCategory.REVOCATION_ORDER)

  // TODO:  MD get all the secured endpoints and make sure they are all included here (or get them all and automagically create the requests?)
  // Can this be a more lightweight test?  i.e. something other than a SpringBootTest. WebMVC test?
  @Suppress("unused")
  private fun requestBodySpecs(): Stream<WebTestClient.RequestHeadersSpec<*>>? {
    return Stream.of(
      webTestClient.post().uri("/recalls").bodyValue(bookRecallRequest),
      webTestClient.get().uri("/recalls/${UUID.randomUUID()}"),
      webTestClient.get().uri("/recalls"),
      webTestClient.get().uri("/recalls/${UUID.randomUUID()}/recallNotification/${UUID.randomUUID()}"),
      webTestClient.get().uri("/recalls/${UUID.randomUUID()}/dossier"),
      webTestClient.post().uri("/recalls/${UUID.randomUUID()}/documents").bodyValue(addDocumentRequest),
      webTestClient.get().uri("/recalls/${UUID.randomUUID()}/documents/${::DocumentId.random()}"),
      webTestClient.get().uri("/users/${UUID.randomUUID()}"),
      webTestClient.get().uri("/users/current"),
      webTestClient.patch().uri("/recalls/${UUID.randomUUID()}").bodyValue(updateRecallRequest),
      webTestClient.patch().uri("/recalls/${UUID.randomUUID()}/documents/${::DocumentId.random()}").bodyValue(updateDocumentRequest),
      webTestClient.post().uri("/recalls/search").bodyValue(recallSearchRequest),
      webTestClient.post().uri("/search").bodyValue(apiSearchRequest),
      webTestClient.post().uri("/recalls/${UUID.randomUUID()}/assignee/${::UserId.random()}"),
      webTestClient.post().uri("/users/").bodyValue(userDetailsRequest),
      webTestClient.delete().uri("/recalls/${UUID.randomUUID()}/assignee/${::UserId.random()}"),
      webTestClient.delete().uri("/recalls/${UUID.randomUUID()}/documents/${::DocumentId.random()}")
    )
  }

  @ParameterizedTest
  @MethodSource("requestBodySpecs")
  fun `unauthorized when ROLE_MANAGE_RECALLS role is missing`(requestBodySpec: RequestBodySpec) {
    requestBodySpec
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `unauthenticated endpoint can be hit without credentials`() {
    webTestClient.get().uri("/reference-data/local-delivery-units")
      .exchange()
      .expectStatus().isOk
  }
}
