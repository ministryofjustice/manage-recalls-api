package uk.gov.justice.digital.hmpps.managerecallsapi.integration

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.BookRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import java.util.UUID
import java.util.stream.Stream

class RecallsIntegrationTest : IntegrationTestBase() {

  @MockkBean
  private lateinit var recallRepository: RecallRepository

  private val nomsNumber = "123456"
  private val bookRecallRequest = BookRecallRequest(nomsNumber)

  private fun requestBodySpecs() = Stream.of(
    webTestClient.get().uri("/recalls"),
    webTestClient.post().uri("/recalls").bodyValue(bookRecallRequest)
  )

  @ParameterizedTest
  @MethodSource("requestBodySpecs")
  fun `unauthorized when MANAGE_RECALLS role is missing`(requestBodySpec: WebTestClient.RequestBodySpec) {
    val invalidUserJwt = jwtAuthenticationHelper.createTestJwt(role = "ROLE_UNKNOWN")
    requestBodySpec.headers { it.withBearerAuthToken(invalidUserJwt) }
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `books a recall`() {
    val jwt = jwtAuthenticationHelper.createTestJwt(role = "ROLE_MANAGE_RECALLS")
    val aRecall = Recall(UUID.randomUUID(), nomsNumber)

    every { recallRepository.save(any()) } returns aRecall

    webTestClient.post().uri("/recalls").bodyValue(bookRecallRequest).headers { it.withBearerAuthToken(jwt) }
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.id").isEqualTo(aRecall.id.toString())
      .jsonPath("$.nomsNumber").isEqualTo(aRecall.nomsNumber)
  }

  @Test
  fun `returns all recalls`() {
    val jwt = jwtAuthenticationHelper.createTestJwt(role = "ROLE_MANAGE_RECALLS")

    every { recallRepository.findAll() } returns listOf(Recall(UUID.randomUUID(), nomsNumber))

    webTestClient.get().uri("/recalls").headers { it.withBearerAuthToken(jwt) }
      .exchange()
      .expectStatus().isOk
      .expectBody().jsonPath("$[:1].nomsNumber").isEqualTo(nomsNumber)
  }
}
