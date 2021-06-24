package uk.gov.justice.digital.hmpps.managerecallsapi.integration.health

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.OK
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.managerecallsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers.GotenbergMockServer
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter.ISO_DATE

class HealthCheckTest : IntegrationTestBase() {

  @Autowired
  lateinit var gotenberg: GotenbergMockServer

  @BeforeAll
  fun startGotenberg() {
    gotenberg.start()
  }

  @AfterAll
  fun stopGotenberg() {
    gotenberg.stop()
  }

  @Test
  fun `healthy service returns status of each health check and version details`() {
    prisonerOffenderSearch.isHealthy()
    gotenberg.isHealthy()

    healthCheckIsUpWith(
      "/health",
      "status" to "UP",
      "components.prisonerOffenderSearchHealth.details.HttpStatus" to OK.name,
      "components.gotenbergHealth.details.HttpStatus" to OK.name,
      "components.healthInfo.details.version" to LocalDateTime.now().format(ISO_DATE)
    )
  }

  @Test
  fun `service is unhealthy when prisonerOffenderSearch is unhealthy`() {
    prisonerOffenderSearch.isUnhealthy()

    healthCheckIsDownWith("components.prisonerOffenderSearchHealth.details.HttpStatus" to INTERNAL_SERVER_ERROR.name)
  }

  @Test
  fun `service is unhealthy when gotenberg is unhealthy`() {
    gotenberg.isUnhealthy()

    healthCheckIsDownWith("components.gotenbergHealth.details.HttpStatus" to INTERNAL_SERVER_ERROR.name)
  }

  @Test
  fun `Health ping page is accessible`() {
    healthCheckIsUp("/health/ping")
  }

  @Test
  fun `readiness reports ok`() {
    healthCheckIsUp("/health/readiness")
  }

  @Test
  fun `liveness reports ok`() {
    healthCheckIsUp("/health/liveness")
  }

  private fun healthCheckIsUp(healthUrl: String) {
    healthCheckIsUpWith(healthUrl, "status" to "UP")
  }

  private fun healthCheckIsDownWith(vararg jsonPathAssertions: Pair<String, String>) {
    webTestClient.get()
      .uri("/health")
      .exchange()
      .expectStatus().is5xxServerError
      .expectBody()
      .jsonPath("status").isEqualTo("DOWN")
      .apply {
        jsonPathAssertions.forEach { (jsonPath, equalTo) ->
          hasJsonPath(jsonPath, equalTo)
        }
      }
  }

  private fun healthCheckIsUpWith(healthUrl: String, vararg jsonPathAssertions: Pair<String, String>) {
    webTestClient.get()
      .uri(healthUrl)
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .apply {
        jsonPathAssertions.forEach { (jsonPath, equalTo) ->
          hasJsonPath(jsonPath, equalTo)
        }
      }
  }

  private fun WebTestClient.BodyContentSpec.hasJsonPath(jsonPath: String, equalTo: String) =
    jsonPath(jsonPath).isEqualTo(equalTo)
}
