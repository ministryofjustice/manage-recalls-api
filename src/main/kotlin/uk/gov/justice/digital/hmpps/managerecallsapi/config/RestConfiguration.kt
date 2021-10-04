package uk.gov.justice.digital.hmpps.managerecallsapi.config

import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.http4k.format.AutoMappingConfiguration
import org.http4k.format.ConfigurableJackson
import org.http4k.format.asConfigurable
import org.http4k.format.withStandardMappings
import org.http4k.lens.BiDiMapping
import org.http4k.lens.StringBiDiMappings
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Email
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhoneNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Validated

@Configuration
class RestConfiguration {
  @Bean
  fun restTemplate(builder: RestTemplateBuilder): RestTemplate {
    return builder.build()
  }

  @Bean
  fun objectMapper(): ObjectMapper = ManageRecallsApiJackson.mapper
}

object ManageRecallsApiJackson : ConfigurableJackson(
  KotlinModule()
    .asConfigurable()
    .withStandardMappings()
    .withCustomMappings()
    .done()
    .deactivateDefaultTyping()
    .configure(WRITE_DATES_AS_TIMESTAMPS, false)
    .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(FAIL_ON_IGNORED_PROPERTIES, false)
    .setSerializationInclusion(NON_NULL)
)

private fun AutoMappingConfiguration<ObjectMapper>.withCustomMappings() = apply {
  text(::NomsNumber)
  text(::FirstName)
  text(::LastName)
  text(::Email)
  text(::PhoneNumber)
  text(::PrisonName)
  text(::PrisonId)
  text(StringBiDiMappings.uuid().map(::RecallId, RecallId::value))
  text(StringBiDiMappings.uuid().map(::UserId, UserId::value))
}

inline fun <reified T : Validated<*>> AutoMappingConfiguration<ObjectMapper>.text(
  crossinline read: (String) -> T,
  crossinline write: (T) -> String = { it.value.toString() }
) = text(BiDiMapping({ read(it) }, { write(it) }))
