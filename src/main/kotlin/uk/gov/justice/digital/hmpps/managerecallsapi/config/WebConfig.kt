package uk.gov.justice.digital.hmpps.managerecallsapi.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.SentryContextAppender

@Configuration
class WebConfig : WebMvcConfigurer {
  override fun addInterceptors(registry: InterceptorRegistry) {
    registry.addInterceptor(SentryContextAppender())
  }
}
