package uk.gov.justice.digital.hmpps.managerecallsapi.health

import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.boot.info.BuildProperties
import org.springframework.stereotype.Component

/**
 * Adds version data to the /health endpoint. This is called by the UI to display API details
 */
@Component
class HealthInfo(buildProperties: BuildProperties) : HealthIndicator {
  private val version: String = buildProperties.version
  private val buildUrl: String = System.getenv("BUILD_URL") ?: "BUILD_URL not defined"

  override fun health(): Health = Health.up().withDetail("version", version).withDetail("build_url", buildUrl).build()
}
