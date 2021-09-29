package uk.gov.justice.digital.hmpps.managerecallsapi.config

import com.fasterxml.jackson.databind.ObjectMapper
import io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import uk.gov.justice.digital.hmpps.managerecallsapi.search.AuthenticatingRestClient
import java.lang.Integer.MAX_VALUE
import java.time.Duration.ofSeconds
import java.util.concurrent.TimeUnit.MILLISECONDS

@Configuration
class WebClientConfig {

  @Value("\${prisonerSearch.endpoint.url}")
  private lateinit var prisonerOffenderSearchBaseUrl: String

  @Bean
  fun prisonerOffenderSearchWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    objectMapper: ObjectMapper
  ): AuthenticatingRestClient {
    return AuthenticatingRestClient(
      webClientFactory(prisonerOffenderSearchBaseUrl, authorizedClientManager, MAX_VALUE, objectMapper),
      "offender-search-client"
    )
  }

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
  fun prisonRegisterWebClient(): WebClient {
    return WebClient.builder().baseUrl(prisonRegisterEndpointUrl).build()
  }

  private fun webClientFactory(
    baseUrl: String,
    authorizedClientManager: OAuth2AuthorizedClientManager,
    bufferByteCount: Int,
    objectMapper: ObjectMapper
  ): WebClient {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)

    val httpClient = HttpClient.create()
      .option(CONNECT_TIMEOUT_MILLIS, 10000)
      .doOnConnected {
        it.addHandlerLast(ReadTimeoutHandler(0, MILLISECONDS))
          .addHandlerLast(WriteTimeoutHandler(0, MILLISECONDS))
      }

    return WebClient
      .builder()
      .clientConnector(ReactorClientHttpConnector(httpClient))
      .codecs {
        it.defaultCodecs().jackson2JsonEncoder(Jackson2JsonEncoder(objectMapper, APPLICATION_JSON))
        it.defaultCodecs().jackson2JsonDecoder(Jackson2JsonDecoder(objectMapper, APPLICATION_JSON))
        it.defaultCodecs().maxInMemorySize(bufferByteCount)
      }
      .baseUrl(baseUrl)
      .defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
      .apply(oauth2Client.oauth2Configuration())
      .build()
  }

  @Bean
  fun authorizedClientManager(
    clientRegistrationRepository: ClientRegistrationRepository?,
    authorizedClientRepository: OAuth2AuthorizedClientRepository?
  ): OAuth2AuthorizedClientManager =
    DefaultOAuth2AuthorizedClientManager(
      clientRegistrationRepository,
      authorizedClientRepository
    ).apply {
      this.setAuthorizedClientProvider(
        OAuth2AuthorizedClientProviderBuilder.builder()
          .clientCredentials()
          .build()
      )
    }
}
