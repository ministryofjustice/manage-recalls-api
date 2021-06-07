package uk.gov.justice.digital.hmpps.managerecallsapi.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
class RestConfiguration {
  @Bean
  fun restTemplate(builder: RestTemplateBuilder): RestTemplate {
    return builder.build()
  }

  @Bean
  fun objectMapper(): ObjectMapper = ObjectMapper()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
//    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
//    .setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
    .registerModules(Jdk8Module(), JavaTimeModule(), KotlinModule())
}
