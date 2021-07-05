package uk.gov.justice.digital.hmpps.managerecallsapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.thymeleaf.spring5.SpringTemplateEngine
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver

@Configuration
class ThymeleafConfig {

  @Bean
  fun thymeleafEngine(): SpringTemplateEngine {
    val templateEngine = SpringTemplateEngine()
    val templateResolver = ClassLoaderTemplateResolver()
    templateResolver.prefix = "templates/"
    templateResolver.isCacheable = false
    templateResolver.suffix = ".html"
    templateResolver.setTemplateMode("HTML5")

    templateEngine.setTemplateResolver(templateResolver)
    return templateEngine
  }
}
