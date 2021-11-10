package uk.gov.justice.digital.hmpps.managerecallsapi.component.health

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.OK
import org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.managerecallsapi.component.ComponentTestBase
import uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers.HealthServer
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter.ISO_DATE
import java.util.stream.Stream

@ActiveProfiles("db-test-no-clam")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureWebTestClient(timeout = "15000")
class HealthCheckComponentTest : ComponentTestBase() {

  @Test
  fun `healthy service returns status of each health check and version details`() {
    prisonerOffenderSearch.isHealthy()
    gotenbergMockServer.isHealthy()
    prisonRegisterMockServer.isHealthy()
    courtRegisterMockServer.isHealthy()
    hmppsAuthMockServer.isHealthy()

    healthCheckIsUpWith(
      "/health",
      "status" to "UP",
      "components.prisonerOffenderSearch.status" to "UP",
      "components.prisonerOffenderSearch.details.status" to OK.name,
      "components.gotenberg.status" to "UP",
      "components.gotenberg.details.status" to OK.name,
      "components.healthInfo.details.version" to LocalDateTime.now().format(ISO_DATE),
      "components.db.status" to "UP",
      "components.s3.status" to "UP",
      "components.prisonRegister.status" to "UP",
      "components.courtRegister.status" to "UP",
      "components.clamAV.status" to "UP",
      "components.hmppsAuth.status" to "UP",
      "components.bankHoliday.status" to "UP"
    )
  }

  @Test
  fun `timeout is handled gracefully as down`() {
    prisonerOffenderSearch.isSlow(INTERNAL_SERVER_ERROR, 3000)
    gotenbergMockServer.isSlow(INTERNAL_SERVER_ERROR, 3000)
    prisonRegisterMockServer.isSlow(INTERNAL_SERVER_ERROR, 3000)
    courtRegisterMockServer.isSlow(INTERNAL_SERVER_ERROR, 3000)
    hmppsAuthMockServer.isSlow(INTERNAL_SERVER_ERROR, 3000)

    healthCheckIsUpWith(
      "/health",
      "status" to "UP",
      "components.db.status" to "UP",
      "components.s3.status" to "UP",
      "components.clamAV.status" to "UP",
      "components.healthInfo.details.version" to LocalDateTime.now().format(ISO_DATE),
      "components.prisonerOffenderSearch.status" to "UNKNOWN",
      "components.prisonerOffenderSearch.details.body" to "java.lang.IllegalStateException: Timeout on blocking read for 2000000000 NANOSECONDS",
      "components.gotenberg.status" to "UNKNOWN",
      "components.gotenberg.details.body" to "java.lang.IllegalStateException: Timeout on blocking read for 2000000000 NANOSECONDS",
      "components.prisonRegister.status" to "UNKNOWN",
      "components.prisonRegister.details.body" to "java.lang.IllegalStateException: Timeout on blocking read for 2000000000 NANOSECONDS",
      "components.courtRegister.status" to "UNKNOWN",
      "components.courtRegister.details.body" to "java.lang.IllegalStateException: Timeout on blocking read for 2000000000 NANOSECONDS",
      "components.hmppsAuth.status" to "UNKNOWN",
      "components.hmppsAuth.details.body" to "java.lang.IllegalStateException: Timeout on blocking read for 2000000000 NANOSECONDS",
      "components.bankHoliday.status" to "UNKNOWN",
      "components.bankHoliday.details.body" to "java.lang.IllegalStateException: Timeout on blocking read for 2000000000 NANOSECONDS"
    )
  }

  @ParameterizedTest(name = "service is unhealthy when {0} is unhealthy")
  @MethodSource("parameterArrays")
  fun `service is unhealthy when dependency is unhealthy`(
    dependency: String,
    mockServer: HealthServer
  ) {
    mockServer.isUnhealthy()

    healthCheckIsDownWith(
      SERVICE_UNAVAILABLE,
      "components.$dependency.details.status" to INTERNAL_SERVER_ERROR.name
    )
  }

  private fun parameterArrays(): Stream<Arguments>? {
    return Stream.of(
      Arguments.of("prisonerOffenderSearch", prisonerOffenderSearch),
      Arguments.of("courtRegister", courtRegisterMockServer),
      Arguments.of("prisonRegister", prisonRegisterMockServer),
      Arguments.of("gotenberg", gotenbergMockServer),
      Arguments.of("hmppsAuth", hmppsAuthMockServer),
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
