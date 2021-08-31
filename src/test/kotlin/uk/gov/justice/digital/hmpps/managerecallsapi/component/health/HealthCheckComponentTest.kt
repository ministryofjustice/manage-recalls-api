package uk.gov.justice.digital.hmpps.managerecallsapi.component.health

import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.OK
import org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.managerecallsapi.component.ComponentTestBase
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter.ISO_DATE

class HealthCheckComponentTest : ComponentTestBase() {

  @Test
  fun `healthy service returns status of each health check and version details`() {
    prisonerOffenderSearch.isHealthy()
    gotenbergMockServer.isHealthy()

    healthCheckIsUpWith(
      "/health",
      "status" to "UP",
      "components.prisonerOffenderSearchHealth.details.status" to OK.name,
      "components.gotenbergHealth.details.status" to OK.name,
      "components.healthInfo.details.version" to LocalDateTime.now().format(ISO_DATE),
      "components.db.status" to "UP"
    )
  }

  @Test
  fun `service is unhealthy when prisonerOffenderSearch is unhealthy`() {
    prisonerOffenderSearch.isUnhealthy()

    healthCheckIsDownWith(
      SERVICE_UNAVAILABLE,
      "components.prisonerOffenderSearchHealth.details.status" to INTERNAL_SERVER_ERROR.name
    )
  }

  @Test
  fun `service is unhealthy when gotenberg is unhealthy`() {
    gotenbergMockServer.isUnhealthy()

    healthCheckIsDownWith(
      SERVICE_UNAVAILABLE,
      "components.gotenbergHealth.details.status" to INTERNAL_SERVER_ERROR.name
    )
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

  private fun healthCheckIsDownWith(expectedStatus: HttpStatus, vararg jsonPathAssertions: Pair<String, String>) {
    unauthenticatedGet("/health", expectedStatus)
      .expectBody()
      .jsonPath("status").isEqualTo("DOWN")
      .apply {
        jsonPathAssertions.forEach { (jsonPath, equalTo) ->
          hasJsonPath(jsonPath, equalTo)
        }
      }
  }

  private fun healthCheckIsUpWith(healthUrl: String, vararg jsonPathAssertions: Pair<String, String>, expectedStatus: HttpStatus = OK) {
    unauthenticatedGet(healthUrl, expectedStatus)
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
