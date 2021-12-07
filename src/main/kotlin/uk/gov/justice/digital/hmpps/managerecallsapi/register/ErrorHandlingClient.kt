package uk.gov.justice.digital.hmpps.managerecallsapi.register

import org.springframework.core.ParameterizedTypeReference
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.config.ClientException

abstract class ErrorHandlingClient(val webClient: WebClient) {

  fun <T> getResponse(uri: String, typeReference: ParameterizedTypeReference<T>): Mono<T> {
    return getResponseWithoutErrorHandling(uri, typeReference)
      .onErrorResume(WebClientResponseException::class.java) { exception ->
        Mono.error(ClientException(this.javaClass.simpleName, exception))
      }
  }

  fun <T> getResponseWith404Handling(uri: String, typeReference: ParameterizedTypeReference<T>): Mono<T> {
    return getResponseWithoutErrorHandling(uri, typeReference)
      .onErrorResume(WebClientResponseException::class.java) { exception ->
        when (exception.rawStatusCode) {
          404 -> Mono.empty()
          else -> Mono.error(ClientException(this.javaClass.simpleName, exception))
        }
      }
  }

  private fun <T> getResponseWithoutErrorHandling(
    uri: String,
    value: ParameterizedTypeReference<T>
  ) = webClient
    .get()
    .uri(uri)
    .retrieve()
    .bodyToMono(value)
}
