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
    prisonRegisterMockServer.isHealthy()
    courtRegisterMockServer.isHealthy()

    healthCheckIsUpWith(
      "/health",
      "status" to "UP",
      "components.prisonerOffenderSearch.details.status" to OK.name,
      "components.gotenberg.details.status" to OK.name,
      "components.healthInfo.details.version" to LocalDateTime.now().format(ISO_DATE),
      "components.db.status" to "UP",
      "components.s3.status" to "UP",
      "components.prisonRegister.status" to "UP",
      "components.courtRegister.status" to "UP"
    )
  }

  @Test
  fun `service is unhealthy when prisonerOffenderSearch is unhealthy`() {
    prisonerOffenderSearch.isUnhealthy()

    healthCheckIsDownWith(
      SERVICE_UNAVAILABLE,
      "components.prisonerOffenderSearch.details.status" to INTERNAL_SERVER_ERROR.name
    )
  }

  @Test
  fun `service is unhealthy when courtRegister is unhealthy`() {
    courtRegisterMockServer.isUnhealthy()

    healthCheckIsDownWith(
      SERVICE_UNAVAILABLE,
      "components.courtRegister.details.status" to INTERNAL_SERVER_ERROR.name
    )
  }

  @Test
  fun `service is unhealthy when prisonRegister is unhealthy`() {
    prisonRegisterMockServer.isUnhealthy()

    healthCheckIsDownWith(
      SERVICE_UNAVAILABLE,
      "components.prisonRegister.details.status" to INTERNAL_SERVER_ERROR.name
    )
  }

  @Test
  fun `service is unhealthy when gotenberg is unhealthy`() {
    gotenbergMockServer.isUnhealthy()

    healthCheckIsDownWith(
      SERVICE_UNAVAILABLE,
      "components.gotenberg.details.status" to INTERNAL_SERVER_ERROR.name
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
      .jsonPath("status").isEqualTo("DOWN")
      .apply {
        jsonPathAssertions.forEach { (jsonPath, equalTo) ->
          hasJsonPath(jsonPath, equalTo)
        }
      }
  }

  private fun healthCheckIsUpWith(healthUrl: String, vararg jsonPathAssertions: Pair<String, String>, expectedStatus: HttpStatus = OK) {
    unauthenticatedGet(healthUrl, expectedStatus)
      .apply {
        jsonPathAssertions.forEach { (jsonPath, equalTo) ->
          hasJsonPath(jsonPath, equalTo)
        }
      }
  }

  private fun WebTestClient.BodyContentSpec.hasJsonPath(jsonPath: String, equalTo: String) =
    jsonPath(jsonPath).isEqualTo(equalTo)
}
