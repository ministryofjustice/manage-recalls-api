package uk.gov.justice.digital.hmpps.managerecallsapi.integration

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.managerecallsapi.config.ManageRecallsApiJackson
import uk.gov.justice.digital.hmpps.managerecallsapi.search.AuthenticatingRestClient

// Create WebClients without MetricsCustomizer
@TestConfiguration
class TestWebClientConfig {

  @Bean("prisonRegisterTestWebClient")
  fun prisonRegisterTestWebClient(@Value("\${prisonRegister.endpoint.url}") prisonRegisterEndpointUrl: String): WebClient =
    webClient(prisonRegisterEndpointUrl)

  @Bean("courtRegisterTestWebClient")
  fun courtRegisterTestWebClient(@Value("\${courtRegister.endpoint.url}") courtRegisterEndpointUrl: String): WebClient =
    webClient(courtRegisterEndpointUrl)

  @Bean("prisonerOffenderSearchWebClient")
  fun prisonerOffenderSearchWebClient(@Value("\${prisonerSearch.endpoint.url}") prisonerOffenderSearchBaseUrl: String): AuthenticatingRestClient =
    AuthenticatingRestClient(webClient(prisonerOffenderSearchBaseUrl), "unused")

  @Bean("prisonApiWebClient")
  fun prisonApiWebClient(@Value("\${prisonApi.endpoint.url}") prisonApiBaseUrl: String): AuthenticatingRestClient =
    AuthenticatingRestClient(webClient(prisonApiBaseUrl), "unused")

  private fun webClient(endpointUrl: String): WebClient {
    val builder = WebClient.builder()
    return builder
      .baseUrl(endpointUrl)
      .codecs {
        it.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)
        it.defaultCodecs().jackson2JsonEncoder(Jackson2JsonEncoder(ManageRecallsApiJackson.mapper, APPLICATION_JSON))
        it.defaultCodecs().jackson2JsonDecoder(Jackson2JsonDecoder(ManageRecallsApiJackson.mapper, APPLICATION_JSON))
      }
      .build()
  }
}
