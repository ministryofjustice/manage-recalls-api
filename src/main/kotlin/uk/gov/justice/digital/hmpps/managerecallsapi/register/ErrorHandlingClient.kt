package uk.gov.justice.digital.hmpps.managerecallsapi.register

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import org.springframework.core.ParameterizedTypeReference
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.config.ClientException
import uk.gov.justice.digital.hmpps.managerecallsapi.config.ClientTimeoutException
import java.net.URI
import java.time.Duration
import java.util.concurrent.TimeoutException

abstract class ErrorHandlingClient(
  val webClient: WebClient,
  private val endpointUrl: String,
  private val clientTimeout: Long,
  private val meterRegistry: MeterRegistry
) {

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
    .timeout(Duration.ofSeconds(clientTimeout))
    .onErrorMap(TimeoutException::class.java) { ex ->
      meterRegistry.counter("http_client_requests_timeout", Tags.of("clientName", URI(endpointUrl).host)).increment()
      ClientTimeoutException(this.javaClass.simpleName, ex.javaClass.canonicalName)
    }
}
