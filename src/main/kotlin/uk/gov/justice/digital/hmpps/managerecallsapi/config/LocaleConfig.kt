package uk.gov.justice.digital.hmpps.managerecallsapi.config

import org.springframework.context.annotation.Configuration
import java.time.ZoneOffset
import java.util.TimeZone
import javax.annotation.PostConstruct

@Configuration
class LocaleConfig {
  @PostConstruct
  fun init() {
    TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC))
  }
}
