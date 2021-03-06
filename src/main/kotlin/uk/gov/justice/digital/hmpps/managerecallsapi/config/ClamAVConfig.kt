package uk.gov.justice.digital.hmpps.managerecallsapi.config

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.digital.hmpps.managerecallsapi.service.ClamavVirusScanner
import uk.gov.justice.digital.hmpps.managerecallsapi.service.VirusScanResult.NoVirusFound
import uk.gov.justice.digital.hmpps.managerecallsapi.service.VirusScanner
import xyz.capybara.clamav.ClamavClient

@Configuration
class ClamAVConfig(
  @Value("\${clamav.hostname}") val clamavHost: String,
  @Value("\${clamav.port}") val clamavPort: Int
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  @Bean
  @ConditionalOnProperty(
    value = ["clamav.virus.scan.enabled"],
    havingValue = "true",
    matchIfMissing = true
  )
  fun clamavClient(): ClamavClient = ClamavClient(clamavHost, clamavPort)

  @Bean
  @ConditionalOnProperty(
    value = ["clamav.virus.scan.enabled"],
    havingValue = "true",
    matchIfMissing = true
  )
  fun virusScanner(clamavClient: ClamavClient): VirusScanner = ClamavVirusScanner(clamavClient)

  @Bean
  @ConditionalOnMissingBean
  fun stubVirusScanner() = VirusScanner {
    log.info("Stub Virus Scanner returning NoVirusFound")
    NoVirusFound
  }
}
