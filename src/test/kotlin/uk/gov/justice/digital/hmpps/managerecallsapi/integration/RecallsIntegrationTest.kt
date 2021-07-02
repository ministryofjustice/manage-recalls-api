package uk.gov.justice.digital.hmpps.managerecallsapi.integration

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import java.util.UUID

class RecallsIntegrationTest : IntegrationTestBase() {

  @MockkBean
  private lateinit var recallRepository: RecallRepository

  @Test
  fun `unauthorized when MANAGE_RECALLS role is missing`() {
    val invalidUserJwt = jwtAuthenticationHelper.createTestJwt(role = "ROLE_UNKNOWN")
    webTestClient.get().uri("/recalls").headers { it.withBearerAuthToken(invalidUserJwt) }
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `returns all recalls`() {
    val jwt = jwtAuthenticationHelper.createTestJwt(role = "ROLE_MANAGE_RECALLS")
    val nomsNumber = "nomsno"

    every { recallRepository.findAll() } returns listOf(Recall(UUID.randomUUID(), nomsNumber))

    webTestClient.get().uri("/recalls").headers { it.withBearerAuthToken(jwt) }
      .exchange()
      .expectStatus().isOk
      .expectBody().jsonPath("$[:1].nomsNumber").isEqualTo(nomsNumber)
  }
}
