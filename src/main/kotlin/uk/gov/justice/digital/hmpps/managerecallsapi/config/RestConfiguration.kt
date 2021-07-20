package uk.gov.justice.digital.hmpps.managerecallsapi.config

import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import java.util.UUID

@Configuration
class RestConfiguration {
  @Bean
  fun restTemplate(builder: RestTemplateBuilder): RestTemplate {
    return builder.build()
  }

  @Bean
  fun objectMapper(): ObjectMapper =
    ObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
      .setSerializationInclusion(NON_NULL)
      .registerModules(Jdk8Module(), JavaTimeModule(), KotlinModule())
      .registerModule(
        SimpleModule().apply {
          NomsNumberSerializers(this)
          RecallIdSerializors(this)
        }
      )

  object NomsNumberSerializers {
    operator fun invoke(simpleModule: SimpleModule) {
      simpleModule.addSerializer(NomsNumber::class.java, nomsNumberSerializer())
      simpleModule.addDeserializer(NomsNumber::class.java, nomsNumberDeserializer())
    }

    private fun nomsNumberDeserializer() = object : JsonDeserializer<NomsNumber>() {
      override fun deserialize(p: JsonParser, ctxt: DeserializationContext) = NomsNumber(p.text)
    }

    private fun nomsNumberSerializer() = object : JsonSerializer<NomsNumber>() {
      override fun serialize(value: NomsNumber, gen: JsonGenerator, serializers: SerializerProvider) =
        gen.writeString(value.value)
    }
  }

  object RecallIdSerializors {
    operator fun invoke(simpleModule: SimpleModule) {
      simpleModule.addSerializer(RecallId::class.java, nomsNumberSerializer())
      simpleModule.addDeserializer(RecallId::class.java, nomsNumberDeserializer())
    }

    private fun nomsNumberDeserializer() = object : JsonDeserializer<RecallId>() {
      override fun deserialize(p: JsonParser, ctxt: DeserializationContext) = RecallId(UUID.fromString(p.text))
    }

    private fun nomsNumberSerializer() = object : JsonSerializer<RecallId>() {
      override fun serialize(value: RecallId, gen: JsonGenerator, serializers: SerializerProvider) =
        gen.writeString(value.value.toString())
    }
  }
}
