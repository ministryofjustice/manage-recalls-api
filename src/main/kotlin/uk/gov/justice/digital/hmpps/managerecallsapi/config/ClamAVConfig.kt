package uk.gov.justice.digital.hmpps.managerecallsapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import xyz.capybara.clamav.ClamavClient

@Configuration
class ClamAVConfig(
  @Value("\${clamav.hostname}") val clamavHost: String,
  @Value("\${clamav.port}") val clamavPort: Int
) {

  @Bean
  fun clamavClient(): ClamavClient = ClamavClient(clamavHost, clamavPort)
}
