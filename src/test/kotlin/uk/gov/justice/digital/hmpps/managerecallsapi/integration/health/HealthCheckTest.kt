package uk.gov.justice.digital.hmpps.managerecallsapi.integration.health

import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.OK
import uk.gov.justice.digital.hmpps.managerecallsapi.integration.IntegrationTestBase
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class HealthCheckTest : IntegrationTestBase() {
  @Test
  fun `service is healthy`() {
    prisonerOffenderSearchApi.isHealthy()

    webTestClient.get()
      .uri("/health")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("components.prisonerOffenderSearchHealth.details.HttpStatus").isEqualTo(OK.name)
      .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `service is unhealthy`() {
    prisonerOffenderSearchApi.isUnhealthy()

    webTestClient.get()
      .uri("/health")
      .exchange()
      .expectStatus().is5xxServerError
      .expectBody()
      .jsonPath("components.prisonerOffenderSearchHealth.details.HttpStatus").isEqualTo(INTERNAL_SERVER_ERROR.name)
      .jsonPath("status").isEqualTo("DOWN")
  }

  @Test
  fun `Health info reports version`() {
    prisonerOffenderSearchApi.isHealthy()

    webTestClient.get().uri("/health")
      .exchange()
      .expectStatus().isOk
      .expectBody().jsonPath("components.healthInfo.details.version").value(
        Matchers.startsWith(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE))
      )
  }

  @Test
  fun `Health ping page is accessible`() {
    webTestClient.get()
      .uri("/health/ping")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `readiness reports ok`() {
    webTestClient.get()
      .uri("/health/readiness")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `liveness reports ok`() {
    webTestClient.get()
      .uri("/health/liveness")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }
}
