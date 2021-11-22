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

  @Bean("prisonRegisterWebClient")
  fun prisonRegisterWebClient(): WebClient = webClient(prisonRegisterEndpointUrl)

  @Value("\${courtRegister.endpoint.url}")
  private lateinit var courtRegisterEndpointUrl: String

  @Bean("courtRegisterWebClient")
  fun courtRegisterWebClient(): WebClient = webClient(courtRegisterEndpointUrl)

  @Value("\${bankHolidayRegister.endpoint.url}")
  private lateinit var bankHolidayRegisterEndpointUrl: String

  @Bean("bankHolidayRegisterWebClient")
  fun bankHolidayRegisterWebClient(): WebClient = webClient(bankHolidayRegisterEndpointUrl)

  @Value("\${policeUkApi.endpoint.url}")
  private lateinit var policeUkApiEndpointUrl: String

  @Bean("policeUkApiWebClient")
  fun policeUkApiWebClient(): WebClient = webClient(policeUkApiEndpointUrl)

  private fun webClient(endpointUrl: String) = WebClient.builder()
    .baseUrl(endpointUrl)
    .codecs {
      it.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)
      it.defaultCodecs().jackson2JsonEncoder(Jackson2JsonEncoder(ManageRecallsApiJackson.mapper, APPLICATION_JSON))
      it.defaultCodecs().jackson2JsonDecoder(Jackson2JsonDecoder(ManageRecallsApiJackson.mapper, APPLICATION_JSON))
    }
    .build()
}
