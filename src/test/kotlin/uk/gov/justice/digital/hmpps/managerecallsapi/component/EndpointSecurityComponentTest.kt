package uk.gov.justice.digital.hmpps.managerecallsapi.component

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.WebTestClient.RequestBodySpec
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.AddDocumentRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.BookRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.SearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UpdateRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import java.util.Base64
import java.util.UUID
import java.util.stream.Stream

class EndpointSecurityComponentTest : ComponentTestBase() {

  private val nomsNumber = NomsNumber("123456")
  private val bookRecallRequest = BookRecallRequest(nomsNumber)
  private val fileBytes = "content".toByteArray()
  private val category = RecallDocumentCategory.PART_A_RECALL_REPORT
  private val addDocumentRequest = AddDocumentRequest(category, Base64.getEncoder().encodeToString(fileBytes), "fileName")
  private val updateRecallRequest = UpdateRecallRequest()
  private val apiSearchRequest = SearchRequest(nomsNumber)

  // TODO:  MD get all the secured endpoints and make sure they are all included here (or get them all and automagically create the requests?)
  // Can this be a more lightweight test?  i.e. something other than a SpringBootTest. WebMVC test?
  @Suppress("unused")
  private fun requestBodySpecs(): Stream<WebTestClient.RequestHeadersSpec<*>>? {
    return Stream.of(
      webTestClient.post().uri("/recalls").bodyValue(bookRecallRequest),
      webTestClient.get().uri("/recalls/${UUID.randomUUID()}"),
      webTestClient.get().uri("/recalls"),
      webTestClient.get().uri("/recalls/${UUID.randomUUID()}/revocationOrder"),
      webTestClient.post().uri("/recalls/${UUID.randomUUID()}/documents").bodyValue(addDocumentRequest),
      webTestClient.get().uri("/recalls/${UUID.randomUUID()}/documents/${UUID.randomUUID()}"),
      webTestClient.patch().uri("/recalls/${UUID.randomUUID()}").bodyValue(updateRecallRequest),
      webTestClient.post().uri("/search").bodyValue(apiSearchRequest)
    )
  }

  @ParameterizedTest
  @MethodSource("requestBodySpecs")
  fun `unauthorized when ROLE_MANAGE_RECALLS role is missing`(requestBodySpec: RequestBodySpec) {
    requestBodySpec
      .exchange()
      .expectStatus().isUnauthorized
  }
}
