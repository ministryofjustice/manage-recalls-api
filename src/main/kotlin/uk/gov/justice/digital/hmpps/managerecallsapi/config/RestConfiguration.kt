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
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Email
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FieldName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FieldPath
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FullName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastKnownAddressId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.MiddleNames
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.MissingDocumentsRecordId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhoneNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PoliceForceId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PoliceForceName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Validated
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.WarrantReferenceNumber
import java.util.Base64

@Configuration
class RestConfiguration {
  @Bean
  fun restTemplate(builder: RestTemplateBuilder): RestTemplate {
    return builder.build()
  }

  @Bean
  fun decoder(): Base64.Decoder = Base64.getDecoder()

  @Bean
  fun objectMapper(): ObjectMapper = ManageRecallsApiJackson.mapper
}

object ManageRecallsApiJackson : ConfigurableJackson(
  KotlinModule.Builder()
    .build()
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
  text(::MiddleNames)
  text(::LastName)
  text(::FullName)
  text(::Email)
  text(::PhoneNumber)
  text(::WarrantReferenceNumber)
  text(::PrisonName)
  text(::PrisonId)
  text(::CourtId)
  text(::CourtName)
  text(::PoliceForceId)
  text(::PoliceForceName)
  text(::FieldName)
  text(::FieldPath)
  text(StringBiDiMappings.uuid().map(::RecallId, RecallId::value))
  text(StringBiDiMappings.uuid().map(::MissingDocumentsRecordId, MissingDocumentsRecordId::value))
  text(StringBiDiMappings.uuid().map(::LastKnownAddressId, LastKnownAddressId::value))
  text(StringBiDiMappings.uuid().map(::DocumentId, DocumentId::value))
  text(StringBiDiMappings.uuid().map(::UserId, UserId::value))
}

inline fun <reified T : Validated<*>> AutoMappingConfiguration<ObjectMapper>.text(
  crossinline read: (String) -> T,
  crossinline write: (T) -> String = { it.value.toString() }
) = text(BiDiMapping({ read(it) }, { write(it) }))
