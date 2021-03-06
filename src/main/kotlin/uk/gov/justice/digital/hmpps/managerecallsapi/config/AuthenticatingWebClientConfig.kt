package uk.gov.justice.digital.hmpps.managerecallsapi.config

import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.metrics.web.reactive.client.MetricsWebClientCustomizer
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
import uk.gov.justice.digital.hmpps.managerecallsapi.webclient.AuthenticatingWebClient
import java.lang.Integer.MAX_VALUE
import java.net.URI
import java.util.concurrent.TimeUnit.MILLISECONDS

@Configuration
class AuthenticatingWebClientConfig(
  @Autowired private val meterRegistry: MeterRegistry
) {

  @Value("\${prisonerSearch.endpoint.url}")
  private lateinit var prisonerOffenderSearchBaseUrl: String

  @Bean("prisonerOffenderSearchWebClient")
  fun prisonerOffenderSearchWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    objectMapper: ObjectMapper,
    metricsCustomizer: MetricsWebClientCustomizer
  ): AuthenticatingWebClient =
    AuthenticatingWebClient(
      authenticatedWebClientFactory(
        prisonerOffenderSearchBaseUrl,
        authorizedClientManager,
        MAX_VALUE,
        objectMapper,
        metricsCustomizer
      ),
      "offender-search-client"
    )

  @Bean("prisonerOffenderSearchTimeoutCounter")
  fun prisonerOffenderSearchTimeoutCounter(): Counter = timeoutCounter(prisonerOffenderSearchBaseUrl)

  @Value("\${prisonApi.endpoint.url}")
  private lateinit var prisonApiBaseUrl: String

  @Bean("prisonApiWebClient")
  fun prisonApiWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    objectMapper: ObjectMapper,
    metricsCustomizer: MetricsWebClientCustomizer
  ): AuthenticatingWebClient =
    AuthenticatingWebClient(
      authenticatedWebClientFactory(prisonApiBaseUrl, authorizedClientManager, MAX_VALUE, objectMapper, metricsCustomizer),
      "prison-api-client"
    )

  @Bean("prisonApiTimeoutCounter")
  fun prisonApiTimeoutCounter(): Counter = timeoutCounter(prisonApiBaseUrl)

  private fun authenticatedWebClientFactory(
    baseUrl: String,
    authorizedClientManager: OAuth2AuthorizedClientManager,
    bufferByteCount: Int,
    objectMapper: ObjectMapper,
    metricsCustomizer: MetricsWebClientCustomizer
  ): WebClient {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)

    val httpClient = HttpClient.create()
      .option(CONNECT_TIMEOUT_MILLIS, 10000)
      .doOnConnected {
        it.addHandlerLast(ReadTimeoutHandler(0, MILLISECONDS))
          .addHandlerLast(WriteTimeoutHandler(0, MILLISECONDS))
      }

    val builder = WebClient.builder()
    metricsCustomizer.customize(builder)
    return builder
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

  private fun timeoutCounter(endpointUrl: String): Counter {
    val metricName = "http_client_requests_timeout"
    val host = URI(endpointUrl).host
    return meterRegistry.counter(metricName, Tags.of("clientName", host))
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
