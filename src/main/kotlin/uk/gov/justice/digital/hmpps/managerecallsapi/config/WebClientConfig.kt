package uk.gov.justice.digital.hmpps.managerecallsapi.config

import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import uk.gov.justice.digital.hmpps.managerecallsapi.search.AuthenticatingRestClient
import java.util.concurrent.TimeUnit

@Configuration
class WebClientConfig {

  @Value("\${prisonerSearch.endpoint.url}")
  private lateinit var prisonerOffenderSearchBaseUrl: String

  @Value("\${probationSearch.endpoint.url}")
  private lateinit var probationOffenderSearchBaseUrl: String

  @Bean
  fun prisonerOffenderSearchWebClient(authorizedClientManager: OAuth2AuthorizedClientManager): AuthenticatingRestClient {
    return AuthenticatingRestClient(
      webClientFactory(prisonerOffenderSearchBaseUrl, authorizedClientManager, Integer.MAX_VALUE),
      "offender-search-client"
    )
  }

  @Bean
  fun probationOffenderSearchWebClient(authorizedClientManager: OAuth2AuthorizedClientManager): AuthenticatingRestClient {
    return AuthenticatingRestClient(
      webClientFactory(probationOffenderSearchBaseUrl, authorizedClientManager, Integer.MAX_VALUE),
      "offender-search-client"
    )
  }

  private fun webClientFactory(
    baseUrl: String,
    authorizedClientManager: OAuth2AuthorizedClientManager,
    bufferByteCount: Int
  ): WebClient {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)

    val httpClient = HttpClient.create()
      .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
      .doOnConnected {
        it.addHandlerLast(ReadTimeoutHandler(0, TimeUnit.MILLISECONDS))
          .addHandlerLast(WriteTimeoutHandler(0, TimeUnit.MILLISECONDS))
      }

    return WebClient
      .builder()
      .clientConnector(ReactorClientHttpConnector(httpClient))
      .codecs { it.defaultCodecs().maxInMemorySize(bufferByteCount) }
      .baseUrl(baseUrl)
      .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
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
