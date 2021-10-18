package uk.gov.justice.digital.hmpps.managerecallsapi.health

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.managerecallsapi.config.ClamAVConfig

@Component("clamAV")
class ClamAVHealth(
  @Value("\${clamav.virus.scan.enabled:true}") val clamavEnabled: Boolean,
  @Autowired val clamAVConfig: ClamAVConfig
) : HealthIndicator {

  override fun health(): Health {
    return try {
      if (clamavEnabled) {
        clamAVConfig.clamavClient().ping()
      }
      Health.up().withDetail("active", clamavEnabled).build()
    } catch (e: Exception) {
      Health.down(e).build()
    }
  }
}
