package uk.gov.justice.digital.hmpps.managerecallsapi.webclient

import io.micrometer.core.instrument.Counter
import org.springframework.core.ParameterizedTypeReference
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.config.ClientTimeoutException
import java.time.Duration
import java.util.concurrent.TimeoutException

class TimeoutHandlingWebClient(
  private val webClient: WebClient,
  private val clientTimeout: Long,
  private val timeoutCounter: Counter
) {

  fun <T> getWithTimeout(
    uri: String,
    value: ParameterizedTypeReference<T>,
    callingClazz: Class<CachingClient<*>>
  ): Mono<T> = webClient
    .get()
    .uri(uri)
    .retrieve()
    .bodyToMono(value)
    .timeout(Duration.ofSeconds(clientTimeout))
    .onErrorMap(TimeoutException::class.java) { ex ->
      timeoutCounter.increment()
      ClientTimeoutException(callingClazz.simpleName, ex.javaClass.canonicalName)
    }
}
