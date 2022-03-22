package uk.gov.justice.digital.hmpps.managerecallsapi.webclient

import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId
import org.springframework.web.reactive.function.client.WebClient

class AuthenticatingWebClient(
  private val webClient: WebClient,
  private val oauthClient: String
) {
  fun post(path: String, body: Any): WebClient.RequestHeadersSpec<*> = webClient
    .post()
    .uri(path)
    .accept(APPLICATION_JSON)
    .contentType(APPLICATION_JSON)
    .attributes(clientRegistrationId(oauthClient))
    .bodyValue(body)

  fun get(path: String): WebClient.RequestHeadersSpec<*> = webClient
    .get()
    .uri(path)
    .accept(APPLICATION_JSON)
    .attributes(clientRegistrationId(oauthClient))
}
