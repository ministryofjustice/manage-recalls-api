package uk.gov.justice.digital.hmpps.managerecallsapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import uk.gov.justice.digital.hmpps.managerecallsapi.config.ManageRecallsApiJackson.mapper
import java.time.Duration.ofSeconds

@Configuration
class WebClientConfig {

  @Bean
  fun webClient(): WebClient =
    WebClient.builder()
      .codecs { it.defaultCodecs().maxInMemorySize(16 * 1024 * 1024) }
      .clientConnector(
        ReactorClientHttpConnector(HttpClient.create().responseTimeout(ofSeconds(10)))
      )
      .build()

  @Value("\${prisonRegister.endpoint.url}")
  private lateinit var prisonRegisterEndpointUrl: String

  @Bean
  fun prisonRegisterWebClient(): WebClient =
    WebClient.builder()
      .baseUrl(prisonRegisterEndpointUrl)
      .codecs {
        it.defaultCodecs().jackson2JsonEncoder(Jackson2JsonEncoder(mapper, APPLICATION_JSON))
        it.defaultCodecs().jackson2JsonDecoder(Jackson2JsonDecoder(mapper, APPLICATION_JSON))
      }
      .build()
}
